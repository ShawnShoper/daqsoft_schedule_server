package com.daqsoft.schedule.server.module;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.zookeeper.KeeperException;
import org.shoper.redis.RedisClient;
import org.shoper.redis.RedisPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.conf.RedisInfo;
import com.daqsoft.schedule.conf.ZKInfo;
import com.daqsoft.schedule.manager.ZKModule;

@Component
public class RedisModule extends ZKModule
{
	@Autowired
	private ZKInfo zkInfo;
	@Autowired
	private ApplicationInfo appInfo;
	@Autowired
	LogModule logModule;
	RedisClient redisClient;
	@PostConstruct
	public void init()
	{
		super.setZkInfo(zkInfo);
	}
	@PreDestroy
	public void destory()
	{
		stop();
	}

	@Override
	public void stop()
	{
		super.stop();
		redisClient.close();
	}
	@Override
	public int start()
	{
		// Initialize redis info....
		if (super.start() == 1)
			return 1;
		try
		{
			RedisInfo redisInfo = readData();
			initial(redisInfo);
			logModule.info(RedisModule.class, "开始获取 provider 端提供的进度");

		} catch (KeeperException | InterruptedException e)
		{
			e.printStackTrace();
			return 1;
		}
		setStarted(true);
		return 0;
	}
	private void initial(RedisInfo redisInfo)
	{
		redisClient = RedisPool
				.newInstances(redisInfo.getHost(), redisInfo.getPort(),
						redisInfo.getTimeout(), redisInfo.getPassword())
				.getRedisClient();
	}

	private RedisInfo readData() throws KeeperException, InterruptedException
	{
		byte[] data = super.getZkClient().showData(appInfo.getRedisPath());
		String info = new String(data);
		return RedisInfo.parseObject(info);
	}
	public RedisClient getRedisClient()
	{
		return redisClient;
	}
}
