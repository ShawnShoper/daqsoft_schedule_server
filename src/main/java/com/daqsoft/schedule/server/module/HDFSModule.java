package com.daqsoft.schedule.server.module;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.HDFSInfo;
import com.daqsoft.schedule.conf.ZKInfo;
import com.daqsoft.schedule.manager.ZKModule;
/**
 * "Hadoop Distributed File System" module<br>
 * #TODO..目前尚未添加 zk 信息修改事件
 * 
 * @author ShawnShoper
 *
 */
@Component
public class HDFSModule extends ZKModule
{
	@Autowired
	private ZKInfo zkInfo;
	private Logger logger = LoggerFactory.getLogger(HDFSModule.class);
	@Autowired
	private HDFSInfo hdfsInfo;
	// Hadoop Distributed File System configuration
	Configuration conf;
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
	}
	@Override
	public int start()
	{
		try
		{
			if (super.start() == 0x01)
				return 1;
			initHDFS();
		} catch (Exception e)
		{
			e.printStackTrace();
			return 1;
		}
		setStarted(true);
		return 0;
	}
	private void initHDFS() throws KeeperException
	{
		try
		{
			HDFSInfo hdfsInfo = readData();
			hdfsInfo.setNodePath(this.hdfsInfo.getNodePath());
			this.hdfsInfo = hdfsInfo;
			conf = new Configuration();
			conf.set(hdfsInfo.getHostKey(), hdfsInfo.getHostValue());
		} catch (InterruptedException e)
		{
			;
		}
	}
	@Override
	protected void dataChangeProcess(WatchedEvent event)
	{
		try
		{
			initHDFS();
		} catch (KeeperException e)
		{
			e.printStackTrace();
		}
		logger.info("HDFS数据更改...{}", hdfsInfo);
	}
	/**
	 * Get File system operator
	 * 
	 * @return
	 * @throws IOException
	 */
	public FileSystem getFileSystem() throws IOException
	{
		while (!isStarted())
			try
			{
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		return FileSystem.get(conf);
	}
	/**
	 * readData
	 * 
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private HDFSInfo readData() throws KeeperException, InterruptedException
	{
		byte[] data = super.getZkClient().showData(hdfsInfo.getNodePath());
		String info = new String(data);
		return HDFSInfo.parseObject(info);
	}

}
