package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 单个钢带规格的候选机台试算请求。
 */
@Data
@Builder
public class Cd15MachineTrialRequest {

    /** 施工材料稳定键 */
    private String materialKey;
    /** 钢带代码 */
    private String steelStripCode;
    /** 大卷代码 */
    private String bigRollCode;
    /** 裁断角度 */
    private String cuttingAngle;
    /** 钢带规格 */
    private String cordSpec;
    /** 是否按分裁模式试算机台。 */
    private boolean splitCut;
    /** 是否按单规格一出二模式试算。 */
    private boolean singleSpecSplit;
    /** 当前层位斜裁宽度，来自施工表BELT_CRAFT1/2/3。 */
    private BigDecimal craftWidth;
    /** 机台和角度约束使用的实际占用宽度；单规格一出二时为两倍斜裁宽度。 */
    private BigDecimal machineMatchWidth;
    /** 单片胎体长度，单位毫米/片，来自施工表BELT1_LENGTH/BELT2_LENGTH/BELT3_LENGTH。 */
    private BigDecimal unitConsumeMillimeter;
    /** 本规格采用的标准卷曲长度，单位米；标准表缺失时由CRIMP_LENGTH兜底 */
    private BigDecimal curlLength;
    /** 大卷幅宽，单位毫米；为空时大卷占用按计划量兜底。 */
    private BigDecimal cordWidth;


    /** 班次代码 */
    private String shiftCode;
    /** 班次开始时间 */
    private LocalDateTime shiftStart;
    /** 班次结束时间 */
    private LocalDateTime shiftEnd;
    /** 本班次净需求量（已扣除已排量） */
    private BigDecimal netDemandQuantity;
    /** 是否清尾：清尾时起排量门槛降低、允许跨机台合并 */
    private boolean closeOut;
    /** 是否为均分后转入下一班的剩余计划量；为true时不得再次均分或重复叠加损耗。 */
    private boolean equalShareAlreadyApplied;
    /** 已占用车数（前序班次已安排入库的部分） */
    private int occupiedVehicleCount;
    /** 班次可用小时数 */
    private int shiftHours;
    /** 各机台班次剩余秒数（扣除前序任务后），key=机台编码 */
    private Map<String, Integer> remainingSecondsByMachine;
    /** 各机台上次生产规格，key=机台编码 */
    private Map<String, String> previousSpecByMachine;
    /** 各机台上次尾匹状态，key=机台编码 */
    private Map<String, Cd15MachineTailState> previousTailByMachine;
    /** 当前规格的历史/续作原机台，原机台有可排量时不得切换到其他机台。 */
    private String preferredHistoryMachineCode;
    /** 大卷库存时效信息列表 */
    private List<Cd15BigRollAgingStock> bigRollAgingStocks;
    /** 自动排程参数配置 */
    private Cd15AutoScheduleParameters parameters;
}
