package com.daqsoft.schedule.server.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daqsoft.schedule.server.service.ClusterService;
import com.daqsoft.schedule.server.web.response.BootstrapTableResponse;
import com.daqsoft.schedule.server.web.vo.ProviderVO;

@RestController
@RequestMapping("/cluster")
public class ClusterController
{
	@Autowired
	ClusterService clusterService;
	@RequestMapping("/getInfo")
	public BootstrapTableResponse<ProviderVO> getCluster()
	{
		BootstrapTableResponse<ProviderVO> bootstrapTableResponse = new BootstrapTableResponse<ProviderVO>();
		List<ProviderVO> providerVOs = clusterService.getAllCluster();
		bootstrapTableResponse.setRows(providerVOs);
		bootstrapTableResponse.setTotal(providerVOs.size());
		return bootstrapTableResponse;
	}
}
