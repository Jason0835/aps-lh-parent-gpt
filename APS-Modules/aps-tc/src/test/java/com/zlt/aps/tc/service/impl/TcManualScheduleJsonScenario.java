package com.zlt.aps.tc.service.impl;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎侧人工调整 JSON 测试场景。
 *
 * <p>场景只承载本地输入与期望值，不访问数据库、Redis、MES 或其他外部服务。</p>
 */
@Data
public class TcManualScheduleJsonScenario {

    /** 场景编码。 */
    private String caseName;

    /** 人工操作类型。 */
    private String operation;

    /** 场景输入。 */
    private Map<String, Object> input = new LinkedHashMap<>();

    /** 期望结果。 */
    private Map<String, Object> expected = new LinkedHashMap<>();
}
