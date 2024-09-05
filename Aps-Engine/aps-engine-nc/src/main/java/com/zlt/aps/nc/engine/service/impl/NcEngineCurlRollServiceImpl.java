package com.zlt.aps.nc.engine.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import com.zlt.aps.nc.engine.mapper.NcEngineCurlRollMapper;
import com.zlt.aps.nc.engine.service.NcEngineCurlRollService;

@Service
public class NcEngineCurlRollServiceImpl implements NcEngineCurlRollService {
	@Autowired
	private NcEngineCurlRollMapper ncEngineCurlRollMapper;

	/**
	 * 获得内衬卷曲长度，key：胎面
	 * 
	 * @return
	 */
	@Override
	public Map<String, BigDecimal> getNcCurlLengthMap() {
		List<NcCurlRoll> curlRollList = ncEngineCurlRollMapper.getNcCurlRollList();
		Map<String, BigDecimal> curlLengthMap = curlRollList.stream().filter(item -> item.getCurlLength() != null)
				.collect(Collectors.toMap(NcCurlRoll::getLiningCode, NcCurlRoll::getCurlLength, (m1, m2) -> m1));
		return curlLengthMap;
	}
}
