package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 单个帘布规格的候选机台试算请求。
 */
@Data
@Builder
public class Cd90MachineTrialRequest {

    /** 帘布代码 */
    private String clothCode;
    /** 大卷代码 */
    private String bigRollCode;
    /** 帘线规格 */
    private String cordSpec;
    /** 当前层位直裁宽度，来自施工表TIRE_FABRIC_CRAFT1/2/3 */
    private BigDecimal craftWidth;
    /** 本规格采用的标准卷曲长度，单位米；标准表缺失时由CRIMP_LENGTH兜底 */
    private BigDecimal curlLength;
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
    /** 已占用车数（前序班次已安排入库的部分） */
    private int occupiedVehicleCount;
    /** 班次可用小时数 */
    private int shiftHours;
    /** 各机台班次剩余秒数（扣除前序任务后），key=机台编码 */
    private Map<String, Integer> remainingSecondsByMachine;
    /** 各机台上次生产规格，key=机台编码 */
    private Map<String, String> previousSpecByMachine;
    /** 各机台上次尾匹状态，key=机台编码 */
    private Map<String, Cd90MachineTailState> previousTailByMachine;
    /** 当前规格的历史/续作原机台，原机台有可排量时不得切换到其他机台。 */
    private String preferredHistoryMachineCode;
    /** 大卷库存时效信息列表 */
    private List<Cd90BigRollAgingStock> bigRollAgingStocks;
    /** 自动排程参数配置 */
    private Cd90AutoScheduleParameters parameters;
}
