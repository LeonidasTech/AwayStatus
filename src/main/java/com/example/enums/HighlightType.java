package com.example.enums;

public enum HighlightType
{
	TILE("Tile"),
	GLOW("Glow"),
	OUTLINE("Outline");

	private final String displayName;

	HighlightType(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}

