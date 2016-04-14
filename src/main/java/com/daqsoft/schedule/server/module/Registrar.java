package com.daqsoft.schedule.server.module;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.conf.Master;
import com.daqsoft.schedule.conf.ZKInfo;
import com.daqsoft.schedule.connect.ProviderConnection;
import com.daqsoft.schedule.connect.ProviderURLBuilder;
import com.daqsoft.schedule.face.ReportServer;
import com.daqsoft.schedule.manager.ZKModule;
import com.daqsoft.schedule.server.face.ReportServerHandler;
/**
 * 注册系统,用于定位做 master-slaver 的选举.<br>
 * 节点注册信息写入,监听master节点变化<br>
 * 
 * @author ShawnShoper
 *
 */
@Component
public class Registrar extends ZKModule
{
	@Autowired
	LogModule logModule;
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	private ZKInfo zkInfo;
	@Autowired
	private ThriftStarter thriftStarter;
	@Autowired
	private Master master;
	@PostConstruct
	public void init()
	{
		setZkInfo(zkInfo);
	}

	@Override
	protected void sessionExpired()
	{
		super.sessionExpired();
		super.startZookeeper();
		for (;;)
			try
			{
				registry();
				break;
			} catch (KeeperException e)
			{
				e.printStackTrace();
			} catch (InterruptedException e)
			{
				break;
			}
	}

	@Override
	public int start()
	{
		if (super.start() == 1)
			return 1;
		try
		{
			checkMaster();
			startThrift();
			registry();
		} catch (InterruptedException | KeeperException e)
		{
			return 1;
		}
		return 0;
	}
	private boolean checkMaster() throws KeeperException, InterruptedException
	{
		// 存在的话就不进行注册...不存在节点进行抢占式夺取 master.
		// 不存在那么进行注册..
		if (!super.getZkClient().exists(master.getNodePath()))
		{
			if (super.getZkClient().createNode(master.getNodePath(),
					CreateMode.EPHEMERAL))
			{
				return true;
			} else
			{
				return false;
			}
		}
		return false;
	}

	/**
	 * 构建 zk 节点.
	 * 
	 * @return
	 */
	String masterData()
	{
		ProviderConnection tc = new ProviderConnection();
		tc.setGroup(appInfo.getGroup());
		tc.setHost(appInfo.getBindAddr());
		tc.setPort(appInfo.getPort());
		tc.setProvideName(ReportServer.class.getName());
		tc.setTimeout(tc.getTimeout());
		tc.setUnit(TimeUnit.SECONDS);
		tc.setVersion(tc.getVersion());
		return StringUtil.urlEncode(ProviderURLBuilder.Builder().build(tc));
	}

	@Override
	public void nodeDeleteProcess(WatchedEvent event)
	{
		// 防止节点被外接删除...
		try
		{
			registry();
		} catch (KeeperException e)
		{
			e.printStackTrace();
		} catch (InterruptedException e)
		{
			;
		}
	}
	void createMonitorNode(String path)
			throws KeeperException, InterruptedException
	{
		super.getZkClient().createNode(path, masterData(),
				CreateMode.EPHEMERAL);
	}
	/**
	 * Registry zookeeper
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private void registry() throws KeeperException, InterruptedException
	{

		String path = master.getNodePath();
		if (super.getZkClient().exists(path))
		{
			super.getZkClient().editData(path, masterData());
			// super.getZkClient().deleteNode(path);
		} else
		{
			createMonitorNode(path);
		}
	}
	/**
	 * start thrift
	 * 
	 * @throws InterruptedException
	 */
	private void startThrift() throws InterruptedException
	{
		thriftStarter.start(appInfo.getBindAddr(), appInfo.getPort());
		while (!thriftStarter.isStarted())
		{
			TimeUnit.MILLISECONDS.sleep(10);
		}
		logModule.info(Registrar.class, "Thrift started...");
	}

	@PreDestroy
	public void destroy()
	{
		super.stop();
		logModule.debug(Registrar.class, "Registrar destroy");
	}
}
@Component
class ThriftStarter extends AsynCallable<Boolean>
{
	private final String name = "thrift-starter";
	@Autowired
	ReportServerHandler reportServerHandler;
	private int port;
	private String host;
	TServer server;
	public Future<Boolean> result;
	public Future<Boolean> getResult()
	{
		return result;
	}
	@PreDestroy
	public void shutdown()
	{
		if (server != null)
			server.stop();
		FutureManager.futureDone(null, name);
	}
	/**
	 * 启动 thrift
	 * 
	 * @param port
	 * @return
	 */
	public void start(String host, int port)
	{
		this.port = port;
		this.host = host;
		result = (FutureManager.pushFuture(null, name, this));
	}
	@Override
	public Boolean run() throws Exception
	{
		try
		{
			startServer();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}
	public boolean isStarted()
	{
		if (server == null)
			return false;
		return server.isServing();
	}
	private void startServer() throws TTransportException
	{
		TMultiplexedProcessor processor = new TMultiplexedProcessor();
		InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
		TServerTransport t = new TServerSocket(inetSocketAddress);
		server = new TThreadPoolServer(
				new TThreadPoolServer.Args(t).processor(processor));
		processor.registerProcessor(ReportServer.class.getName(),
				new ReportServer.Processor<ReportServer.Iface>(
						reportServerHandler));
		server.serve();
	}

}
