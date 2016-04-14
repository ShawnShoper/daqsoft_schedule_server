package com.daqsoft.schedule.server.module;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ProviderInfo;
import com.daqsoft.schedule.conf.ZKInfo;
import com.daqsoft.schedule.manager.ZKModule;
@Component
public class ProviderScanner extends ZKModule
{
	@Autowired
	LogModule logModule;
	@Autowired
	private ZKInfo zkInfo;
	@Autowired
	private ProviderInfo providerInfo;
	@Autowired
	private ProviderManager providerManager;

	@PostConstruct
	public void init()
	{
		setZkInfo(zkInfo);
	}
	@Override
	public int start()
	{
		if (super.start() == 1)
			return 1;
		try
		{
			scanner();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			return 1;
		}
		setStarted(true);
		return 0;
	}

	/**
	 * 扫描provider
	 * 
	 * @throws InterruptedException
	 */
	private void scanner() throws InterruptedException
	{
		logModule.info(ProviderScanner.class, "开始扫描 provider");
		try
		{
			List<String> nodes = super.getZkClient()
					.getChildren(providerInfo.getNodePath(), true);
			logModule.info(ProviderScanner.class,
					"发现provider个数[" + nodes.size() + "]");
			providerManager.pushAllProvider(nodes);
		} catch (KeeperException e)
		{
			e.printStackTrace();
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run()
				{
					try
					{
						scanner();
					} catch (InterruptedException e)
					{
						;
					}
					timer.cancel();
				}
			}, 0);
		}
	}
	@PreDestroy
	public void destroy()
	{
		super.getZkClient().close();
	}

	@Override
	public void childrenNodeChangeProcess(WatchedEvent event)
	{
		try
		{
			scanner();
		} catch (InterruptedException e)
		{
			;
		}
	}
}
