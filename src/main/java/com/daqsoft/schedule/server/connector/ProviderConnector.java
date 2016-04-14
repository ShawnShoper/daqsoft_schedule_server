package com.daqsoft.schedule.server.connector;

import com.daqsoft.schedule.connect.ProviderConnection;

/**
 * Thrift connector...<br>
 * 支持优先级调整筛选.实现算法参考 {@code CompareTo} 方法
 * 
 * @author ShawnShoper
 *
 */
public class ProviderConnector implements Comparable<ProviderConnector>
{
	private ProviderConnection thriftConnection;
	private ProviderHandler thriftHandler;
	// private StatusResponse statusResponse;
	private boolean disabled;
	private int priority;
	private boolean checkHeartbeatByRegistry = true;

	// public StatusResponse getStatusResponse()
	// {
	// return statusResponse;
	// }

	// public void setStatusResponse(StatusResponse statusResponse)
	// {
	// this.statusResponse = statusResponse;
	// }

	public void reset()
	{
		disabled = false;
		checkHeartbeatByRegistry = true;
	}

	public boolean isCheckHeartbeatByRegistry()
	{
		return checkHeartbeatByRegistry;
	}

	public void setCheckHeartbeatByRegistry(boolean checkHeartbeatByRegistry)
	{
		this.checkHeartbeatByRegistry = checkHeartbeatByRegistry;
	}

	public static ProviderConnector buider(ProviderConnection tc)
	{
		ProviderConnector thriftConnector = new ProviderConnector();
		thriftConnector.setConnecion(tc);
		ProviderHandler thriftHandler = new ProviderHandler(tc);
		thriftConnector.setThriftHandler(thriftHandler);
		return thriftConnector;
	}

	public ProviderHandler getThriftHandler()
	{
		return thriftHandler;
	}

	public void setThriftHandler(ProviderHandler thriftHandler)
	{
		this.thriftHandler = thriftHandler;
	}

	public void setConnecion(ProviderConnection thriftConnection)
	{
		this.thriftConnection = thriftConnection;
	}
	public ProviderConnection getThriftConnection()
	{
		return thriftConnection;
	}
	public void setThriftConnection(ProviderConnection thriftConnection)
	{
		this.thriftConnection = thriftConnection;
	}
	public synchronized void enabled()
	{
		if (disabled)
		{
			disabled = false;
		}
	}
	public synchronized void disabled()
	{
		if (!disabled)
			disabled = true;
	}

	public boolean isDisabled()
	{
		return disabled;
	}

	/**
	 * 用于权重判断... <br>
	 * -1那么 this 靠前，1this 靠后
	 */
	@Override
	public int compareTo(ProviderConnector o)
	{
		if (this.getPriority() > o.getPriority())
			return -1;
		else if (this.getPriority() == o.getPriority())
			return 0;
		// 用于比较...权重,提升优先级
		return 1;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}
	public int getPriority()
	{
		return this.priority;
	}
}
