package com.daqsoft.schedule.server.module.schedule;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.daqsoft.schedule.SystemContext;
import com.daqsoft.schedule.pojo.Task;
import com.daqsoft.schedule.server.job.TaskManager;
import com.daqsoft.schedule.server.module.LogModule;
/**
 * 任务调度执行。
 * 
 * @author ShawnShoper 所有任务统一走这里,然后进行获取来实现provider真正的调度
 */
public class BaseJob implements Job
{
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException
	{
		LogModule logModule = SystemContext.getBean("logModule",
				LogModule.class);
		TaskManager taskManager = SystemContext.getBean("taskManager",
				TaskManager.class);
		Task task = (Task) context.getJobDetail().getJobDataMap().get("task");
		logModule.info(BaseJob.class, "执行定时任务调度.调度任务 【" + task.getName() + "】");
		try
		{
			taskManager.pushTask(task);
		} catch (InterruptedException e)
		{
			;
		}
	}
}
