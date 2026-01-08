package com.example.model;

import lombok.Data;

@Data
public class AfkStatus
{
	private String status;
	private long endTime;
	private long startTime;
	private int streak;
	private long lastActivityTime;
	private String icon;

	public AfkStatus(String status, long endTime, long startTime, int streak, long lastActivityTime, String icon)
	{
		this.status = status;
		this.endTime = endTime;
		this.startTime = startTime;
		this.streak = streak;
		this.lastActivityTime = lastActivityTime;
		this.icon = icon;
	}

	public AfkStatus()
	{
		this("", 0, 0, 0, 0, "");
	}
}


