package com.zlt.aps.nc.engine.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 内衬卷曲信息service
 */
public interface NcEngineCurlRollService {

    /**
     * 获得内衬卷曲长度，key
     * @return
     */
	Map<String, BigDecimal> getNcCurlLengthMap();
}
