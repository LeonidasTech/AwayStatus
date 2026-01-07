package com.example.overlay;

import com.example.BrbPlugin;
import com.example.config.BrbConfig;
import com.example.enums.FontType;
import com.example.enums.HighlightType;
import com.example.model.AfkStatus;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class BrbOverlay extends Overlay
{
	private final Client client;
	private final BrbPlugin plugin;
	private final BrbConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private BrbOverlay(Client client, BrbPlugin plugin, BrbConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Map<String, AfkStatus> afkPlayers = plugin.getAfkPlayers();
		
		if (afkPlayers.isEmpty())
		{
			return null;
		}

		List<Player> players = client.getPlayers();
		if (players == null || players.isEmpty())
		{
			return null;
		}

		for (Player player : players)
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}

			String normalizedName = plugin.normalizePlayerName(player.getName());
			if (normalizedName == null || normalizedName.isEmpty())
			{
				continue;
			}

			AfkStatus status = afkPlayers.get(normalizedName);
			if (status == null)
			{
				continue;
			}

			boolean isFriendOrClan = plugin.isFriendOrClanMember(normalizedName);
			Color highlightColor = isFriendOrClan && config.highlightFriendsClan() 
				? config.friendsClanColor() 
				: config.highlightColor();

			if (config.highlightAfkPlayer())
			{
				renderPlayerHighlight(graphics, player, config.highlightType(), highlightColor);
			}

			renderPlayerText(graphics, player, status, isFriendOrClan);
		}

		return null;
	}

	private void renderPlayerText(Graphics2D graphics, Player player, AfkStatus status, boolean isFriendOrClan)
	{
		LocalPoint localPoint = player.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		String displayText = status.getStatus();
		
		if (config.textFontType() == FontType.RUNESCAPE && status.getIcon() != null && !status.getIcon().isEmpty())
		{
			displayText = status.getIcon() + " " + displayText;
		}
		
		if (config.showCountdown())
		{
			if (status.getEndTime() > 0)
			{
				long remainingMs = status.getEndTime() - System.currentTimeMillis();
				if (remainingMs > 0)
				{
					long remainingSeconds = remainingMs / 1000;
					long minutes = remainingSeconds / 60;
					long seconds = remainingSeconds % 60;
					
					if (minutes > 0)
					{
						displayText += String.format(" (%dm %ds)", minutes, seconds);
					}
					else
					{
						displayText += String.format(" (%ds)", seconds);
					}
				}
				else
				{
					long lateMs = System.currentTimeMillis() - status.getEndTime();
					long lateSeconds = lateMs / 1000;
					long lateMinutes = lateSeconds / 60;
					lateSeconds = lateSeconds % 60;
					
					if (lateMinutes > 0)
					{
						displayText += String.format(" (%dm %ds late)", lateMinutes, lateSeconds);
					}
					else
					{
						displayText += String.format(" (%ds late)", lateSeconds);
					}
				}
			}
			else
			{
				long elapsedMs = System.currentTimeMillis() - status.getStartTime();
				long elapsedSeconds = elapsedMs / 1000;
				long minutes = elapsedSeconds / 60;
				long seconds = elapsedSeconds % 60;
				
				if (minutes > 0)
				{
					displayText += String.format(" (%dm %ds)", minutes, seconds);
				}
				else
				{
					displayText += String.format(" (%ds)", seconds);
				}
			}
		}

		Point textLocation = Perspective.getCanvasTextLocation(
			client,
			graphics,
			localPoint,
			displayText,
			player.getLogicalHeight() + config.textOffsetY()
		);

		if (textLocation == null)
		{
			return;
		}

		int x = textLocation.getX() + config.textOffsetX();
		int y = textLocation.getY();

		Font originalFont = graphics.getFont();
		Color originalColor = graphics.getColor();
		
		Font textFont;
		if (config.textFontType() == FontType.RUNESCAPE)
		{
			Font baseFont = FontManager.getRunescapeFont();
			if (baseFont == null)
			{
				baseFont = FontManager.getRunescapeSmallFont();
			}
			
			if (baseFont != null)
			{
				textFont = baseFont.deriveFont((float) config.textFontSize());
			}
			else
			{
				textFont = new Font("Arial", Font.PLAIN, config.textFontSize());
			}
		}
		else
		{
			int fontStyle = config.textFontBold() ? Font.BOLD : Font.PLAIN;
			textFont = new Font(config.textFontType().getFontName(), fontStyle, config.textFontSize());
		}
		
		Color textColor = isFriendOrClan && config.highlightFriendsClan() 
			? config.friendsClanColor() 
			: config.textColor();

		graphics.setFont(textFont);
		graphics.setColor(textColor);

		graphics.setColor(Color.BLACK);
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				if (dx != 0 || dy != 0)
				{
					graphics.drawString(displayText, x + dx, y + dy);
				}
			}
		}
		
		graphics.setColor(textColor);
		graphics.drawString(displayText, x, y);

		graphics.setFont(originalFont);
		graphics.setColor(originalColor);
	}

	private void renderPlayerHighlight(Graphics2D graphics, Player player, HighlightType highlightType, Color color)
	{
		LocalPoint localPoint = player.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		switch (highlightType)
		{
			case TILE:
				renderTileHighlight(graphics, localPoint, color);
				break;
			case GLOW:
				renderGlowHighlight(graphics, player, localPoint, color);
				break;
			case OUTLINE:
				renderOutlineHighlight(graphics, player, localPoint, color);
				break;
		}
	}

	private void renderTileHighlight(Graphics2D graphics, LocalPoint localPoint, Color color)
	{
		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null)
		{
			return;
		}

		graphics.setColor(color);
		graphics.setStroke(new java.awt.BasicStroke(2));
		graphics.drawPolygon(tilePoly);
		
		Color fillColor = new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			50
		);
		graphics.setColor(fillColor);
		graphics.fillPolygon(tilePoly);
	}

	private void renderGlowHighlight(Graphics2D graphics, Player player, LocalPoint localPoint, Color color)
	{
		Color fillColor = new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			150
		);

		modelOutlineRenderer.drawOutline(player, 12, fillColor, 8);
		modelOutlineRenderer.drawOutline(player, 10, fillColor, 6);
		modelOutlineRenderer.drawOutline(player, 8, fillColor, 4);
		modelOutlineRenderer.drawOutline(player, 2, color, 0);
	}

	private void renderOutlineHighlight(Graphics2D graphics, Player player, LocalPoint localPoint, Color color)
	{
		modelOutlineRenderer.drawOutline(player, 3, color, 0);
	}
}


