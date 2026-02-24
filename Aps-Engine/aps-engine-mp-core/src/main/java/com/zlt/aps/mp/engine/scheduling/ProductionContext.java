package com.zlt.aps.mp.engine.scheduling;

import com.zlt.aps.mp.engine.domain.Context;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 排产上下文
 *
 * @author ZLT
 * @date 20250229
 */
@Data
public class ProductionContext extends Context {

    /**
     * 操作批次号
     */
    private String operationWorkNo;

    /**
     * 分厂停工日列表<日期>
     */
    private Set<Integer> factoryStopDays;
    /**
     * 分厂参数配置
     */
    private Map<String, Object> factoryParams;

    /**
     * 排产参数配置项
     */
    private ProductionParamConfiguration productionParam;

}