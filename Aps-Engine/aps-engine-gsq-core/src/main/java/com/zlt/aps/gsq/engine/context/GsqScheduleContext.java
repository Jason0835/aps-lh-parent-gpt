package com.zlt.aps.gsq.engine.context;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.vo.GsqMonthSurplusVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqTaskNode;
import com.zlt.aps.gsq.engine.vo.GsqTotalPlanQtyVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈排程上下文。
 *
 * <p>贯穿一次钢丝圈排程从 S1 到 S6 的可变数据总线。</p>
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
public class GsqScheduleContext {

    // ========== 排程入参（外部传入） ==========

    /** 排程日期，格式：yyyy-MM-dd */
    private String scheduleDate;

    /** 分厂编码 */
    private String factoryCode;

    /** 操作人 */
    private String operator;

    // ========== S1写入 → S2/S3/S4消费 ==========

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：GSQ+年月日+3位定长自增序号 */
    private String batchNo;

    /** 对应的胎圈批次号 */
    private String tqBatchNo;

    /** 工序参数 */
    private GsqScheduleParams params;

    /** 外协规格Map，key=钢丝圈代码，value="1" */
    private Map<String, String> assistSpecMap = new HashMap<>();

    /** 限制作业映射，key=钢丝圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyCanMachineMap = new HashMap<>();

    /** 不可作业映射，key=钢丝圈代码，value=机台ID列表(逗号分隔) */
    private Map<String, String> specifyNotMachineMap = new HashMap<>();

    /** 当日库存（6点MES库存），key=钢丝圈代码，value=库存量 */
    private Map<String, Double> stockMap = new HashMap<>();

    /** 预计库存，key=钢丝圈代码，value=预计库存量 */
    private Map<String, Double> planStockMap = new HashMap<>();

    /** 前日早班计划量，key=钢丝圈代码，value=前日2班计划量（昨日2班剩余库存来源） */
    private Map<String, Double> lastMidPlanMap = new HashMap<>();

    /** 损耗率映射，key=钢丝圈代码，value=损耗率 */
    private Map<String, Double> lossRateMap = new HashMap<>();

    /** 月度剩余，key=钢丝圈代码 */
    private Map<String, GsqMonthSurplusVo> monthSurplusMap = new HashMap<>();

    /** 全部机台列表 */
    private List<GsqMachineInfo> allMachineList = new ArrayList<>();

    /** 机台寸口映射，key=机台编号，value=该机台可做的寸口值列表 */
    private Map<String, List<java.math.BigDecimal>> machineChuckMap = new HashMap<>();

    /** 机台钢丝直径映射（钢丝圈独有），key=机台编号，value=该机台支持的钢丝直径列表 */
    private Map<String, List<String>> machineWireDiameterMap = new HashMap<>();

    /** 机台产线规则映射（钢丝圈独有），key=机台编号，value=产线编号(1/2/3/4) */
    private Map<String, Integer> machineProductionLineMap = new HashMap<>();

    /** 工装车总数（钢丝圈独有） */
    private Integer cartTotalCount;

    /** 工装车整车容量，key=钢丝圈编码, value=整车容量（默认120） */
    private Map<String, Integer> cartCapacityMap = new HashMap<>();

    /** 可用工装车数（库存扣除后剩余） */
    private Integer availableCartCount;

    /** 检修计划机台，key=日期班次(如"2025-01-01|03"), value=该班次检修中的机台编号列表。班次编码使用两位格式：01=夜班,02=早班,03=中班 */
    private Map<String, List<String>> maintenanceMachineMap = new HashMap<>();

    /** 钢丝圈-胎圈关联关系，key=钢丝圈编码, value=关联胎圈编码列表（一个钢丝圈可能对应多个胎圈） */
    private Map<String, List<String>> steelRingTireRingMap = new HashMap<>();

    /** 钢丝圈-胎胚关联关系，key=钢丝圈编码, value=关联胎胚编码列表 */
    private Map<String, List<String>> steelRingEmbryoMap = new HashMap<>();

    /** BOM分解结果，key=钢丝圈编码, value=对应胎圈的BOM用量(默认1) */
    private Map<String, Double> bomDecomposeMap = new HashMap<>();

    /** 钢丝直径映射（钢丝圈独有），key=钢丝圈编码, value=钢丝直径 */
    private Map<String, String> wireDiameterMap = new HashMap<>();

    /** 胎圈6班次排程结果，key=胎圈代码, value=Map<班次号(1~6), 计划量> */
    private Map<String, Map<Integer, Double>> tq6ShiftResultMap = new HashMap<>();

    /** 胎圈停产班次，key=日期班次(如"2025-01-01|03"), value=true表示胎圈停产 */
    private Map<String, Boolean> tqStopShiftMap = new HashMap<>();

    /** 钢丝圈停产班次，key=日期班次(如"2025-01-01|03"), value=true表示钢丝圈停产 */
    private Map<String, Boolean> gsqStopShiftMap = new HashMap<>();

    /** 各规格各班次机台定额总产能，key=钢丝圈编码, value=Map<班次号(1~6), 定额总产能> */
    private Map<String, Map<Integer, Double>> specClassQuotaMap = new HashMap<>();

    /** 任务链，key=机台编号, value=该机台的任务链（按班次顺序排列） */
    private Map<String, LinkedList<GsqTaskNode>> taskChainMap = new HashMap<>();

    /** 排程基础信息Map，key=钢丝圈代码，value=施工表关联信息 */
    private Map<String, GsqScheduleBaseInfoVo> scheduleBaseInfoMap = new HashMap<>();

    /** 施工信息列表（S1校验用） */
    private List<EngineConstructionInfo> constructionInfoList = new ArrayList<>();

    // ========== S1+S2写入 → S3/S4消费 ==========

    /**
     * 排程基础数据列表。
     * S1写入基础数据，S2修改计划量字段，S3修改machineCode字段，S4读取并持久化。
     */
    private List<GsqScheduleResultVo> scheduleList = new ArrayList<>();

    // ========== S2写入 → S3/S4消费 ==========

    /** 总计划量统计（6班次） */
    private GsqTotalPlanQtyVo totalPlanQtyVo = new GsqTotalPlanQtyVo();

    /** 末班估值缓存：胎圈7班消耗量，key=钢丝圈代码，value=估值 */
    private Map<String, Double> lastShiftEstimateMap = new HashMap<>();

    // ========== S4写入 ==========

    /** 已有排程记录（当天已存在的排产记录） */
    private List<GsqScheduleResultVo> existScheduleList = new ArrayList<>();

    /** 插入记录数 */
    private int insertedCount;

    // ========== S3阶段临时传递字段 ==========

    /**
     * 当前正在排产的班次编码（CLASS_NUM_THREE字典值："01"=夜班, "02"=早班, "03"=中班）。
     * 由GsqMachineAssignHandler在策略链过滤前设置，
     * 供MaintenanceFilter等策略按班次精确过滤维修机台使用。
     */
    private String currentClassCode;

    /** 当前正在排产的班次索引(1~6) */
    private int currentClassIndex;

    // ========== 流程控制 ==========

    /** 是否中断排程 */
    private boolean interrupted = false;

    /** 中断原因 */
    private String interruptReason;

    /** 当前执行步骤 */
    private String currentStep;

    /** 校验错误信息集合 */
    private List<String> validationErrors = new ArrayList<>();

    /** 排程开始时间 */
    private Date startTime;

    /** 排程结束时间 */
    private Date endTime;

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
