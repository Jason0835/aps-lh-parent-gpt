package com.zlt.aps.dj.engine.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.engine.mapper.DjEngineCurlRollMapper;
import com.zlt.aps.dj.engine.service.DjEngineCurlRollService;

@Service
public class DjEngineCurlRollServiceImpl implements DjEngineCurlRollService {
	@Autowired
	private DjEngineCurlRollMapper djEngineCurlRollMapper;

	/**
	 * 获得垫胶卷曲长度，key：胎面
	 * 
	 * @return
	 */
	@Override
	public Map<String, BigDecimal> getDjCurlLengthMap() {
		List<DjCurlRoll> curlRollList = djEngineCurlRollMapper.getDjCurlRollList();
		Map<String, BigDecimal> curlLengthMap = curlRollList.stream().filter(item -> item.getCurlLength() != null)
				.collect(Collectors.toMap(DjCurlRoll::getPaddingCode, DjCurlRoll::getCurlLength, (m1, m2) -> m1));
		return curlLengthMap;
	}
}
