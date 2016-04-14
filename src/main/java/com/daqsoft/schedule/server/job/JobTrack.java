package com.daqsoft.schedule.server.job;

import java.util.concurrent.ConcurrentHashMap;

import org.shoper.util.StringUtil;

import com.daqsoft.schedule.connect.ProviderConnection;

public class JobTrack
{
	private static volatile ConcurrentHashMap<ProviderConnection, String[]> holdJobs = new ConcurrentHashMap<>();
	public static String[] getTaskHoldJobs(String provider)
	{
		if (StringUtil.isEmpty(provider))
			throw new NullPointerException();
		return holdJobs.get(provider);
	}
}
