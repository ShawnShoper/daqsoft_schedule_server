package com.daqsoft.schedule.server.module.schedule;

import java.util.concurrent.TimeUnit;

import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.AsynRunnable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.concurrent.future.RunnableCallBack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.exception.SystemException;
import com.daqsoft.schedule.pojo.Task;
import com.daqsoft.schedule.pojo.TaskMessage;
import com.daqsoft.schedule.server.connector.ProviderConnector;
import com.daqsoft.schedule.server.job.TaskManager;
import com.daqsoft.schedule.server.module.LogModule;
import com.daqsoft.schedule.server.module.ProviderManager;
import com.daqsoft.schedule.server.service.TaskService;

@Component
public class TaskSchedule {
	@Autowired
	LogModule logModule;
	@Autowired
	ApplicationInfo applicationInfo;
	@Autowired
	TaskService taskService;
	@Autowired
	TaskManager taskManager;
	@Autowired
	ProviderManager providerManager;
	@Autowired
	TimingSchedule timingSchedule;
	final String normalGenerator = "normalGenerator";
	final String timingGenerator = "timingGenerator";

	/**
	 * 0 running<br>
	 * 1 pending<br>
	 * 2 terminate<br>
	 */
	public void fire() {

		// 启动定时任务...
		FutureManager.pushFuture(applicationInfo.getName(), timingGenerator, new AsynCallable<Boolean>() {
			@Override
			public Boolean run() throws Exception {
				try {
					timingSchedule.schedule(taskManager.getAllTimingTask());
				} catch (SystemException e) {
					logModule.error(TaskSchedule.class, "schedule failed...", e);
				}
				return true;
			}
		});

		// 启动任务执行任务...
		FutureManager.pushFuture(applicationInfo.getName(), normalGenerator, new AsynCallable<Boolean>() {
			@Override
			public Boolean run() throws Exception {
				boolean flag = true;
				try {
					doTask();
				} catch (InterruptedException e) {
					flag = false;
				}
				return flag;
			}
		});
	}

	/**
	 * Generate task
	 * 
	 * @throws InterruptedException
	 */
	private void doTask() throws InterruptedException {
		// 0正常,1 无task,2 无 provider
		for (;;) {
			logModule.info(TaskSchedule.class, "Request get a avaliable task and provider...");
			try {

				TaskAndConnector taskAndConnector = getTaskAndConnector();

				if (taskAndConnector.status == 0x01) {
					logModule.info(TaskSchedule.class, "No task");
					// taskManager.waitTaskConsumer();
				} else if (taskAndConnector.status == 0x02) {
					logModule.info(TaskSchedule.class, "No provider");
					// providerManager.waitProvider();
				} else {
					logModule.info(TaskSchedule.class,
							"Send task to [" + taskAndConnector.thriftConnector.getThriftConnection().getID()
									+ "],task name [" + taskAndConnector.taskMessage.getTask().getName() + "]."

					);
					// 增加超时处理,避免由于 thrift 连接 IP 绑定错误时，无法返回的错误
					try {
						FutureManager.pushFuture(applicationInfo.getGroup(), "send-task", new AsynRunnable() {

							@Override
							public void call() throws Exception {
								taskAndConnector.thriftConnector.getThriftHandler()
										.sendTask(taskAndConnector.taskMessage);

							}
						}.setCallback(new RunnableCallBack() {
							@Override
							protected void fail(Exception e) {
								pushTaskFaild(taskAndConnector, e);
							}
						})).get(applicationInfo.getTimeout(), TimeUnit.SECONDS);
					} catch (Exception e) {
						pushTaskFaild(taskAndConnector, e);
					}
				}
			} catch (SystemException e1) {
				e1.printStackTrace();
			}
		}
	}

	void pushTaskFaild(TaskAndConnector taskAndConnector, Exception e) {
		try {
			logModule.error(TaskSchedule.class,
					"[TaskSchedule]:Send task error.return task [" + taskAndConnector.taskMessage.getTask().getName()
							+ "] and provider [" + taskAndConnector.thriftConnector.getThriftConnection().getID()
							+ "]...",

					e);
			taskManager.returnTask(taskAndConnector.taskMessage.getTask());
			providerManager.putBack(taskAndConnector.thriftConnector.getThriftConnection().getGroup(),
					taskAndConnector.thriftConnector.getThriftConnection().getID());
		} catch (InterruptedException e1) {
			;
		}
	}

	class TaskAndConnector {
		TaskMessage taskMessage;
		ProviderConnector thriftConnector;
		int status;
	}

	public TaskAndConnector getTaskAndConnector() throws InterruptedException, SystemException {
		TaskAndConnector taskAndConnector = new TaskAndConnector();
		for (;;) {
			Task task = taskManager.getTask(3, TimeUnit.MINUTES);
			if (task == null) {
				taskAndConnector.status = 1;
				return taskAndConnector;
			}
			String group = taskService.getTaskGroup(task.getTemplateID());
			// 未获取到数据,休眠...等待 task pool 获取到新的 task
			ProviderConnector connector = providerManager.getAvailableProvider(group, 3, TimeUnit.MINUTES, false);
			if (connector == null) {
				taskAndConnector.status = 2;
				taskManager.addTask(task);
				return taskAndConnector;
			}
			try {
				taskAndConnector.taskMessage = taskService.getTaskMessage(task);
			} catch (Exception e) {
				taskService.inverseStatus(task.getId(), "false", "1");
				e.printStackTrace();
			}
			taskAndConnector.thriftConnector = connector;
			break;
		}
		return taskAndConnector;
	}
}
