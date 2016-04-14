package com.daqsoft.schedule.server.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.SystemContext;
import com.daqsoft.schedule.conf.ZKInfo;
import com.daqsoft.schedule.module.MongoModule;
import com.daqsoft.schedule.server.job.TaskManager;
import com.daqsoft.schedule.server.module.HDFSModule;
import com.daqsoft.schedule.server.module.MailModule;
import com.daqsoft.schedule.server.module.ProviderManager;
import com.daqsoft.schedule.server.module.ProviderScanner;
import com.daqsoft.schedule.server.module.RedisModule;
import com.daqsoft.schedule.server.module.Registrar;
import com.daqsoft.schedule.server.module.consensus.ConsensusHelper;
import com.daqsoft.schedule.server.module.schedule.TaskSchedule;
import com.daqsoft.schedule.server.module.sub.TaskProgressLog;
import com.daqsoft.schedule.server.web.TaskController;
/**
 * spring context started listener...
 * 
 * @author ShawnShoper
 *
 */
@Component
public class StartedListener
		implements
			ApplicationListener<ContextRefreshedEvent>
{
	@Autowired
	ZKInfo zkInfo;
	@Autowired
	HDFSModule hDFSModule;

	@Autowired
	ProviderScanner providerScanner;
	@Autowired
	MongoModule mongo;
	@Autowired
	TaskManager taskManager;
	@Autowired
	TaskSchedule taskSchedule;
	@Autowired
	Registrar registrar;
	@Autowired
	RedisModule logModule;
	@Autowired
	ProviderManager providerManager;
	@Autowired
	MailModule mailModule;
	@Autowired
	TaskProgressLog taskProgressLog;
	@Autowired
	TaskController taskController;
	@Autowired
	ConsensusHelper consensusHelper;
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		try
		{
			// start master module
			registrar.start();
			// start reids log module
			logModule.start();
			// start providerScanner module
			providerScanner.start();

			providerManager.start();
			// task progress log
			taskProgressLog.start();
			// start mongo database operate module
			// 邮件系统
			mailModule.start();
			// mongo 数据库模块
			mongo.start();
			// start task management module
			taskManager.fire();
			// start schedule module
			taskSchedule.fire();
			// start hdfs module
			hDFSModule.start();
			consensusHelper.start();
		} catch (Exception e)
		{
			e.printStackTrace();
			SystemContext.shutdown();
		} finally
		{
		}
	}
}
