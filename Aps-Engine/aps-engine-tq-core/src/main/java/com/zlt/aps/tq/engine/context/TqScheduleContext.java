package com.zlt.aps.tq.engine.context;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqTaskNode;
import com.zlt.aps.tq.engine.vo.TqTotalPlanQtyVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈排程上下文。
 *
 * <p>贯穿一次胎圈排程从 S1 到 S6 的可变数据总线。</p>
 *
 * <p>字段按阶段标注数据流向：</p>
 * <ul>
 *   <li>S1写入 → S2/S3/S4/S5/S6消费：基础数据（库存、机台、损耗率等）</li>
 *   <li>S2写入 → S3/S4/S5/S6消费：中间计算结果（计划量、供应时长等）</li>
 *   <li>S3写入 → S4/S5/S6消费：机台分配结果（直接修改scheduleList中的machineCode）</li>
 *   <li>S4写入 → S5/S6消费：停产协调结果</li>
 *   <li>S5写入 → S6消费：均衡调整结果</li>
 *   <li>S6写入：持久化结果</li>
 * </ul>
 *
 * <p>注意：该对象会被多个Handler原地修改。新增字段时必须同时确认
 * 初始化入口和消费位置，避免字段只有写入没有消费。</p>
 *
 * @author APS
 */
@Data
public class TqScheduleContext {

    // ========== 排程入参（外部传入） ==========

    /** 排程日期，格式：yyyy-MM-dd */
    private String scheduleDate;

    /** 分厂编码 */
    private String factoryCode;

    /** 操作人 */
    private String operator;

    // ========== S1写入 → S2/S3/S4消费 ==========

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    private String batchNo;

    /** 对应的成型批次号 */
    private String cxBatchNo;

    /** 工序参数（13项） */
    private TqScheduleParams params;

    /**
     * 外协规格Map，key=胎圈代码，value="1"
     *
     * @deprecated 外协规格逻辑已废弃（2026-06-27），6班次排程不再区分外协/非外协。
     *             字段保留仅为兼容已有 getter/setter 调用，不再被写入和读取。
     */
    @Deprecated
    private Map<String, String> assistSpecMap = new HashMap<>();

    /** 口型板→机台映射，key=口型板代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> mouthPlateMachineMap = new HashMap<>();

    /** 限制作业映射，key=胎圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyCanMachineMap = new HashMap<>();

    /** 不可作业映射，key=胎圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyNotMachineMap = new HashMap<>();

    /** 当日库存，key=胎圈代码，value=库存量 */
    private Map<String, Double> stockMap = new HashMap<>();

    /** 预计库存，key=胎圈代码，value=预计库存量 */
    private Map<String, Double> planStockMap = new HashMap<>();

    /** 当天早班(D日早班)计划量，key=胎圈代码，value=计划量（昨天已排的、属于今天早班的胎圈计划量） */
    private Map<String, Double> todayMorningPlanMap = new HashMap<>();

    /** 损耗率映射，key=胎圈代码，value=损耗率 */
    private Map<String, Double> lossRateMap = new HashMap<>();

    /** 月度剩余，key=胎圈代码 */
    private Map<String, TqMonthSurplusVo> monthSurplusMap = new HashMap<>();

    /** 全部机台列表 */
    private List<TqMachineInfo> allMachineList = new ArrayList<>();

    /** 机台寸口映射，key=机台编号，value=该机台可做的寸口值列表（来自TqMachineChuck） */
    private Map<String, List<java.math.BigDecimal>> machineChuckMap = new HashMap<>();

    /** 工装车整车容量，key=胎圈编码, value=整车容量 */
    private Map<String, Integer> cartCapacityMap = new HashMap<>();

    /** 检修计划机台，key=日期班次(如"2025-01-01|03"), value=该班次检修中的机台编号列表。班次编码使用两位格式：01=夜班,02=早班,03=中班 */
    private Map<String, List<String>> maintenanceMachineMap = new HashMap<>();

    /** 胎圈-胎胚关联关系，key=胎圈编码, value=关联胎胚编码列表（一个胎圈可能对应多个胎胚） */
    private Map<String, List<String>> beadEmbryoMap = new HashMap<>();

    /** 胎圈备库班数配置列表（按工厂过滤），S2阶段匹配用 */
    private List<TqStockShiftConfig> stockShiftConfigList = new ArrayList<>();

    /** 胎圈规格→成型机台数映射，key=胎圈编码, value=正在生产该胎圈规格的成型机台数量 */
    private Map<String, Integer> beadMachineCountMap = new HashMap<>();

    /** 成型停产班次，key=日期班次(如"2025-01-01|中班"), value=true表示成型停产 */
    private Map<String, Boolean> cxStopShiftMap = new HashMap<>();

    /** 胎圈停产班次（区别于成型停产），key=日期班次(如"2025-01-01|中班"), value=true表示胎圈停产 */
    private Map<String, Boolean> tqStopShiftMap = new HashMap<>();

    /** 各规格各班次机台定额总产能，key=胎圈编码, value=Map<班次号(1~6), 定额总产能> */
    private Map<String, Map<Integer, Double>> specClassQuotaMap = new HashMap<>();

    /** 任务链，key=机台编号, value=该机台的任务链（按班次顺序排列） */
    private Map<String, java.util.LinkedList<TqTaskNode>> taskChainMap = new HashMap<>();

    // ========== S1+S2写入 → S3/S4消费 ==========

    /**
     * 排程基础数据列表。
     * S1写入基础数据，S2修改计划量字段，S3修改machineCode字段，S4读取并持久化。
     */
    private List<TqScheduleResultVo> scheduleList = new ArrayList<>();

    // ========== S2写入 → S3/S4消费 ==========

    /** 总计划量统计（中班/夜班/白班/次日中班） */
    private TqTotalPlanQtyVo totalPlanQtyVo = new TqTotalPlanQtyVo();

    // ========== S4写入 ==========

    /**
     * 外协排程数据
     *
     * @deprecated 外协规格逻辑已废弃（2026-06-27），6班次排程不再区分外协/非外协。
     *             字段保留仅为兼容已有 getter/setter 调用，不再被写入和读取。
     */
    @Deprecated
    private List<TqScheduleResultVo> assistScheduleList = new ArrayList<>();

    /**
     * 非外协排程数据
     *
     * @deprecated 外协规格逻辑已废弃（2026-06-27），6班次排程不再区分外协/非外协。
     *             字段保留仅为兼容已有 getter/setter 调用，不再被写入和读取。
     */
    @Deprecated
    private List<TqScheduleResultVo> normalScheduleList = new ArrayList<>();

    /** 已有排程记录（当天已存在的排产记录） */
    private List<TqScheduleResultVo> existScheduleList = new ArrayList<>();

    /** 插入记录数 */
    private int insertedCount;

    // ========== S3阶段临时传递字段 ==========

    /**
     * 当前正在排产的班次编码（CLASS_NUM_THREE字典值："01"=夜班, "02"=早班, "03"=中班）。
     * 由TqMachineAssignHandler.searchOptionalMachineList在策略链过滤前设置，
     * 供MaintenanceFilter等策略按班次精确过滤维修机台使用。
     */
    private String currentClassCode;

    // ========== 流程控制 ==========

    /** 是否中断排程 */
    private boolean interrupted = false;

    /** 中断原因 */
    private String interruptReason;

    /** 当前执行步骤 */
    private String currentStep;

    /** 校验错误信息集合 */
    private List<String> validationErrors = new ArrayList<>();

    /**
     * 中断排程流程
     *
     * @param reason 中断原因
     */
    public void interruptSchedule(String reason) {
        this.interrupted = true;
        this.interruptReason = reason;
    }

    /**
     * 追加一条校验错误信息（空串或null将被忽略）
     *
     * @param message 错误描述
     */
    public void addValidationError(String message) {
        if (StringUtils.isNotEmpty(message)) {
            this.validationErrors.add(message);
        }
    }
}
