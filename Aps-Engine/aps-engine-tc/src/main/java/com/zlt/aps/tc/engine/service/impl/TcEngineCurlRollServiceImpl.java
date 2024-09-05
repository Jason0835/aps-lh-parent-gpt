package com.zlt.aps.tc.engine.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.engine.mapper.TcEngineCurlRollMapper;
import com.zlt.aps.tc.engine.service.TcEngineCurlRollService;

@Service
public class TcEngineCurlRollServiceImpl implements TcEngineCurlRollService {
	@Autowired
	private TcEngineCurlRollMapper tcEngineCurlRollMapper;

	/**
	 * 获得胎面卷曲长度，key：胎面
	 * 
	 * @return
	 */
	@Override
	public Map<String, BigDecimal> getTcCurlLengthMap() {
		List<TcCurlRoll> curlRollList = tcEngineCurlRollMapper.getTcCurlRollList();
		Map<String, BigDecimal> curlLengthMap = curlRollList.stream().filter(item -> item.getCurlLength() != null)
				.collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength, (m1, m2) -> m1));
		return curlLengthMap;
	}
}
