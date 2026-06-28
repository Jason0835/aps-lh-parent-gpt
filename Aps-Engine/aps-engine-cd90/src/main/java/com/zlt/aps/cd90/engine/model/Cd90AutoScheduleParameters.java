package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 直裁自动排程强类型参数快照。
 *
 * <p>排程启动时生成一次快照，算法运行期间不再读取实时参数，最终提交前再比较参数指纹。</p>
 */
@Data
@Builder
public class Cd90AutoScheduleParameters {

    /** 工厂编码。 */
    private String factoryCode;
    /** 需求计算方式：AVERAGE或SUM。 */
    private String demandCalcMode;
    /** 直裁排程输出窗口班数。 */
    private int scheduleWindow;
    /** 每班大卷切换提醒次数。 */
    private int maxRollChangePerShift;
    /** 非收尾规格最小起排量。 */
    private BigDecimal minStartQty;
    /** 机台优先顺序。 */
    private List<String> machinePriority;
    /** 连续四班同规格上机次数上限。 */
    private int maxTime4Shift;
    /** 停产前瞻天数。 */
    private int stopLookaheadDays;
    /** 实际复产后的备库上限。 */
    private BigDecimal restartStockThreshold;
    /** 工装总数。 */
    private int rollTotalCount;
    /** 各班计划量均分阈值，按加损耗前的净需求量判断是否触发均分。 */
    private BigDecimal equalShareThreshold;
    /** 标准卷曲长度缺失时的兜底米数，对应参数CRIMP_LENGTH。 */
    private BigDecimal rollCoilMeter;
    /** 规格切换耗时，单位为分钟。 */
    private int specChangeMinutes;
    /** 同大卷不同直裁规格切换耗时，单位分钟。 */
    private int sameRollDiffSpecChangeMinutes;
    /** 不同大卷同直裁规格切换耗时，单位分钟。 */
    private int diffRollSameSpecChangeMinutes;
    /** 不同大卷不同直裁规格切换耗时，单位分钟。 */
    private int diffRollDiffSpecChangeMinutes;
    /** 上机后按耗尽处理的特殊大卷代码列表。 */
    private List<String> specialRollUseUpCodes;
    /** 特殊大卷允许额外前瞻的成型班次数，当前仅由策略组件读取。 */
    private int specialRollLookaheadShifts;
    /** 特殊大卷允许的额外备库上限，当前仅由策略组件读取。 */
    private BigDecimal specialRollExtraStockLimit;
    /** 非收尾部分排最小车数，达到该车数才允许在库排不足时提交部分排。 */
    private int partialMinVehicleCount;
    /** 大卷静置成熟时长，单位小时。 */
    private int agingPeriodHours;
    /** 通用损耗率兜底（百分比），对应参数 SYS0701003；t_cd90_loss_setting 四层优先级均未命中时使用。 */
    private BigDecimal fallbackLossRatePercent;
    /** 自动排程任务超时分钟数。 */
    private int taskTimeoutMinutes;
    /** 自动排程定时表达式。 */
    private String autoScheduleCron;
    /** 按PARAM_CODE保存的原始参数快照。 */
    private Map<String, String> sourceValues;
    /** 参数快照指纹。 */
    private String fingerprint;
}
