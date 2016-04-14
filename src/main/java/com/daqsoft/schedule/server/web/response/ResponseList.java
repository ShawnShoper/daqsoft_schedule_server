package com.daqsoft.schedule.server.web.response;

import java.util.List;

public class ResponseList<T> extends ResponseDataVO
{
	private List<T> data;

	public List<T> getData()
	{
		return data;
	}

	public void setData(List<T> data)
	{
		this.data = data;
	}

}
