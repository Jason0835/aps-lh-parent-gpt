package com.zlt.aps.tm.engine.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 胎面卷曲信息service
 */
public interface TmEngineCurlRollService {

    /**
     * 获得胎面卷曲长度，key
     * @return
     */
	Map<String, BigDecimal> getTmCurlLengthMap();
}
