package com.zlt.aps.dj.engine.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 垫胶卷曲信息service
 */
public interface DjEngineCurlRollService {

    /**
     * 获得垫胶卷曲长度，key
     * @return
     */
	Map<String, BigDecimal> getDjCurlLengthMap();
}
