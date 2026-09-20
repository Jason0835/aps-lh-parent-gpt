package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成型来源收尾计算快照。
 *
 * <p>该模型只存在于一次排程运行内，用于保证来源任务合并、未来需求前移后仍能
 * 使用原始八班计划和原始成型余量重新判定收尾位置。</p>
 */
@Data
public class ScheduleFormingTailSourceModel {

    /**
     * 来源唯一键。
     */
    private String sourceKey;
    /**
     * 来源成型计划所属排程日期。
     */
    private LocalDate sourceScheduleDate;
    /**
     * 解析后的胎面或胎侧代码。
     */
    private String processCode;
    /**
     * 胎胚代码。
     */
    private String embryoCode;
    /**
     * 成型来源工单号。
     */
    private String sourceOrderNo;
    /**
     * 原始 CLASS1~CLASS8 成型计划条数。
     */
    private Map<Integer, BigDecimal> originalClassQtyMap = new LinkedHashMap<>();
    /**
     * 原始成型余量，空值必须保留。
     */
    private BigDecimal formingTailRemainQty;
    /**
     * 计算出的实际成型收尾班次。
     */
    private ScheduleFormingShiftKey tailShiftKey;
    /**
     * 无法计算时的原因。
     */
    private String unresolvedReason;

    /**
     * 设置原始八班计划快照并复制容器。
     *
     * @param originalClassQtyMap 原始八班计划
     */
    public void setOriginalClassQtyMap(Map<Integer, BigDecimal> originalClassQtyMap) {
        this.originalClassQtyMap = originalClassQtyMap == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(originalClassQtyMap);
    }
}
