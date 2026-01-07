package com.example;

import com.example.config.BrbConfig;
import com.example.model.AfkStatus;
import com.example.model.CustomKeyword;
import com.example.model.PlayerStats;
import com.example.overlay.BrbMinimapOverlay;
import com.example.overlay.BrbOverlay;
import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Away Status",
	description = "Displays AFK status above players when they type keywords like 'brb', 'afk', or '2mins'",
	tags = {"afk", "brb", "status", "overlay", "away"}
)
public class BrbPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BrbConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BrbOverlay overlay;

	@Inject
	private BrbMinimapOverlay minimapOverlay;

	private final Map<String, AfkStatus> afkPlayers = new ConcurrentHashMap<>();
	private final Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
	private final Map<String, CustomKeyword> customKeywords = new HashMap<>();
	private static final Map<String, CustomKeyword> DEFAULT_KEYWORDS = new HashMap<>();
	static
	{
		DEFAULT_KEYWORDS.put("brb toilet", new CustomKeyword("brb toilet", "BRB Toilet", "🚽"));
		DEFAULT_KEYWORDS.put("brb piss", new CustomKeyword("brb piss", "BRB", "🚽"));
		DEFAULT_KEYWORDS.put("brb bathroom", new CustomKeyword("brb bathroom", "BRB", "🚽"));
		DEFAULT_KEYWORDS.put("gonna go eat", new CustomKeyword("gonna go eat", "Getting Food", "🍔"));
		DEFAULT_KEYWORDS.put("going to eat", new CustomKeyword("going to eat", "Getting Food", "🍔"));
		DEFAULT_KEYWORDS.put("gonna eat", new CustomKeyword("gonna eat", "Getting Food", "🍔"));
		DEFAULT_KEYWORDS.put("brb food", new CustomKeyword("brb food", "Getting Food", "🍔"));
		DEFAULT_KEYWORDS.put("brb eating", new CustomKeyword("brb eating", "Getting Food", "🍔"));
		DEFAULT_KEYWORDS.put("brb coffee", new CustomKeyword("brb coffee", "Getting Coffee", "☕"));
		DEFAULT_KEYWORDS.put("gonna get coffee", new CustomKeyword("gonna get coffee", "Getting Coffee", "☕"));
		DEFAULT_KEYWORDS.put("going to get coffee", new CustomKeyword("going to get coffee", "Getting Coffee", "☕"));
		DEFAULT_KEYWORDS.put("brb smoke", new CustomKeyword("brb smoke", "BRB Smoke", "🚬"));
		DEFAULT_KEYWORDS.put("brb smoking", new CustomKeyword("brb smoking", "BRB Smoke", "🚬"));
		DEFAULT_KEYWORDS.put("gonna smoke", new CustomKeyword("gonna smoke", "BRB Smoke", "🚬"));
		DEFAULT_KEYWORDS.put("going to smoke", new CustomKeyword("going to smoke", "BRB Smoke", "🚬"));
		DEFAULT_KEYWORDS.put("brb shower", new CustomKeyword("brb shower", "BRB Shower", "🚿"));
		DEFAULT_KEYWORDS.put("gonna shower", new CustomKeyword("gonna shower", "BRB Shower", "🚿"));
		DEFAULT_KEYWORDS.put("going to shower", new CustomKeyword("going to shower", "BRB Shower", "🚿"));
		DEFAULT_KEYWORDS.put("brb phone", new CustomKeyword("brb phone", "BRB Phone", "📱"));
		DEFAULT_KEYWORDS.put("phone call", new CustomKeyword("phone call", "BRB Phone", "📱"));
		DEFAULT_KEYWORDS.put("brb call", new CustomKeyword("brb call", "BRB Phone", "📱"));
	}

	private final Map<String, WorldPoint> playerPositions = new ConcurrentHashMap<>();
	private static final String[] KEYWORDS = {"brb", "be right back", "afk", "away"};
	private static final Pattern TIME_PATTERN = Pattern.compile(
		"(\\d+)\\s*(min(?:ute)?s?|sec(?:ond)?s?|m|s)",
		Pattern.CASE_INSENSITIVE
	);

	public String normalizePlayerName(String name)
	{
		if (name == null)
		{
			return null;
		}
		return name.replaceAll("\\p{Zs}+", " ")
			.replaceAll("\\s+", " ")
			.trim();
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		overlayManager.add(minimapOverlay);
		parseCustomKeywords();
		log.debug("BRB/AFK Status plugin started!");
	}

	private void parseCustomKeywords()
	{
		customKeywords.clear();
		customKeywords.putAll(DEFAULT_KEYWORDS);
		
		String keywordsStr = config.customKeywords();
		if (keywordsStr == null || keywordsStr.trim().isEmpty())
		{
			return;
		}

		String[] entries = keywordsStr.split(",");
		for (String entry : entries)
		{
			String[] parts = entry.trim().split(":");
			if (parts.length >= 2)
			{
				String keyword = parts[0].trim().toLowerCase();
				String status = parts[1].trim();
				String icon = parts.length >= 3 ? parts[2].trim() : "";
				customKeywords.put(keyword, new CustomKeyword(keyword, status, icon));
			}
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(minimapOverlay);
		afkPlayers.clear();
		playerStats.clear();
		log.debug("BRB/AFK Status plugin stopped!");
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.PUBLICCHAT && 
			event.getType() != ChatMessageType.PRIVATECHAT &&
			event.getType() != ChatMessageType.MODCHAT &&
			event.getType() != ChatMessageType.FRIENDSCHAT)
		{
			return;
		}

		String message = event.getMessage();
		if (message == null)
		{
			return;
		}

		message = message.toLowerCase();
		String playerName = normalizePlayerName(event.getName());
		
		if (playerName == null || playerName.isEmpty())
		{
			return;
		}

		Pattern backPattern = Pattern.compile("\\bback\\b", Pattern.CASE_INSENSITIVE);
		if (backPattern.matcher(message).find())
		{
			AfkStatus removed = afkPlayers.remove(playerName);
			if (removed != null)
			{
				log.debug("Player {} removed AFK status (said 'back')", playerName);
			}
			playerPositions.remove(playerName);
			return;
		}

		CustomKeyword customKeyword = null;
		for (Map.Entry<String, CustomKeyword> entry : customKeywords.entrySet())
		{
			String keyword = entry.getKey();
			Pattern keywordPattern = Pattern.compile("(^|\\s)" + Pattern.quote(keyword) + "(\\s|$|[.,!?])", Pattern.CASE_INSENSITIVE);
			if (keywordPattern.matcher(message).find())
			{
				customKeyword = entry.getValue();
				break;
			}
		}

		boolean hasKeyword = false;
		if (customKeyword == null)
		{
			for (String keyword : KEYWORDS)
			{
				Pattern keywordPattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "(?=\\s|\\d|\\b|$)", Pattern.CASE_INSENSITIVE);
				if (keywordPattern.matcher(message).find())
				{
					hasKeyword = true;
					break;
				}
			}
		}

		if (!hasKeyword && customKeyword == null)
		{
			return;
		}

		if (customKeyword != null)
		{
			String statusText = customKeyword.getStatusText();
			String icon = customKeyword.getIcon();
			
			long durationMs = 0;
			Matcher timeMatcher = TIME_PATTERN.matcher(message);
			if (timeMatcher.find())
			{
				int amount = Integer.parseInt(timeMatcher.group(1));
				String unit = timeMatcher.group(2).toLowerCase();
				
				if (unit.startsWith("min") || unit.equals("m"))
				{
					if (amount > 60) amount = 60;
					durationMs = amount * 60 * 1000L;
					statusText += " " + amount + " min" + (amount != 1 ? "s" : "");
				}
				else if (unit.startsWith("sec") || unit.equals("s"))
				{
					durationMs = amount * 1000L;
					statusText += " " + amount + " sec" + (amount != 1 ? "s" : "");
				}
			}
			
			AfkStatus existingStatus = afkPlayers.get(playerName);
			int streak = (existingStatus != null) ? existingStatus.getStreak() + 1 : 1;
			
			long currentTime = System.currentTimeMillis();
			long endTime = durationMs > 0 ? currentTime + durationMs : 0;
			long startTime = existingStatus != null && existingStatus.getEndTime() == 0 
				? existingStatus.getStartTime() 
				: currentTime;
			
			AfkStatus status = new AfkStatus(statusText, endTime, startTime, streak, currentTime, icon);
			afkPlayers.put(playerName, status);
			updateStatistics(playerName, durationMs);
		
		log.debug("Player {} set to {} (custom keyword, streak: {})", playerName, statusText, streak);
			return;
		}

		long durationMs = 0;
		String statusText = "AFK";
		String icon = "⏸️";

		Pattern brbPattern = Pattern.compile("\\b(brb|be right back)(?=\\s|\\d|\\b|$)", Pattern.CASE_INSENSITIVE);
		Pattern afkPattern = Pattern.compile("\\bafk(?=\\s|\\d|\\b|$)", Pattern.CASE_INSENSITIVE);
		boolean isBrb = brbPattern.matcher(message).find();
		boolean isAfk = afkPattern.matcher(message).find();
		
		if (isBrb)
		{
			statusText = "BRB";
			icon = "💤";
		}
		else if (isAfk)
		{
			statusText = "AFK";
			icon = "⏸️";
		}
		
		Matcher timeMatcher = TIME_PATTERN.matcher(message);
		if (timeMatcher.find())
		{
			int amount = Integer.parseInt(timeMatcher.group(1));
			String unit = timeMatcher.group(2).toLowerCase();
			
			if (unit.startsWith("min") || unit.equals("m"))
			{
				if (amount > 60)
				{
					amount = 60;
				}
				durationMs = amount * 60 * 1000L;
				statusText = statusText + " " + amount + " min" + (amount != 1 ? "s" : "");
				icon = "⏰";
			}
			else if (unit.startsWith("sec") || unit.equals("s"))
			{
				durationMs = amount * 1000L;
				statusText = statusText + " " + amount + " sec" + (amount != 1 ? "s" : "");
				icon = "⏰";
			}
		}

		AfkStatus existingStatus = afkPlayers.get(playerName);
		int streak = (existingStatus != null) ? existingStatus.getStreak() + 1 : 1;

		long currentTime = System.currentTimeMillis();
		long endTime = durationMs > 0 ? currentTime + durationMs : 0;
		long startTime = existingStatus != null && existingStatus.getEndTime() == 0 
			? existingStatus.getStartTime() 
			: currentTime;
		
		AfkStatus status = new AfkStatus(statusText, endTime, startTime, streak, currentTime, icon);
		afkPlayers.put(playerName, status);
		
		Player player = findPlayerByName(playerName);
		if (player != null)
		{
			playerPositions.put(playerName, player.getWorldLocation());
		}

		updateStatistics(playerName, durationMs);

		log.debug("Player {} set to {} (duration: {}ms, streak: {})", playerName, statusText, durationMs, streak);
	}

	private void updateStatistics(String playerName, long durationMs)
	{
		PlayerStats stats = playerStats.get(playerName);
		if (stats == null)
		{
			stats = new PlayerStats(0, 0, System.currentTimeMillis());
		}
		stats.setAfkCount(stats.getAfkCount() + 1);
		stats.setTotalAfkTime(stats.getTotalAfkTime() + durationMs);
		stats.setLastAfkTime(System.currentTimeMillis());
		playerStats.put(playerName, stats);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		parseCustomKeywords();

		java.util.List<Player> players = client.getPlayers();
		if (players != null)
		{
			for (Player player : players)
			{
				if (player == null || player.getName() == null)
				{
					continue;
				}

				String playerName = normalizePlayerName(player.getName());
				
				if (playerName == null || playerName.isEmpty() || !afkPlayers.containsKey(playerName))
				{
					continue;
				}

				WorldPoint currentPosition = player.getWorldLocation();
				WorldPoint lastPosition = playerPositions.get(playerName);

				if (lastPosition != null && !currentPosition.equals(lastPosition))
				{
					AfkStatus removed = afkPlayers.remove(playerName);
					if (removed != null)
					{
						log.debug("Player {} removed AFK status (moved)", playerName);
					}
					playerPositions.remove(playerName);
				}
				else
				{
					playerPositions.put(playerName, currentPosition);
				}
			}
		}

		playerPositions.keySet().retainAll(afkPlayers.keySet());

		if (config.autoRemoveInactive())
		{
			long currentTime = System.currentTimeMillis();
			long timeoutMs = config.inactiveTimeoutMinutes() * 60 * 1000L;
			
			afkPlayers.entrySet().removeIf(entry -> {
				AfkStatus status = entry.getValue();
				long inactiveTime = currentTime - status.getLastActivityTime();
				return inactiveTime > timeoutMs;
			});
		}
	}

	@Provides
	BrbConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BrbConfig.class);
	}

	public Map<String, AfkStatus> getAfkPlayers()
	{
		return afkPlayers;
	}

	public Map<String, PlayerStats> getPlayerStats()
	{
		return playerStats;
	}

	public boolean isFriendOrClanMember(String playerName)
	{
		if (!config.highlightFriendsClan() || playerName == null)
		{
			return false;
		}

		String normalizedName = normalizePlayerName(playerName);
		if (client.isFriended(normalizedName, false))
		{
			return true;
		}

		FriendsChatManager friendsChatManager = client.getFriendsChatManager();
		if (friendsChatManager != null)
		{
			for (FriendsChatMember member : friendsChatManager.getMembers())
			{
				if (member != null)
				{
					String memberName = normalizePlayerName(member.getName());
					if (normalizedName.equals(memberName))
					{
						return true;
					}
				}
			}
		}

		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null)
		{
			for (ClanChannelMember member : clanChannel.getMembers())
			{
				if (member != null)
				{
					String memberName = normalizePlayerName(member.getName());
					if (normalizedName.equals(memberName))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	private Player findPlayerByName(String playerName)
	{
		java.util.List<Player> players = client.getPlayers();
		if (players == null)
		{
			return null;
		}

		for (Player player : players)
		{
			if (player != null)
			{
				String name = normalizePlayerName(player.getName());
				if (name != null && name.equals(playerName))
				{
					return player;
				}
			}
		}
		return null;
	}
}

