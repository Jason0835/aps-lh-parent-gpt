package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 按帘布、大卷和机台归并的排程主结果草稿。 */
@Data
@Builder
public class Cd90ScheduleResultDraft {

    /** 帘布、大卷和机台组成的草稿归并键，仅用于排程结果构建阶段。 */
    private String resultKey;
    /** 帘布代码。 */
    private String clothCode;
    /** 帘布大卷编号。 */
    private String bigRollCode;
    /** 帘线规格。 */
    private String cordSpec;
    /** 直裁机台编码。 */
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
    private List<Cd90ScheduleShiftSlotDraft> shiftSlots;
}
