package com.daqsoft.schedule.server.connector;

import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import com.daqsoft.schedule.connect.ProviderConnection;
import com.daqsoft.schedule.exception.RPCConnectionException;
import com.daqsoft.schedule.exception.SystemException;
import com.daqsoft.schedule.face.TransServer;
import com.daqsoft.schedule.pojo.TaskMessage;
import com.daqsoft.schedule.resp.AcceptResponse;
import com.daqsoft.schedule.resp.StatusResponse;

public class ProviderHandler
{
	private volatile TransServer.Client transServerClient;
	private volatile ProviderConnection thriftConnection;
	TSocket socket = null;

	public ProviderHandler(ProviderConnection thriftConnection)
	{
		this.thriftConnection = thriftConnection;
	}

	public TransServer.Client getTransServerClient()
	{
		return transServerClient;
	}

	public void setTransServerClient(TransServer.Client transServerClient)
	{
		this.transServerClient = transServerClient;
	}
	/**
	 * Connecting remote server...<br>
	 * if connect fail , will try 2 times to reconnect
	 * 
	 * @return server instance
	 * @throws SystemException
	 * @throws RPCConnectionException
	 */
	public TransServer.Client connect() throws RPCConnectionException
	{
		boolean flag = false;

		for (int i = 0; i < 3; i++)
		{
			try
			{
				socket = new TSocket(thriftConnection.getHost(),
						thriftConnection.getPort());
				TBinaryProtocol protocol = new TBinaryProtocol(socket);
				TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,
						thriftConnection.getProvideName());
				transServerClient = new TransServer.Client(mp1);
				socket.open();
				flag = true;
				break;
			} catch (TTransportException e)
			{
				;// Retry
			}
		}
		if (!flag)
			throw new RPCConnectionException("连接" + thriftConnection.getHost()
					+ ":" + thriftConnection.getPort() + "失败.");
		return transServerClient;
	}

	public void close()
	{
		socket.close();
	}

	public AcceptResponse sendTask(TaskMessage taskMessage)
			throws RPCConnectionException
	{
		AcceptResponse acceptResponse = null;
		try
		{
			acceptResponse = AcceptResponse
					.parseObject(connect().sendTask(taskMessage.toJson()));
		} catch (TException e)
		{
			throw new RPCConnectionException(e);
		} finally
		{
			close();
		}
		return acceptResponse;
	}

	public StatusResponse getStatus() throws RPCConnectionException
	{
		StatusResponse result = null;
		try
		{
			result = StatusResponse.parseObject(connect().getStatus());
		} catch (TException e)
		{
			throw new RPCConnectionException(e);
		} finally
		{
			close();
		}
		return result;
	}

	public List<String> getAllRunning() throws RPCConnectionException
	{
		List<String> result;
		try
		{

			result = connect().getAllRunning();
		} catch (TException e)
		{
			throw new RPCConnectionException(e);
		} finally
		{
			close();
		}
		return result;
	}

}
