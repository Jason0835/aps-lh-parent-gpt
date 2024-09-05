package com.zlt.aps.tc.engine.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 胎侧卷曲信息service
 */
public interface TcEngineCurlRollService {

    /**
     * 获得胎侧卷曲长度，key
     * @return
     */
	Map<String, BigDecimal> getTcCurlLengthMap();
}
