package com.daqsoft.schedule.server.web;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.daqsoft.schedule.exception.SystemException;
import com.daqsoft.schedule.server.module.HDFSModule;
import com.daqsoft.schedule.server.service.ConsensusService;
import com.daqsoft.schedule.server.web.response.Response;
import com.daqsoft.schedule.server.web.response.ResponseMsg;

@RestController
@RequestMapping(value = "/consensus")
public class ConsensusContoller
{
	@Autowired
	ConsensusService consensusService;
	/**
	 * submit keyword 's charact
	 * 
	 * @param key
	 * @param charact
	 *            0.positive<br>
	 *            1.neutral<br>
	 *            2.Pejorative
	 * @return
	 */
	@RequestMapping(value = "/submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String submit(String key, Integer charact, HttpServletRequest req,
			HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{

			consensusService.submit(key, charact, req.getRemoteAddr());
		} catch (SystemException e)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return response.toJson();
	}
	/**
	 * Import need analyzed file
	 * 
	 * @param filePath
	 * @param resp
	 * @return
	 */
	@Autowired
	HDFSModule hDFSModule;
	@RequestMapping(value = "/import", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String importFile(String filePath, HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{
			consensusService.importFile(filePath);
		} catch (IOException e)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return response.toJson();
	}
	/**
	 * Get a word that need be analyzed with random
	 * 
	 * @param word
	 * @return
	 */
	@RequestMapping(value = "/random", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String random(String word)
	{
		ResponseMsg responseMsg = new ResponseMsg();
		responseMsg.setData(consensusService.random(word));
		return responseMsg.toJson();
	}
	@RequestMapping(value = "/remain", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String remain()
	{
		ResponseMsg responseMsg = new ResponseMsg();
		responseMsg.setData(consensusService.getRemain());
		return responseMsg.toJson();
	}
}
