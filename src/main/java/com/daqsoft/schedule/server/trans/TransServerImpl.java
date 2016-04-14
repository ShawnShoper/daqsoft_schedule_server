package com.daqsoft.schedule.server.trans;

import java.util.List;

import org.apache.thrift.TException;

import com.daqsoft.schedule.face.TransServer;

public class TransServerImpl implements TransServer.Iface{

	@Override
	public String sendTask(String task) throws TException {
		
		return null;
	}

	@Override
	public String getStatus() throws TException {
		return null;
	}

	@Override
	public List<String> getAllRunning() throws TException {
		return null;
	}
}
