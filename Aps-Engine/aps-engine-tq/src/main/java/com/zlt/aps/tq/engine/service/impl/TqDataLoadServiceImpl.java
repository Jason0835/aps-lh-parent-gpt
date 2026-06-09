package com.zlt.aps.tq.engine.service.impl;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.mapper.TqEngineStockMapper;
import com.zlt.aps.tq.engine.service.ITqDataLoadService;
import com.zlt.aps.tq.engine.service.TqEngineLossService;
import com.zlt.aps.tq.engine.service.TqEngineMachineService;
import com.zlt.aps.tq.engine.service.TqEngineMonthSurplusService;
import com.zlt.aps.tq.engine.service.TqEngineStockService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private TqEngineStockService tqEngineStockService;
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
        log.info("[数据加载] 开始加载排程基础数据, 排程日期:{}", scheduleDate);

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
        // 过滤掉成型5个班的计划量都为0的数据
        scheduleList = scheduleList.stream()
                .filter(s -> (s.getCxClass2Plan() + s.getCxClass3Plan() + s.getCxClass4Plan() + s.getCxClass5Plan()) > 0)
                .collect(Collectors.toList());
        context.setScheduleList(scheduleList);
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "根据'成型排程记录'统计出胎圈胶排程记录基础数据",
                toJSONString(scheduleList));

        // 4. 加载外协规格
        Map<String, String> assistSpecMap = loadAssistSpecMap();
        context.setAssistSpecMap(assistSpecMap);

        // 5. 加载机台相关数据
        context.setAllMachineList(tqEngineMachineService.listTqMachine());
        context.setMouthPlateMachineMap(tqEngineMachineService.getMouthPlateMachineMap());
        context.setSpecifyCanMachineMap(tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN));
        context.setSpecifyNotMachineMap(tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT));
        context.setMachineChuckMap(tqEngineMachineService.getMachineChuckMap());

        // 6. 加载库存数据
        context.setStockMap(loadTqStock(scheduleDate));
        context.setPlanStockMap(tqEngineStockService.getPlanStockMap(batchNo, scheduleDate, params.getStockLossRate()));
        context.setTodayMorningPlanMap(loadTodayMorningPlan(scheduleDate));

        // 7. 加载损耗率
        context.setLossRateMap(tqEngineLossService.getLossRateMap());

        // 8. 加载月度剩余
        context.setMonthSurplusMap(tqEngineMonthSurplusService.getMonthSurplus(scheduleDate));

        // 9. 记录基础数据日志
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
        params.setProductionStage(paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE));
        params.setLossRate(getDouble(paramsMap.get(EngineConstants.LOSS_RATE)));
        params.setMergeThreshold(getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD)));
        params.setCloseOutNum(getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));
        params.setToolCapacity(getDouble(paramsMap.getOrDefault(EngineConstants.TOOL_CAPACITY, DEFAULT_TOOL_CAPACITY)));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        params.setProductStockDay(productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue());
        params.setLargeDemand(getDouble(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)));
        params.setBigSizeSpec(com.zlt.aps.common.core.utils.BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.BIG_SIZE_SPEC, DEFAULT_BIG_SIZE_SPEC)));
        params.setMinPlanQty(getDouble(paramsMap.getOrDefault(EngineConstants.MIN_PLAN_QTY, DEFAULT_MIN_PLAN_QTY)));
        params.setStockLossRate(getDouble(paramsMap.getOrDefault(EngineConstants.STOCK_LOSS_RATE, "0")));
        params.setEqualShareThreshold(new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD)));
        params.setClassStockReference(getDouble(paramsMap.getOrDefault(EngineConstants.CLASS_STOCK_REFERENCE, DEFAULT_CLASS_STOCK_REFERENCE)));
        params.setOneRollNum(new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM)));
        params.setBackupShiftCount(getDouble(paramsMap.getOrDefault(EngineConstants.BACKUP_SHIFT_COUNT, "1")));
        params.setDemandCoefficient(getDouble(paramsMap.getOrDefault(EngineConstants.DEMAND_COEFFICIENT, "2")));
        return params;
    }

    /**
     * 加载外协规格Map
     */
    private Map<String, String> loadAssistSpecMap() {
        Map<String, String> map = new HashMap<>();
        List<String> listAssistSpec = tqEngineMapper.listAssistSpec();
        if (listAssistSpec == null || listAssistSpec.isEmpty()) {
            return map;
        }
        for (String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 加载当天库存
     */
    private Map<String, Double> loadTqStock(String scheduleDate) {
        return tqEngineStockMapper.listTqStock(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeadCode()))
                .collect(Collectors.toMap(TqStockVo::getBeadCode, TqStockVo::getStockNum));
    }

    /**
     * 加载当天早班(D日早班)计划量
     * 昨天排程的CLASS3_PLAN_QTY对应今天早班
     */
    private Map<String, Double> loadTodayMorningPlan(String scheduleDate) {
        return tqEngineStockMapper.listTodayMorningPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeadCode()))
                .collect(Collectors.toMap(TqStockConsumeVo::getBeadCode, TqStockConsumeVo::getConsume));
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
        logDetail.append("16点预计库存集合：").append(toJSONString(context.getPlanStockMap())).append(division);
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
}
