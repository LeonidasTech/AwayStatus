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
			case DISCO:
				renderDiscoHighlight(graphics, player, localPoint);
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

	private void renderDiscoHighlight(Graphics2D graphics, Player player, LocalPoint localPoint)
	{
		int width = config.highlightWidth();
		int feather = config.highlightFeather();
		
		// Generate rainbow color based on current time
		Color rainbowColor = getRainbowColor();
		
		Color fillColor = new Color(
			rainbowColor.getRed(),
			rainbowColor.getGreen(),
			rainbowColor.getBlue(),
			150
		);

		// Create multiple layers for glow effect with rainbow color
		int outerWidth = width + feather * 2;
		int midWidth = width + feather;
		
		if (outerWidth > 0)
		{
			modelOutlineRenderer.drawOutline(player, outerWidth, fillColor, feather * 2);
		}
		if (midWidth > 0)
		{
			modelOutlineRenderer.drawOutline(player, midWidth, fillColor, feather);
		}
		if (width > 0)
		{
			modelOutlineRenderer.drawOutline(player, width, rainbowColor, 0);
		}
	}

	private void renderOutlineHighlight(Graphics2D graphics, Player player, LocalPoint localPoint, Color color)
	{
		int width = config.highlightWidth();
		int feather = config.highlightFeather();
		modelOutlineRenderer.drawOutline(player, width, color, feather);
	}

	private Color getRainbowColor()
	{
		long time = System.currentTimeMillis();
		// Cycle through hue over 3 seconds (3000ms)
		float hue = (time % 3000) / 3000.0f;
		
		// Convert HSL to RGB
		// H: hue (0-1), S: saturation (1.0 = full), L: lightness (0.5 = medium)
		float saturation = 1.0f;
		float lightness = 0.5f;
		
		float c = (1 - Math.abs(2 * lightness - 1)) * saturation;
		float x = c * (1 - Math.abs((hue * 6) % 2 - 1));
		float m = lightness - c / 2;
		
		float r, g, b;
		if (hue < 1.0f / 6)
		{
			r = c;
			g = x;
			b = 0;
		}
		else if (hue < 2.0f / 6)
		{
			r = x;
			g = c;
			b = 0;
		}
		else if (hue < 3.0f / 6)
		{
			r = 0;
			g = c;
			b = x;
		}
		else if (hue < 4.0f / 6)
		{
			r = 0;
			g = x;
			b = c;
		}
		else if (hue < 5.0f / 6)
		{
			r = x;
			g = 0;
			b = c;
		}
		else
		{
			r = c;
			g = 0;
			b = x;
		}
		
		return new Color(
			(int) ((r + m) * 255),
			(int) ((g + m) * 255),
			(int) ((b + m) * 255)
		);
	}
}


