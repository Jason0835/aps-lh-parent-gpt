package com.zlt.aps.tc.autoplan;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧自动排程 JSON 场景对象。
 *
 * <p>仅承载本地测试输入和期望值，不访问数据库、Redis、MES 或其他外部服务。</p>
 */
@Data
public class TcAutoPlanJsonScenario {

    /** 场景名称。 */
    private String caseName;

    /** 场景类型。 */
    private String type;

    /** 单一算法输入。 */
    private Map<String, Object> input = new LinkedHashMap<>();

    /** 单一任务输入。 */
    private Map<String, Object> task = new LinkedHashMap<>();

    /** 多任务输入。 */
    private List<Map<String, Object>> tasks = new ArrayList<>();

    /** 单一候选机台输入。 */
    private Map<String, Object> candidate = new LinkedHashMap<>();

    /** 多候选机台输入。 */
    private List<Map<String, Object>> candidates = new ArrayList<>();

    /** 本次场景参数快照。 */
    private Map<String, String> params = new LinkedHashMap<>();

    /** 班次编码映射，key 为班次顺序字符串。 */
    private Map<String, String> shiftCodes = new LinkedHashMap<>();

    /** 期望结果。 */
    private Map<String, Object> expected = new LinkedHashMap<>();
}

