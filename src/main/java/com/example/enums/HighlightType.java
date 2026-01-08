package com.example.enums;

public enum HighlightType
{
	TILE("Tile"),
	DISCO("Disco"),
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


