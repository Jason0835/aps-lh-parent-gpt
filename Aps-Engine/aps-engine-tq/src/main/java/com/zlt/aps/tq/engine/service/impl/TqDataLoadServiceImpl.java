package com.zlt.aps.tq.engine.service.impl;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.mapper.TqEngineStockMapper;
import com.zlt.aps.tq.engine.service.ITqDataLoadService;
import com.zlt.aps.tq.engine.service.TqEngineLossService;
import com.zlt.aps.tq.engine.service.TqEngineMachineService;
import com.zlt.aps.tq.engine.service.TqEngineMonthSurplusService;
import com.zlt.aps.tq.engine.vo.BeadMachineCountVo;
import com.zlt.aps.tq.engine.vo.TqScheduleParams;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import com.zlt.aps.tq.engine.vo.TqStockConsumeVo;
import com.zlt.aps.tq.engine.vo.TqStockVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 胎圈排程数据加载服务实现。
 *
 * <p>在 aps-engine-tq 层实现 ITqDataLoadService 接口（接口定义在 core 层），
 * 负责将所有排程所需的基础数据从各个 Service 查询后写入 Context 的对应字段。</p>
 *
 * <p>加载的数据包括：</p>
 * <ul>
 *   <li>批次号（自增生成）</li>
 *   <li>工序参数（13项）</li>
 *   <li>排程基础数据（从成型排程统计）</li>
 *   <li>外协规格</li>
 *   <li>机台列表、定点机台、口型板机台</li>
 *   <li>库存、预计库存、昨日中班计划</li>
 *   <li>损耗率</li>
 *   <li>月度剩余</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
public class TqDataLoadServiceImpl implements ITqDataLoadService {

    @Resource
    private TqEngineMapper tqEngineMapper;
    @Resource
    private TqEngineStockMapper tqEngineStockMapper;
    @Resource
    private TqEngineMachineService tqEngineMachineService;
    @Resource
    private TqEngineLossService tqEngineLossService;
    @Resource
    private TqEngineMonthSurplusService tqEngineMonthSurplusService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    private static final BigDecimal HOUR24 = new BigDecimal("24");
    private static final String DEFAULT_TOOL_CAPACITY = "110";
    private static final String DEFAULT_PRODUCT_STOCK_HOUR = "12";
    private static final String DEFAULT_LARGE_DEMAND = "1500";
    private static final String DEFAULT_BIG_SIZE_SPEC = "35";
    private static final String DEFAULT_MIN_PLAN_QTY = "12750";
    private static final String DEFAULT_EQUAL_SHARE_THRESHOLD = "300";
    private static final String DEFAULT_CLASS_STOCK_REFERENCE = "22500";
    private static final String DEFAULT_ONE_ROLL_NUM = "220";

    @Override
    public void loadAllData(TqScheduleContext context) {
        String scheduleDate = context.getScheduleDate();
        String factoryCode = context.getFactoryCode();
        log.info("[数据加载] 开始加载排程基础数据, 排程日期:{}, 分厂编码:{}", scheduleDate, factoryCode);

        // 1. 生成批次号
        String batchNo = createBatchNo(scheduleDate);
        context.setBatchNo(batchNo);

        // 2. 加载工序参数
        TqScheduleParams params = loadParams();
        context.setParams(params);

        // 3. 加载排程基础数据（从成型排程统计）
        String productionStage = params.getProductionStage();
        List<TqScheduleResultVo> scheduleList = tqEngineMapper.statTqScheduleBase(scheduleDate, productionStage);
        if (scheduleList == null || scheduleList.isEmpty()) {
            autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败",
                    "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料");
            throw new RuntimeException("成型排程数据为空，或没有在施工信息中找到对应的物料");
        }
        // 过滤掉成型3~8班计划量都为0的数据（胎圈6班供应成型3~8班，1~2班由库存直接供应）
        scheduleList = scheduleList.stream()
                .filter(s -> (s.getCxClass3Plan() + s.getCxClass4Plan() + s.getCxClass5Plan()
                        + s.getCxClass6Plan() + s.getCxClass7Plan() + s.getCxClass8Plan()) > 0)
                .collect(Collectors.toList());
        context.setScheduleList(scheduleList);
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "根据'成型排程记录'统计出胎圈胶排程记录基础数据",
                toJSONString(scheduleList));

        // 4. 加载外协规格（外协逻辑已废弃，6班次排程不再使用外协规格）
        // Map<String, String> assistSpecMap = loadAssistSpecMap();
        // context.setAssistSpecMap(assistSpecMap);

        // 5. 加载机台相关数据（按工厂过滤机台）
        context.setAllMachineList(tqEngineMachineService.listTqMachine(factoryCode));
        context.setMouthPlateMachineMap(tqEngineMachineService.getMouthPlateMachineMap());
        context.setSpecifyCanMachineMap(tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN));
        context.setSpecifyNotMachineMap(tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT));
        context.setMachineChuckMap(tqEngineMachineService.getMachineChuckMap());

        // 6. 加载库存数据（按工厂过滤库存）
        // 注：旧4班次算法的 planStockMap（16点预计库存）已废弃，新6班次算法改用
        // stockMap（7点实际库存）+ todayMorningPlanMap（昨日排程CLASS3=今日早班）逐班滚动计算
        context.setStockMap(loadTqStock(scheduleDate, factoryCode));
        context.setTodayMorningPlanMap(loadTodayMorningPlan(scheduleDate, factoryCode));

        // 7. 加载损耗率
        context.setLossRateMap(tqEngineLossService.getLossRateMap());

        // 8. 加载月度剩余
        context.setMonthSurplusMap(tqEngineMonthSurplusService.getMonthSurplus(scheduleDate));

        // 9. 加载工装数据（整车容量和工装总数）
        loadToolingData(context);

        // 10. 加载检修计划数据（按工厂过滤检修计划）
        loadMaintenanceData(context, scheduleDate, factoryCode);

        // 11. 加载工作日历（停产班次）
        loadWorkCalendar(context, scheduleDate);

        // 12. 加载胎圈备库班数配置（按工厂过滤）+ 胎圈规格→成型机台数映射
        loadStockShiftConfigData(context, scheduleDate, factoryCode);

        // 13. 记录基础数据日志
        baseDataLog(batchNo, context);

        log.info("[数据加载] 排程基础数据加载完成, 批次号:{}, 排程记录数:{}", batchNo, scheduleList.size());
    }

    /**
     * 生成排程批次号
     *
     * @param scheduleDate 排程日期
     * @return 排程批次号，格式：TQ + 日期(yyyyMMdd) + 3位自增序号
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.TQ_BATCH_NO_PREFIX + scheduleDate);
    }

    /**
     * 加载工序参数（13项）
     */
    private TqScheduleParams loadParams() {
        List<com.zlt.aps.tq.engine.vo.TqParamsVo> list = tqEngineMapper.listTqParams();
        Map<String, String> paramsMap = list.stream()
                .collect(Collectors.toMap(
                        com.zlt.aps.tq.engine.vo.TqParamsVo::getParamCode,
                        com.zlt.aps.tq.engine.vo.TqParamsVo::getParamValue));

        TqScheduleParams params = new TqScheduleParams();
        // 胎圈排程专用参数（SYS1101XXX 系列）
        params.setBackupShiftCount(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_BACKUP_SHIFT_COUNT, "1")));
        params.setDemandCoefficient(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_DEMAND_COEFFICIENT, "2")));
        params.setClassHours(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_CLASS_HOURS, "8")));
        params.setToolCapacity(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_TOOL_CAPACITY, DEFAULT_TOOL_CAPACITY)));
        params.setLossRate(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_LOSS_RATE, "0.02")));
        params.setMergeThreshold(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_MERGE_THRESHOLD, "100")));
        // 预生产库存天数（SYS1101007直接配置天数，无需再除以24）
        params.setProductStockDay(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_PRODUCT_STOCK_DAY, "1")));
        params.setLargeDemand(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_LARGE_DEMAND, DEFAULT_LARGE_DEMAND)));
        params.setCloseOutNum(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_CLOSE_OUT_NUM, "50")));
        params.setMinPlanQty(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_MIN_PLAN_QTY, "10")));
        params.setMaxClassOutput(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_MAX_CLASS_OUTPUT, "3000")));
        params.setDemandCalcMode(getInt(paramsMap.getOrDefault(EngineConstants.TQ_DEMAND_CALC_MODE, "2")));
        params.setSupplyTimeThreshold(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_SUPPLY_TIME_THRESHOLD, "24")));
        params.setSpecSwitchTime(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_SPEC_SWITCH_TIME, "0.5")));
        params.setApexSwitchTime(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_APEX_SWITCH_TIME, "0.8")));
        params.setInchSwitchTime(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_INCH_SWITCH_TIME, "1.5")));
        params.setStockConsumeRatioHigh(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_STOCK_CONSUME_RATIO_HIGH, "2.0")));
        params.setStockConsumeRatioLow(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_STOCK_CONSUME_RATIO_LOW, "0.5")));
        params.setStopIntersectionDays(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_STOP_INTERSECTION_DAYS, "2")));
        params.setReopenStockThreshold(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_REOPEN_STOCK_THRESHOLD, "0")));
        params.setMoldingStopPreShiftCount(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_MOLDING_STOP_PRE_SHIFT_COUNT, "2")));
        // 工装车总数（全局统一值，从参数表加载，默认50）
        params.setToolingTotal(getInt(paramsMap.getOrDefault(EngineConstants.TQ_TOOLING_TOTAL, "50")));
        params.setProductionStage(paramsMap.getOrDefault(EngineConstants.TQ_PRODUCTION_STAGE_PRODUCE, "1"));
        params.setBigSizeSpec(com.zlt.aps.common.core.utils.BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.TQ_BIG_SIZE_SPEC, DEFAULT_BIG_SIZE_SPEC)));
        params.setStockLossRate(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_STOCK_LOSS_RATE, "0")));
        params.setEqualShareThreshold(new BigDecimal(paramsMap.getOrDefault(EngineConstants.TQ_EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD)));
        params.setClassStockReference(getDouble(paramsMap.getOrDefault(EngineConstants.TQ_CLASS_STOCK_REFERENCE, DEFAULT_CLASS_STOCK_REFERENCE)));
        params.setOneRollNum(new BigDecimal(paramsMap.getOrDefault(EngineConstants.TQ_ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM)));
        return params;
    }

    /**
     * 加载外协规格Map（外协逻辑已废弃，6班次排程不再使用外协规格，方法整体注释保留）
     */
    // private Map<String, String> loadAssistSpecMap() {
    //     Map<String, String> map = new HashMap<>();
    //     List<String> listAssistSpec = tqEngineMapper.listAssistSpec();
    //     if (listAssistSpec == null || listAssistSpec.isEmpty()) {
    //         return map;
    //     }
    //     for (String assistSpec : listAssistSpec) {
    //         map.put(assistSpec, "1");
    //     }
    //     return map;
    // }

    /**
     * 加载当天库存（按工厂过滤）
     */
    private Map<String, Double> loadTqStock(String scheduleDate, String factoryCode) {
        return tqEngineStockMapper.listTqStock(scheduleDate, factoryCode).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeadCode()))
                .collect(Collectors.toMap(TqStockVo::getBeadCode, TqStockVo::getStockNum));
    }

    /**
     * 加载当天早班(D日早班)计划量（按工厂过滤）
     * 昨天排程的CLASS3_PLAN_QTY对应今天早班
     */
    private Map<String, Double> loadTodayMorningPlan(String scheduleDate, String factoryCode) {
        return tqEngineStockMapper.listTodayMorningPlan(scheduleDate, factoryCode).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeadCode()))
                .collect(Collectors.toMap(TqStockConsumeVo::getBeadCode, TqStockConsumeVo::getConsume));
    }

    /**
     * 加载工装车容量数据（整车容量，按胎圈编码区分）
     * <p>工装车总数已改为全局参数配置（SYS1101023），在 loadParams() 中加载到 params.toolingTotal</p>
     * <p>T_TQ_TOOLING 表已废弃，不再读取</p>
     */
    private void loadToolingData(TqScheduleContext context) {
        // 加载整车容量：key=胎圈编码, value=整车容量
        List<Map<String, Object>> cartCapacityList = tqEngineMapper.listToolingCartCapacity();
        Map<String, Integer> cartCapacityMap = new HashMap<>();
        for (Map<String, Object> row : cartCapacityList) {
            String beadCode = String.valueOf(row.get("beadCode"));
            Integer capacity = row.get("cartCapacity") != null ? Integer.parseInt(String.valueOf(row.get("cartCapacity"))) : 0;
            cartCapacityMap.put(beadCode, capacity);
        }
        context.setCartCapacityMap(cartCapacityMap);

        log.info("[数据加载] 工装车容量数据加载完成, 规格数:{}", cartCapacityMap.size());
    }

    /**
     * 加载检修计划数据（按工厂过滤）
     * key=日期班次(如"2025-01-01|3"), value=该班次检修中的机台编号列表
     */
    private void loadMaintenanceData(TqScheduleContext context, String scheduleDate, String factoryCode) {
        List<Map<String, Object>> maintenanceList = tqEngineMapper.listMaintenancePlan(scheduleDate, factoryCode);
        Map<String, List<String>> maintenanceMachineMap = new HashMap<>();
        for (Map<String, Object> row : maintenanceList) {
            String machineCode = String.valueOf(row.get("machineCode"));
            String downtimeDate = String.valueOf(row.get("downtimeDate"));
            // 截取日期部分（格式可能为 yyyy-MM-dd HH:mm:ss）
            if (downtimeDate.length() > 10) {
                downtimeDate = downtimeDate.substring(0, 10);
            }
            String downtimeShift = String.valueOf(row.get("downtimeShift"));
            String key = downtimeDate + "|" + downtimeShift;
            maintenanceMachineMap.computeIfAbsent(key, k -> new ArrayList<>()).add(machineCode);
        }
        context.setMaintenanceMachineMap(maintenanceMachineMap);
        log.info("[数据加载] 检修计划数据加载完成, 检修班次数:{}", maintenanceMachineMap.size());
    }

    /**
     * 加载工作日历（停产班次）
     * 区分成型停产和胎圈停产，分别存入cxStopShiftMap和tqStopShiftMap
     * key=日期|班次名称(如"2025-01-01|夜班"), value=true表示停产
     * 班次映射：ONE_SHIFT_FLAG=夜班, TWO_SHIFT_FLAG=早班, THREE_SHIFT_FLAG=中班
     */
    private void loadWorkCalendar(TqScheduleContext context, String scheduleDate) {
        List<Map<String, Object>> calendarList = tqEngineMapper.listWorkCalendar(scheduleDate);
        Map<String, Boolean> cxStopShiftMap = new HashMap<>();
        Map<String, Boolean> tqStopShiftMap = new HashMap<>();
        for (Map<String, Object> row : calendarList) {
            String productionDate = String.valueOf(row.get("productionDate"));
            if (productionDate.length() > 10) {
                productionDate = productionDate.substring(0, 10);
            }
            // 一班=夜班, 二班=早班, 三班=中班
            String oneShiftFlag = String.valueOf(row.get("oneShiftFlag"));
            String twoShiftFlag = String.valueOf(row.get("twoShiftFlag"));
            String threeShiftFlag = String.valueOf(row.get("threeShiftFlag"));

            // 成型停产标记（CX_ONE_SHIFT_FLAG / CX_TWO_SHIFT_FLAG / CX_THREE_SHIFT_FLAG）
            String cxOneShiftFlag = row.get("cxOneShiftFlag") != null ? String.valueOf(row.get("cxOneShiftFlag")) : oneShiftFlag;
            String cxTwoShiftFlag = row.get("cxTwoShiftFlag") != null ? String.valueOf(row.get("cxTwoShiftFlag")) : twoShiftFlag;
            String cxThreeShiftFlag = row.get("cxThreeShiftFlag") != null ? String.valueOf(row.get("cxThreeShiftFlag")) : threeShiftFlag;

            // 胎圈停产标记（TQ_ONE_SHIFT_FLAG / TQ_TWO_SHIFT_FLAG / TQ_THREE_SHIFT_FLAG）
            String tqOneShiftFlag = row.get("tqOneShiftFlag") != null ? String.valueOf(row.get("tqOneShiftFlag")) : oneShiftFlag;
            String tqTwoShiftFlag = row.get("tqTwoShiftFlag") != null ? String.valueOf(row.get("tqTwoShiftFlag")) : twoShiftFlag;
            String tqThreeShiftFlag = row.get("tqThreeShiftFlag") != null ? String.valueOf(row.get("tqThreeShiftFlag")) : threeShiftFlag;

            // 成型停产
            if ("0".equals(cxOneShiftFlag)) {
                cxStopShiftMap.put(productionDate + "|夜班", true);
            }
            if ("0".equals(cxTwoShiftFlag)) {
                cxStopShiftMap.put(productionDate + "|早班", true);
            }
            if ("0".equals(cxThreeShiftFlag)) {
                cxStopShiftMap.put(productionDate + "|中班", true);
            }

            // 胎圈停产
            if ("0".equals(tqOneShiftFlag)) {
                tqStopShiftMap.put(productionDate + "|夜班", true);
            }
            if ("0".equals(tqTwoShiftFlag)) {
                tqStopShiftMap.put(productionDate + "|早班", true);
            }
            if ("0".equals(tqThreeShiftFlag)) {
                tqStopShiftMap.put(productionDate + "|中班", true);
            }
        }
        context.setCxStopShiftMap(cxStopShiftMap);
        context.setTqStopShiftMap(tqStopShiftMap);
        log.info("[数据加载] 工作日历加载完成, 成型停产班次数:{}, 胎圈停产班次数:{}", cxStopShiftMap.size(), tqStopShiftMap.size());
    }

    /**
     * 加载胎圈备库班数配置（按工厂过滤）+ 胎圈规格→成型机台数映射
     *
     * <p>加载两份数据供 S2 阶段 TqDemandCalcHandler 使用：</p>
     * <ul>
     *   <li>备库配置列表：按工厂过滤T_TQ_STOCK_SHIFT_CONFIG</li>
     *   <li>胎圈→机台数映射：通过成型排程结果表统计每个胎圈规格对应的DISTINCT成型机台数</li>
     * </ul>
     *
     * @param context 排程上下文
     * @param scheduleDate 排程日期
     * @param factoryCode 分厂编码
     */
    private void loadStockShiftConfigData(TqScheduleContext context, String scheduleDate, String factoryCode) {
        // 加载备库班数配置（按工厂过滤）
        List<TqStockShiftConfig> stockShiftConfigList = tqEngineMapper.listStockShiftConfig(factoryCode);
        context.setStockShiftConfigList(stockShiftConfigList);

        // 统计胎圈规格对应的成型机台数
        List<BeadMachineCountVo> beadMachineCountList = tqEngineMapper.statBeadMachineCount(scheduleDate);
        Map<String, Integer> beadMachineCountMap = beadMachineCountList.stream()
                .collect(Collectors.toMap(
                        BeadMachineCountVo::getBeadCode,
                        BeadMachineCountVo::getMachineCount,
                        (existing, replacement) -> existing));
        context.setBeadMachineCountMap(beadMachineCountMap);

        log.info("[数据加载] 备库班数配置加载完成, 工厂:{}, 配置规则数:{}, 胎圈→机台数映射规格数:{}",
                factoryCode, stockShiftConfigList.size(), beadMachineCountMap.size());
    }

    /**
     * 记录基础数据日志
     */
    private void baseDataLog(String batchNo, TqScheduleContext context) {
        String division = "\r\n---------------------------------------------------\r\n";
        StringBuilder logDetail = new StringBuilder();
        logDetail.append("口型板和机台关系集合：").append(toJSONString(context.getMouthPlateMachineMap())).append(division);
        logDetail.append("定点机台和机台的限制作业集合：").append(toJSONString(context.getSpecifyCanMachineMap())).append(division);
        logDetail.append("定点集合和机台的不可作业集合：").append(toJSONString(context.getSpecifyNotMachineMap())).append(division);
        logDetail.append("耗损率集合：").append(toJSONString(context.getLossRateMap())).append(division);
        logDetail.append("月度计划剩余量、完成量集合：").append(toJSONString(context.getMonthSurplusMap())).append(division);
        logDetail.append("参数设置集合：").append(toJSONString(context.getParams())).append(division);
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

    private double getDouble(String value) {
        if (StringUtils.isBlank(value)) {
            return 0D;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private int getInt(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
