package com.daqsoft.schedule.server.module;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.zookeeper.KeeperException;
import org.shoper.concurrent.future.AsynRunnable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.concurrent.future.RunnableCallBack;
import org.shoper.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.conf.ProviderInfo;
import com.daqsoft.schedule.conf.ZKInfo;
import com.daqsoft.schedule.connect.ProviderConnection;
import com.daqsoft.schedule.connect.ProviderURLBuilder;
import com.daqsoft.schedule.exception.RPCConnectionException;
import com.daqsoft.schedule.manager.ZKModule;
import com.daqsoft.schedule.resp.StatusResponse;
import com.daqsoft.schedule.server.connector.ProviderConnector;

/**
 * ProviderQueue<br>
 * store provider ..<br>
 * 
 * @author ShawnShoper
 */
@Component
public class ProviderManager extends ZKModule
{
	@Autowired
	LogModule logModule;
	@Autowired
	ProviderInfo providerInfo;
	/**
	 * Has been exists provider collections. <br>
	 * connections key is group ID.
	 */
	private volatile ConcurrentSkipListMap<String, Map<String, ProviderConnector>> connections = new ConcurrentSkipListMap<>();
	/**
	 * Available provider queue.Priority. <br>
	 * Choose the highest priority Connectors<br>
	 * Details please see ThriftConnector method {@code compareTo}
	 */
	private volatile Map<String, PriorityBlockingQueue<ProviderConnector>> connectors = new ConcurrentHashMap<String, PriorityBlockingQueue<ProviderConnector>>();
	public volatile boolean isWaitting;
	Timer timer;
	@Autowired
	private ApplicationInfo application;
	@Autowired
	ZKInfo zkInfo;
	@PostConstruct
	public void init()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
		setZkInfo(zkInfo);
	}

	@Override
	public int start()
	{
		if (super.start() == 1)
			return 1;
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
		timer = new Timer(true);
		// 扫描各个节点的状态检查心跳
		timer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				try
				{
					checkHeartBeat();
				} catch (InterruptedException e)
				{
					timer.cancel();
				}
			}
		}, 0, 5000);
		return 0;
	}
	/**
	 * 检测心跳...
	 * 
	 * @throws InterruptedException
	 */
	private void checkHeartBeat() throws InterruptedException
	{

		for (String key : connections.keySet())
		{
			for (String id : connections.get(key).keySet())
			{
				ProviderConnector tc = connections.get(key).get(id);
				// if (!tc.isDisabled())
				FutureManager.pushFuture(application.getName(),
						"check-heatbeat-" + key, new AsynRunnable() {
							@Override
							public void call() throws Exception
							{
								StatusResponse statusResponse = null;
								// 3 times for check heartBeatByRegistry
								for (int i = 0; statusResponse == null
										&& i < 3; i++)
								{
									if (!tc.isCheckHeartbeatByRegistry())
									{
										System.out.println(tc
												.getThriftConnection() + "---"
												+ tc.isCheckHeartbeatByRegistry());
										try
										{
											statusResponse = tc
													.getThriftHandler()
													.getStatus();
										} catch (RPCConnectionException e)
										{
											// Connection failed by thrift
											// direct connect,maybe network
											// error or provider down.
										}
									} else
									{
										// 通过注册中心去取服务器状态
										try
										{
											statusResponse = getStatusRespByZK(
													tc.getThriftConnection());
										} catch (KeeperException e)
										{
											// Connection failed by zookeeper
											// maybe network
											// error or provider down ,so switch
											// manual check by thrift direct
											// connect
										}
									}
								}
								if (statusResponse == null)
								{
									if (tc.isCheckHeartbeatByRegistry())
									{
										tc.setCheckHeartbeatByRegistry(false);
									}
									tc.disabled();
								} else
								{
									tc.enabled();
									// tc.setStatusResponse(statusResponse);
								}
							}
						}.setCallback(new RunnableCallBack() {

							@Override
							protected void fail(Exception e)
							{
								e.printStackTrace();
							}
						}));
			}
		}
	}
	/**
	 * 通过 ZK 注册中心获取 cluster status
	 * 
	 * @param tc
	 * @return
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public StatusResponse getStatusRespByZK(ProviderConnection tc)
			throws InterruptedException, KeeperException
	{
		StatusResponse sr = null;
		String response = null;
		try
		{
			response = new String(
					super.getZkClient().showData(providerInfo.getNodePath()
							+ "/" + StringUtil.urlEncode(tc.getOriginPath())));
			sr = JSONObject.parseObject(response, StatusResponse.class);
		} catch (JSONException e)
		{
			logModule.info(ProviderManager.class,
					"通过 ZK获取【" + tc.getHost() + ":" + tc.getPort() + "/"
							+ tc.getProvideName() + "】,解析 json 失败状态失败" + "\n"
							+ response);
		}
		return sr;
	}
	@PreDestroy
	public void destroy()
	{
		super.stop();
		connections.clear();
		if (timer != null)
			timer.cancel();
	}
	/**
	 * Pushing all provider into manager when application start up.
	 * 
	 * @param nodes
	 *            cluster 节点...
	 * @throws InterruptedException
	 */
	public void pushAllProvider(List<String> nodes) throws InterruptedException
	{
		for (String node : nodes)
		{
			try
			{
				addProvider(StringUtil.urlDecode(node));
			} catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
	}
	/**
	 * add new provider.reset status if exists<br>
	 * put to provider pool and offer to queue if not exists<br>
	 * modify checkHeartbeat way if exists<br>
	 * 
	 * @param path
	 *            of thrift provider addr
	 * @throws InterruptedException
	 */
	public void addProvider(String path) throws InterruptedException
	{

		ProviderConnection tctmp = ProviderURLBuilder.Builder().deBuild(path);
		String group = tctmp.getGroup();
		String key = buildKey(tctmp);
		// In advance check provider's group has exists in connections
		{
			if (!connections.containsKey(group))
				connections.put(group, new HashMap<>());
			if (!connectors.containsKey(group))
				connectors.put(group, new PriorityBlockingQueue<>());
		}
		PriorityBlockingQueue<ProviderConnector> pbqs = connectors.get(group);
		Map<String, ProviderConnector> ctrs = connections.get(group);
		if (!ctrs.containsKey(key))
		{
			logModule.info(ProviderManager.class,
					"Increased provider [" + tctmp + "]");
			ProviderConnector thriftConnector = ProviderConnector.buider(tctmp);
			ctrs.put(key, thriftConnector);

			if (!pbqs.offer(thriftConnector, 1, TimeUnit.SECONDS))
			{
				logModule.warn(ProviderManager.class,
						"Offer connector failed ,the queue is full...");
			}
		} else
		{
			logModule.info(ProviderManager.class,
					"Update provider [" + tctmp + "]");
			ProviderConnector tc = ctrs.get(key);
			if (!tc.isCheckHeartbeatByRegistry())
				tc.setCheckHeartbeatByRegistry(true);
			if (tc.isDisabled())
				ctrs.get(key).enabled();
			if (!pbqs.offer(tc, 1, TimeUnit.SECONDS))
			{
				logModule.warn(ProviderManager.class,
						"Offer connector failed ,the queue is full...");
			}
		}
	}
	/**
	 * 根据传递的 ThriftConnection 生成一个 key 作为 map 的 key
	 * 
	 * @param tc
	 * @return
	 */
	private String buildKey(ProviderConnection tc)
	{
		return tc.getHost() + tc.getPort() + tc.getVersion();
	}
	/**
	 * delete a provider.if some one down.
	 * 
	 * @param path
	 *            path of thrift provider addr
	 */
	public void deleteProvider(String path)
	{
		ProviderConnection tc = ProviderURLBuilder.Builder().deBuild(path);
		String group = tc.getGroup();
		String key = buildKey(tc);
		if (connections.containsKey(group))
		{
			Map<String, ProviderConnector> connectorGroup = connections
					.get(group);
			if (connectorGroup.containsKey(key))
			{
				ProviderConnector connector = connectorGroup.get(key);
				connector.disabled();
				if (connectors.containsKey(group))
					if (connectors.get(group).contains(connector))
						connections.remove(connector);
			}
		}
	}
	/**
	 * get available provider
	 * 
	 * @param timeout
	 *            timeout
	 * @param unit
	 *            time unit
	 * @param isTiming
	 *            timing or not timing
	 * @return
	 * @throws InterruptedException
	 */
	public ProviderConnector getAvailableProvider(String group, int timeout,
			TimeUnit unit, boolean isTiming) throws InterruptedException
	{
		ProviderConnector connector = null;
		if (!connectors.containsKey(group))
			connectors.put(group, new PriorityBlockingQueue<>());
		connector = connectors.get(group).poll(timeout, unit);
		return connector;
	}

	public Map<String, Map<String, ProviderConnector>> takeAllProvider()
	{
		return this.connections;
	}
	/**
	 * 还回 cluster
	 * 
	 * @param group
	 * @param providerKey
	 * @throws InterruptedException
	 */
	public void putBack(String group, String providerKey)
			throws InterruptedException
	{
		System.out.println(group + "----" + providerKey);
		if (connections.containsKey(group))
		{
			Map<String, ProviderConnector> connctor = connections.get(group);
			ProviderConnector thriftConnector = connctor.get(providerKey);
			logModule.info(ProviderManager.class, "Provider ["
					+ thriftConnector.getThriftConnection() + "] 执行完毕，重新待命...");
			if (connectors.containsKey(group))
				while (!connectors.get(group).offer(thriftConnector, 2,
						TimeUnit.SECONDS));
		}
	}
}
