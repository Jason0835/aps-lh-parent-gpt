package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 按钢带、大卷和机台归并的排程主结果草稿。 */
@Data
@Builder
public class Cd15ScheduleResultDraft {

    /** 钢带、大卷和机台组成的草稿归并键，仅用于排程结果构建阶段。 */
    private String resultKey;
    /** 施工材料稳定键，仅用于结果归并。 */
    private String materialKey;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 钢带大卷编号。 */
    private String bigRollCode;
    /** 帘线规格。 */
    private String cordSpec;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 斜裁宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 标准卷曲长度，单位米。 */
    private BigDecimal curlLength;
    /** 大卷幅宽，单位毫米。 */
    private BigDecimal cordWidth;
    /** GDYY大卷实际占用量，单位米。 */
    private BigDecimal bigRollConsumeQuantity;
        /** 裁断模式：SINGLE或SPLIT。 */
    private String cutMode;
    /** 分裁组合稳定键，仅用于两条结果共用工单号。 */
    private String splitGroupKey;
    /** 斜裁机台编码。 */
    private String machineCode;
    /** 排程结果使用的库排号，多个库排按任务出现顺序去重并以逗号分隔。 */
    private String primaryLaneCode;
    /** 数据来源，自动排程结果固定为0。 */
    private String dataSource;
    /** 去重排序后的来源成型批次号，多个值使用逗号分隔。 */
    private String cxBatchNo;
    /** 去重排序后的来源成型机台编码，多个值使用逗号分隔。 */
    private String cxMachineCodes;
    /** 相关胎胚的月计划剩余量合计。 */
    private BigDecimal planSurplusQty;
    /** CLASS1至CLASS8的班次排程槽位。 */
    private List<Cd15ScheduleShiftSlotDraft> shiftSlots;
}
