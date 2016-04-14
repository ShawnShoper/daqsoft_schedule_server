package com.daqsoft.schedule.server.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.daqsoft.schedule.conf.ApplicationInfo;
import com.daqsoft.schedule.server.system.RunningStatus;
import com.daqsoft.schedule.server.web.response.ResponseMsg;

@RestController
@RequestMapping("/sysConf")
public class SysconfigController
{
	@Autowired
	ApplicationInfo appInfo;
	@RequestMapping(value = "/getWSAddr", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String getWSAddress()
	{
		ResponseMsg responseMsg = new ResponseMsg();
		responseMsg.setData(appInfo.getBindAddr() + ":" + RunningStatus.port);
		return responseMsg.toJson();
	}
}
