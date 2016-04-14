package com.daqsoft.schedule.server.module.sub;

import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureCallback;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.module.StartableModule;
import com.daqsoft.schedule.resp.ResultResponse;
import com.daqsoft.schedule.server.module.LogModule;
import com.daqsoft.schedule.server.module.RedisModule;
@Component
public class TaskProgressLog extends StartableModule
{
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	RedisModule redisModule;
	@Autowired
	LogModule logModule;

	@Override
	public int start()
	{
		FutureManager.pushFuture(appInfo.getName(), "log-progress-accepter",
				new AsynCallable<Boolean>() {
					@Override
					public Boolean run() throws Exception
					{
						for (;;)
						{
							try
							{
								String progress = redisModule.getRedisClient()
										.brpop("progress");
								ResultResponse response = ResultResponse
										.parseObject(progress);
								if (response != null)
								{
									String message = String.format(
											"任务名:[%s],执行端地址:[%s]启动时间:[%s],结束时间[%s],处理量:[%d],新增量:[%d],更新量:[%d],耗时:[%s]",

											response.getJobName(),
											response.getAddr(),
											DateUtil.TimeToString(
													DateUtil.DATE24_CN,
													response.getStartTime()),
											response.getEndTime() == 0
													? ""
													: DateUtil.TimeToString(
															DateUtil.DATE24_CN,
															response.getEndTime()),
											response.getHandCount(),
											response.getSaveCount(),
											response.getUpdateCount(),
											DateUtil.TimeToStr(response
													.getTimeConsuming()));
									logModule.info(TaskProgressLog.class,
											message);
								}
							} catch (Exception e)
							{
								// 因为如果没有明抛InterruptedException.是无法直接捕获该异常的。所以需要用
								// instanceof 判断子异常是否是'打断异常'
								if (e instanceof InterruptedException)
								{
									return false;
								}
								logModule.error(TaskProgressLog.class,
										"获取进度日志失败..", e);
							}
						}
					}
				}.setCallback(new FutureCallback<Boolean>() {
					@Override
					protected void fail(Exception e)
					{
						logModule.error(TaskProgressLog.class, "获取进度日志失败..", e);
					}

				}));
		setStarted(true);
		return 0;
	}
	@Override
	public void stop()
	{
		setStarted(false);
	}
}
