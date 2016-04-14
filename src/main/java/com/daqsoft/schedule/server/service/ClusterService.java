package com.daqsoft.schedule.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daqsoft.schedule.connect.ProviderConnection;
import com.daqsoft.schedule.server.connector.ProviderConnector;
import com.daqsoft.schedule.server.module.ProviderManager;
import com.daqsoft.schedule.server.web.vo.ProviderVO;

@Service
public class ClusterService
{
	@Autowired
	ProviderManager providerManager;
	public List<ProviderVO> getAllCluster()
	{
		Map<String, Map<String, ProviderConnector>> connections = providerManager
				.takeAllProvider();
		List<ProviderVO> providers = new ArrayList<>();
		for (String key : connections.keySet())
		{
			Map<String, ProviderConnector> connector = connections.get(key);
			for (String providerKey : connector.keySet())
			{
				ProviderVO provider = new ProviderVO();
				ProviderConnector thriftConnector = connector.get(providerKey);
				ProviderConnection thriftConnection = thriftConnector
						.getThriftConnection();
				provider.setStatus(
						thriftConnector.isDisabled() ? "Disabled" : "Enabled");
				provider.setAddress(thriftConnection.getHost() + ":"
						+ thriftConnection.getPort());
				provider.setGroup(thriftConnection.getGroup());
				provider.setProvideName(thriftConnection.getProvideName());
				provider.setVersion(thriftConnection.getVersion());
				// StatusResponse sr = thriftConnector.getStatusResponse();
				// if (sr != null)
				// {
				// provider.setCpuIdlePercent(sr.getCpuIdlePercent());
				// provider.setMemUsedPercent(sr.getMemUsedPercent());
				// provider.setServeTimes(sr.getServeTimes());
				// provider.setStartTime(sr.getStartTime());
				// provider.setHoldeCount(sr.getHoldeCount());
				// }
				providers.add(provider);
			}
		}

		return providers;
	}
}
