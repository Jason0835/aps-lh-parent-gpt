package com.zlt.aps.tm.autoplan;

import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleRequestVo;
import com.zlt.aps.tm.domain.vo.TmExperimentSpecMonthPlanRowVo;
import com.zlt.aps.tm.domain.vo.TmFormingDemandRowVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 胎面自动排程 JSON 测试场景根对象。
 *
 * <p>该对象承接本地 JSON 文件中的请求、基础资料、成型需求和期望结果，
 * 仅用于测试数据装载，不连接真实数据库、Redis、MES 或外部接口。</p>
 */
@Data
public class TmAutoPlanScenario {

    /** 场景名称，用于测试日志和断言失败提示 */
    private String caseName;

    /** 场景说明 */
    private String description;

    /** 是否跳过完整入口测试，仅做步骤级观察 */
    private Boolean skipScenarioRun = Boolean.FALSE;

    /** 自动排程入口请求 */
    private TmAutoScheduleRequestVo autoPlanRequest;

    /** 排程参数 */
    private List<TmParams> params = new ArrayList<>();

    /** 指定日期已有旧结果 */
    private List<TmScheduleResult> oldScheduleResults = new ArrayList<>();

    /** 历史胎面排程结果，用于新规格回看判断 */
    private List<TmScheduleResult> historyScheduleResults = new ArrayList<>();

    /** 是否强制使用空历史排程结果，验证参数缺省的新规格场景时使用 */
    private Boolean forceEmptyHistoryScheduleResults = Boolean.FALSE;

    /** 成型计划与施工信息关联后的测试行 */
    private List<TmFormingDemandRowVo> cxScheduleResults = new ArrayList<>();

    /** 月计划定稿实验规格行 */
    private List<TmExperimentSpecMonthPlanRowVo> experimentSpecMonthPlans = new ArrayList<>();

    /** 原始施工信息扩展字段，当前测试保留用于可读性和后续扩展 */
    private List<Map<String, Object>> constructionInfos = new ArrayList<>();

    /** 胎面库存快照 */
    private List<TmStock> stocks = new ArrayList<>();

    /** 胎面机台基础资料 */
    private List<TmMachineInfo> machineInfos = new ArrayList<>();

    /** 机台检修或停机资料 */
    private List<TmMachineMaintenance> machineMaintenances = new ArrayList<>();

    /** 机台速度资料 */
    private List<TmMachineSpeed> machineSpeeds = new ArrayList<>();

    /** 口型板资料 */
    private List<TmMouthPlate> mouthPlates = new ArrayList<>();

    /** 定点和禁排机台规则 */
    private List<TmSpecifyMachine> specifyMachines = new ArrayList<>();

    /** 胶料机台关系资料 */
    private List<TmGlueMachineReal> glueMachineReals = new ArrayList<>();

    /** 胎面卷曲长度资料 */
    private List<TmCurlRoll> curlRolls = new ArrayList<>();

    /** 胎面损耗率设置 */
    private List<TmLossSetting> lossSettings = new ArrayList<>();

    /** 工作日历资料 */
    private List<WorkCalendarData> workCalendars = new ArrayList<>();

    /** 任务草稿补充字段，用于覆盖当前数据加载层尚未落地的数据 */
    private List<TaskOverride> taskOverrides = new ArrayList<>();

    /** 是否模拟结果表写入失败 */
    private Boolean mockResultInsertFailure = Boolean.FALSE;

    /** 是否模拟解释表写入失败 */
    private Boolean mockExplainInsertFailure = Boolean.FALSE;

    /** 期望结果 */
    private TmAutoPlanExpectedResult expected = new TmAutoPlanExpectedResult();

    /**
     * 工作日历测试数据。
     */
    @Data
    public static class WorkCalendarData {

        /** 工序编码，03 表示成型，04 表示胎面 */
        private String procCode;

        /** 工作日标识 */
        private String dayFlag;

        /** 一班开班标识 */
        private String oneShiftFlag;

        /** 二班开班标识 */
        private String twoShiftFlag;

        /** 三班开班标识 */
        private String threeShiftFlag;
    }

    /**
     * 任务草稿覆盖数据。
     */
    @Data
    public static class TaskOverride {

        /** 订单号，支持完整匹配 */
        private String orderNo;

        /** 胎面编码 */
        private String treadCode;

        /** 班次顺序 */
        private Integer shiftOrder;

        /** 基部胶编码 */
        private String baseGlueCode;

        /** 指定计划量 */
        private BigDecimal planQty;

        /** 当前班需求量 */
        private BigDecimal currentShiftDemandQty;

        /** 保证范围需求量 */
        private BigDecimal guardDemandQty;

        /** 滚动库存 */
        private BigDecimal rollingStockQty;

        /** 收尾标识，1 表示按收尾规格计算 */
        private String tailFlag;

        /** 收尾成型余量，单位条 */
        private BigDecimal tailBalanceQty;

        /** 胎面肩长，单位米 */
        private BigDecimal treadShoulderLength;

        /** 损耗率，百分比 */
        private BigDecimal lossRate;

        /** 供应小时数 */
        private BigDecimal supplyHours;

        /** 工装总数 */
        private BigDecimal totalToolQty;

        /** 机台剩余产能 */
        private BigDecimal machineRemainCapacity;

        /** 机台生产速度 */
        private BigDecimal machineSpeed;

        /** 检修时长 */
        private BigDecimal maintenanceHours;

        /** 前规格切换时长 */
        private BigDecimal previousSpecSwitchHours;

        /** 前胶料切换时长 */
        private BigDecimal previousGlueSwitchHours;

        /** 未排原因编码 */
        private String unplannedReasonCode;

        /** 未排原因描述 */
        private String unplannedReasonDesc;
    }
}
