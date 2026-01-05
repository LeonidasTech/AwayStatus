package com.example.overlay;

import com.example.BrbPlugin;
import com.example.config.BrbConfig;
import com.example.model.AfkStatus;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class BrbMinimapOverlay extends Overlay
{
	private final Client client;
	private final BrbPlugin plugin;
	private final BrbConfig config;

	@Inject
	private BrbMinimapOverlay(Client client, BrbPlugin plugin, BrbConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showMinimapIndicator())
		{
			return null;
		}

		Map<String, AfkStatus> afkPlayers = plugin.getAfkPlayers();
		if (afkPlayers.isEmpty())
		{
			return null;
		}

		java.util.List<Player> players = client.getPlayers();
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

			AfkStatus status = afkPlayers.get(player.getName());
			if (status == null)
			{
				continue;
			}

			renderMinimapIndicator(graphics, player, status);
		}

		return null;
	}

	private void renderMinimapIndicator(Graphics2D graphics, Player player, AfkStatus status)
	{
		LocalPoint localPoint = player.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		Point minimapPoint = Perspective.localToMinimap(client, localPoint);
		if (minimapPoint == null)
		{
			return;
		}

		boolean isFriendOrClan = plugin.isFriendOrClanMember(player.getName());
		Color indicatorColor = isFriendOrClan && config.highlightFriendsClan() 
			? config.friendsClanColor() 
			: config.highlightColor();

		int x = (int) minimapPoint.getX() - 3;
		int y = (int) minimapPoint.getY() - 3;
		graphics.setColor(indicatorColor);
		graphics.fillOval(x, y, 6, 6);
		graphics.setColor(Color.WHITE);
		graphics.drawOval(x, y, 6, 6);
	}
}

