package com.daqsoft.schedule.server.service;

import java.io.IOException;

import org.shoper.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daqsoft.schedule.exception.SystemException;
import com.daqsoft.schedule.server.module.consensus.ConsensusHelper;

@Service
public class ConsensusService
{
	@Autowired
	ConsensusHelper consensusHelper;
	public void submit(String keyword, Integer charact, String ip)
			throws SystemException
	{
		if (charact != null && charact.intValue() != 0
				&& charact.intValue() != 1 && charact.intValue() != 2
				&& charact.intValue() != 3)
			throw new SystemException(
					"提交词性参数错误,范围0-2。0.positive		1.neutral		2.Pejorative	 3.none");
		if (StringUtil.isEmpty(keyword))
			throw new SystemException("提交请求参数不能为空...");
		consensusHelper.removeWord(keyword);
		consensusHelper.prepareToWrite(keyword, charact, ip);
	}
	public void importFile(String file) throws IOException
	{
		consensusHelper.importWords(file);
	}
	public String random(String keyword)
	{
		if (!StringUtil.isEmpty(keyword))
			consensusHelper.pushWord(keyword);
		String word = consensusHelper.poll();
		return word;
	}

	public Object getRemain()
	{
		return consensusHelper.getRemainCount();
	}
}
