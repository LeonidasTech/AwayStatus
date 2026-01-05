package com.example.config;

import com.example.enums.FontType;
import com.example.enums.HighlightType;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("brb")
public interface BrbConfig extends Config
{
	@ConfigSection(
		name = "Display Settings",
		description = "Settings for how AFK status is displayed",
		position = 0
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Text Settings",
		description = "Settings for text appearance and position",
		position = 1
	)
	String textSection = "text";

	@ConfigSection(
		name = "Advanced Features",
		description = "Additional features and customization",
		position = 2
	)
	String advancedSection = "advanced";

	@Range(min = -200, max = 200)
	@ConfigItem(
		keyName = "textOffsetX",
		name = "Text X Offset",
		description = "Horizontal offset for AFK text (negative = left, positive = right)",
		section = textSection,
		position = 0
	)
	default int textOffsetX()
	{
		return -5;
	}

	@Range(min = -200, max = 200)
	@ConfigItem(
		keyName = "textOffsetY",
		name = "Text Y Offset",
		description = "Vertical offset for AFK text (negative = up, positive = down)",
		section = textSection,
		position = 1
	)
	default int textOffsetY()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "textFontType",
		name = "Text Font",
		description = "Font type for AFK text",
		section = textSection,
		position = 2
	)
	default FontType textFontType()
	{
		return FontType.RUNESCAPE;
	}

	@ConfigItem(
		keyName = "textFontSize",
		name = "Text Font Size",
		description = "Font size for AFK text",
		section = textSection,
		position = 3
	)
	default int textFontSize()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "textFontBold",
		name = "Bold Text",
		description = "Use bold font for AFK text",
		section = textSection,
		position = 4
	)
	default boolean textFontBold()
	{
		return true;
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Text Color",
		description = "Color for AFK text",
		section = textSection,
		position = 5
	)
	default Color textColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "showCountdown",
		name = "Show Countdown",
		description = "Display countdown timer for time-based AFK messages",
		section = displaySection,
		position = 0
	)
	default boolean showCountdown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightAfkPlayer",
		name = "Highlight AFK Player",
		description = "Highlight the character of AFK players",
		section = displaySection,
		position = 1
	)
	default boolean highlightAfkPlayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightType",
		name = "Highlight Type",
		description = "Type of highlight to use for AFK players",
		section = displaySection,
		position = 2
	)
	default HighlightType highlightType()
	{
		return HighlightType.OUTLINE;
	}

	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight Color",
		description = "Color used to highlight AFK players",
		section = displaySection,
		position = 3
	)
	default Color highlightColor()
	{
		return new Color(0xFF0000);
	}


	@ConfigItem(
		keyName = "showMinimapIndicator",
		name = "Show Minimap Indicator",
		description = "Display indicator on minimap for AFK players",
		section = advancedSection,
		position = 0
	)
	default boolean showMinimapIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "customKeywords",
		name = "Custom Keywords",
		description = "Custom keywords and status messages (format: keyword:status:icon, e.g., coffee:Getting Coffee:☕,work:At Work:💼)",
		section = advancedSection,
		position = 1
	)
	default String customKeywords()
	{
		return "coffee:Getting Coffee:☕,work:At Work:💼";
	}

	@ConfigItem(
		keyName = "autoRemoveInactive",
		name = "Auto-Remove Inactive",
		description = "Automatically remove AFK status after player is inactive for specified minutes",
		section = advancedSection,
		position = 2
	)
	default boolean autoRemoveInactive()
	{
		return false;
	}

	@ConfigItem(
		keyName = "inactiveTimeoutMinutes",
		name = "Inactive Timeout (minutes)",
		description = "Minutes of inactivity before auto-removing AFK status",
		section = advancedSection,
		position = 3
	)
	default int inactiveTimeoutMinutes()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "highlightFriendsClan",
		name = "Highlight Friends/Clan",
		description = "Highlight friends and clan members more prominently",
		section = advancedSection,
		position = 4
	)
	default boolean highlightFriendsClan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "friendsClanColor",
		name = "Friends/Clan Highlight Color",
		description = "Color used to highlight friends and clan members",
		section = advancedSection,
		position = 5
	)
	default Color friendsClanColor()
	{
		return new Color(0xF100FF);
	}

}

