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
    /** 成型需求窗口班数。 */
    private int demandWindow;
    /** 直裁排程输出窗口班数。 */
    private int scheduleWindow;
    /** 库存保证班数。 */
    private BigDecimal stockGuaranteeShifts;
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
    /** 单个工装卷曲米数。 */
    private BigDecimal rollCoilMeter;
    /** 规格切换耗时，单位为分钟。 */
    private int specChangeMinutes;
    /** 自动排程任务超时分钟数。 */
    private int taskTimeoutMinutes;
    /** 自动排程定时表达式。 */
    private String autoScheduleCron;
    /** 按PARAM_CODE保存的原始参数快照。 */
    private Map<String, String> sourceValues;
    /** 参数快照指纹。 */
    private String fingerprint;
}
