package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStats
{
	private int afkCount;
	private long totalAfkTime;
	private long lastAfkTime;
}


