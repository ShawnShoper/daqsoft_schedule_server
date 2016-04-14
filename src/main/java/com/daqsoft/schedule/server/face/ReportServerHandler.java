package com.daqsoft.schedule.server.face;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daqsoft.schedule.face.ReportServer;
import com.daqsoft.schedule.server.service.TaskService;
/**
 * report server
 * 
 * @author ShawnShoper
 *
 */
@Component
public class ReportServerHandler implements ReportServer.Iface
{
	@Autowired
	TaskService taskService;
	@Override
	public int reportJobDone(String report) throws TException
	{
		try
		{
			taskService.report(report);
		} catch (InterruptedException e)
		{
			; // TODO Auto-generated catch block
		}
		return 0;
	}

}
