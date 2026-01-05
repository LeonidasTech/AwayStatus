package com.example.enums;

public enum FontType
{
	RUNESCAPE("RuneScape"),
	ARIAL("Arial"),
	VERDANA("Verdana"),
	TAHOMA("Tahoma"),
	COURIER_NEW("Courier New"),
	TIMES_NEW_ROMAN("Times New Roman"),
	COMIC_SANS_MS("Comic Sans MS"),
	IMPACT("Impact"),
	LUCIDA_CONSOLE("Lucida Console");

	private final String fontName;

	FontType(String fontName)
	{
		this.fontName = fontName;
	}

	public String getFontName()
	{
		return fontName;
	}
}

