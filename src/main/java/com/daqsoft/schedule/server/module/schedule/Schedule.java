package com.daqsoft.schedule.server.module.schedule;

import org.quartz.Scheduler;

import com.daqsoft.schedule.exception.SystemException;
import com.daqsoft.schedule.pojo.Task;

public interface Schedule
{
	public void schedule(Task task) throws SystemException;
	public void shutdownJob(Scheduler sched, String name, String group)
			throws SystemException;
}
