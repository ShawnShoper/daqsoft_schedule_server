package com.daqsoft.schedule.server.web.vo;

import java.util.Date;

import org.shoper.util.DateUtil;

public class ProviderVO
{
	private String address;
	private String group;
	private String provideName;
	private String version;
	private String status;

	private long serveTimes;
	/**
	 * Start time
	 */
	private long startTime;
	/**
	 * Holding task count
	 */
	private long holdeCount;
	// cpu used percent
	private double cpuIdlePercent;
	// mem used percent
	private double memUsedPercent;
	public long getServeTimes()
	{
		return serveTimes;
	}
	public void setServeTimes(long serveTimes)
	{
		this.serveTimes = serveTimes;
	}
	public String getStartTime()
	{
		return DateUtil.dateToString(DateUtil.DATE24, new Date(startTime));
	}
	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}
	public long getHoldeCount()
	{
		return holdeCount;
	}
	public void setHoldeCount(long holdeCount)
	{
		this.holdeCount = holdeCount;
	}

	public String getCpuIdlePercent()
	{
		return cpuIdlePercent + "%";
	}
	public void setCpuIdlePercent(double cpuIdlePercent)
	{
		this.cpuIdlePercent = cpuIdlePercent;
	}
	public String getMemUsedPercent()
	{
		return memUsedPercent + "%";
	}
	public void setMemUsedPercent(double memUsedPercent)
	{
		this.memUsedPercent = memUsedPercent;
	}
	public String getAddress()
	{
		return address;
	}
	public void setAddress(String address)
	{
		this.address = address;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	public String getProvideName()
	{
		return provideName;
	}
	public void setProvideName(String provideName)
	{
		this.provideName = provideName;
	}
	public String getVersion()
	{
		return version;
	}
	public void setVersion(String version)
	{
		this.version = version;
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}

}
