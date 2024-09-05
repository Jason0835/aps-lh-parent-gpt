package com.zlt.aps.tm.engine.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.engine.mapper.TmEngineCurlRollMapper;
import com.zlt.aps.tm.engine.service.TmEngineCurlRollService;

@Service
public class TmEngineCurlRollServiceImpl implements TmEngineCurlRollService {
	@Autowired
	private TmEngineCurlRollMapper tmEngineCurlRollMapper;

	/**
	 * 获得胎面卷曲长度，key：胎面
	 * 
	 * @return
	 */
	@Override
	public Map<String, BigDecimal> getTmCurlLengthMap() {
		List<TmCurlRoll> curlRollList = tmEngineCurlRollMapper.getTmCurlRollList();
		Map<String, BigDecimal> curlLengthMap = curlRollList.stream().filter(item -> item.getCurlLength() != null)
				.collect(Collectors.toMap(TmCurlRoll::getTreadCode, TmCurlRoll::getCurlLength, (m1, m2) -> m1));
		return curlLengthMap;
	}
}
