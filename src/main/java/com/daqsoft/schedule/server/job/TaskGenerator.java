package com.daqsoft.schedule.server.job;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.pojo.Task;
import com.daqsoft.schedule.server.module.LogModule;
import com.daqsoft.schedule.server.service.TaskService;

@Component
public class TaskGenerator
{
	@Autowired
	LogModule logModule;
	@Autowired
	TaskService taskService;
	@Autowired
	ApplicationInfo application;
	// private final String FUTURENAMENORMALTASK = "generatorNormalTask";

	@PostConstruct
	public void init()
	{
		logModule.info(TaskGenerator.class, "初始化 Task generator...");
	}

	@PreDestroy
	public void destroy()
	{
		// FutureManager.futureDone(application.getName(),
		// FUTURENAMENORMALTASK);
		logModule.info(TaskGenerator.class, "销毁 Task generator...");
	}

	/**
	 * 启动任务生成器
	 */
	public void fire()
	{
		// 启动任务生成器..

	}

	public List<Task> generateTimingTask()
	{
		return taskService.getTimingTask();
	}
	/**
	 * 生成任务...
	 */
	public List<Task> generateNotTimingTask()
	{
		return taskService.getTask(application.getTaskGenerateInterval(),
				application.getGenerateIntervalUnit(), false);
	}
}
