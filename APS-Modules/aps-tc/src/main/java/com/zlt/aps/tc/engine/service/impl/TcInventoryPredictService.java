package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.enums.TcScheduleErrorCodeEnum;
import com.zlt.aps.tc.api.enums.TcVersionMatchModeEnum;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.mapper.TcEngineInventoryPredictMapper;
import com.zlt.aps.tc.engine.mapper.TcEngineStockMapper;
import com.zlt.aps.tc.engine.service.ITcInventoryPredictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧库存预测默认步骤服务。
 *
 * <p>负责读取库存和损耗相关数据并计算供应时长。
 * 第一个班的滚动库存使用14点预计库存，计算公式：
 * 14点预计库存 = 6点库存 - 早班需求量 + 早班计划量</p>
 *
 * <p>口径说明：
 * <ul>
 *   <li>6点库存：取排程日期前一天的 T_TC_STOCK 记录</li>
 *   <li>早班需求量：取排程日期当天的 T_CX_SCHEDULE_RESULT 早班(CLASS1)成型计划量，
 *       关联 T_MDM_CONSTRUCTION_INFO 获取胎侧标准长度，按胎侧编码汇总
 *       Σ(CLASS1_PLAN_QTY × SIDEWALL_LENGTH)；同一胎胚按胎侧分组择一取施工版本
 *       （优先 BOM_DATA_VERSION 匹配，否则取最新有效记录），与主流程 selectFormingDemandRows 同口径，
 *       避免版本 join 失败导致早班需求归 0</li>
 *   <li>早班计划量：取排程日期前一天的 T_TC_SCHEDULE_RESULT 夜班(CLASS3)胎侧计划量，
 *       按胎侧编码汇总 CLASS3_PLAN_QTY</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcInventoryPredictService implements ITcInventoryPredictService {

    private final TcEngineStockMapper tmStockMapper;

    private final TcEngineInventoryPredictMapper tmEngineInventoryPredictMapper;

    @Override
    public void predict(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }

        Date scheduleDate = context.getScheduleDate();
        if (scheduleDate == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }

        // 获取待排任务中的胎侧编码列表
        List<String> sidewallCodes = context.getTaskDraftList().stream()
                .map(TcTaskDraft::getSidewallCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (sidewallCodes.isEmpty()) {
            log.warn("没有待排任务，跳过库存预测");
            return;
        }

        String factoryCode = context.getFactoryCode();

        // 查询每个胎侧的6点库存（取排程日期前一天的库存快照）
        Map<String, BigDecimal> sixClockStockMap = querySixClockStock(scheduleDate, sidewallCodes);
        this.handleMissingStock(context, sidewallCodes, sixClockStockMap, scheduleDate);

        // 查询早班需求量（当天早班成型消耗，按胎侧标准长度折算）
        boolean useRecipe = isRecipeMode(context);
        Map<String, BigDecimal> firstShiftDemandMap = queryFirstShiftDemand(factoryCode, scheduleDate, sidewallCodes, useRecipe);

        // 查询早班计划量（前一天夜班胎侧排程计划量）
        Map<String, BigDecimal> firstShiftPlanMap = queryFirstShiftPlan(factoryCode, scheduleDate, sidewallCodes);

        // 构建库存预测结果
        Map<String, TcStockForecast> stockForecastMap = new HashMap<>();
        for (String sidewallCode : sidewallCodes) {
            TcStockForecast forecast = new TcStockForecast();
            forecast.setSidewallCode(sidewallCode);
            forecast.setSixClockStockQty(sixClockStockMap.getOrDefault(sidewallCode, BigDecimal.ZERO));
            forecast.setFirstShiftDemandQty(firstShiftDemandMap.getOrDefault(sidewallCode, BigDecimal.ZERO));
            forecast.setFirstShiftPlanQty(firstShiftPlanMap.getOrDefault(sidewallCode, BigDecimal.ZERO));
            forecast.calculateRollingStockQty();
            stockForecastMap.put(sidewallCode, forecast);
            // 打印6点库存计算公式
            log.info("[TC_INVENTORY_PREDICT] sidewallCode={}, 6点库存【stockQty】-不良数量【badQty】+调整数量【adjustQty】=实际库存【{}】",
                    sidewallCode, forecast.getSixClockStockQty());
            // 打印14点预计库存计算公式
            log.info("[TC_INVENTORY_PREDICT] sidewallCode={}, 14点预计库存【rollingStockQty】=6点库存【{}】-早班需求量【{}】+早班计划量【{}】=【{}】",
                    sidewallCode, forecast.getSixClockStockQty(), forecast.getFirstShiftDemandQty(),
                    forecast.getFirstShiftPlanQty(), forecast.getRollingStockQty());
        }

        context.setStockForecastMap(stockForecastMap);
        log.info("库存预测完成，共预测{}个胎侧规格", stockForecastMap.size());
    }

    /**
     * 按参数处理缺少库存快照的胎侧规格。
     *
     * @param context 排程上下文
     * @param sidewallCodes 待排胎侧编码
     * @param stockMap 已查询库存映射
     * @param scheduleDate 排程日期
     * @throws ServiceException 策略为 ERROR 且存在缺失库存时抛出
     */
    private void handleMissingStock(TcScheduleContext context, List<String> sidewallCodes,
                                    Map<String, BigDecimal> stockMap, Date scheduleDate) {
        List<String> missingCodeList = sidewallCodes.stream()
                .filter(sidewallCode -> !stockMap.containsKey(sidewallCode))
                .collect(Collectors.toList());
        if (missingCodeList.isEmpty()) {
            return;
        }
        String policy = context.getParam(TcScheduleConstants.PARAM_STOCK_MISSING_POLICY).getEffectiveValue();
        if ("ERROR".equalsIgnoreCase(policy)) {
            throw new ServiceException(MessageFormat.format(
                    I18nUtil.getMessage("ui.tc.schedule.stockMissingBlocked"),
                    DateUtil.formatDate(DateUtil.offsetDay(scheduleDate, -1)), String.join(",", missingCodeList)));
        }
        for (String sidewallCode : missingCodeList) {
            log.warn("[TC_STOCK_MISSING] policy=ZERO, scheduleDate={}, sidewallCode={}",
                    DateUtil.formatDate(scheduleDate), sidewallCode);
        }
    }

    /**
     * 判断当前是否 RECIPE 模式（按示方书版本关联施工）。
     *
     * <p>读取数据加载阶段写入上下文的 {@code TC_VERSION_MATCH_MODE} 参数，默认 RECIPE；
     * 仅当显式配置为 BOM 时返回 false，保证与数据加载口径一致。</p>
     *
     * @param context 自动排程上下文
     * @return true 表示 RECIPE 模式
     */
    private boolean isRecipeMode(TcScheduleContext context) {
        if (context == null || context.getParamMap() == null) {
            return true;
        }
        TcParamValue value = context.getParamMap().get(TcScheduleConstants.PARAM_VERSION_MATCH_MODE);
        if (value == null) {
            return true;
        }
        String mode = value.getEffectiveValue();
        return TcVersionMatchModeEnum.BOM != TcVersionMatchModeEnum.resolve(mode);
    }

    /**
     * 查询6点库存。
     *
     * <p>取排程日期前一天的 T_TC_STOCK 记录作为6点库存快照。
     * 前一天无记录时该胎侧库存按0处理并提示。</p>
     *
     * @param scheduleDate 排程日期
     * @param sidewallCodes   胎侧编码列表
     * @return 胎侧编码 -> 6点库存 的映射
     */
    private Map<String, BigDecimal> querySixClockStock(Date scheduleDate, List<String> sidewallCodes) {
        // 6点库存取排程日期前一天的库存记录
        Date yesterday = DateUtil.offsetDay(scheduleDate, -1);
        LambdaQueryWrapper<TcStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcStock::getStockDate, yesterday)
                .in(TcStock::getSidewallCode, sidewallCodes);

        List<TcStock> stockList = tmStockMapper.selectList(wrapper);
        if (stockList.isEmpty()) {
            log.warn("排程日期[{}]前一天[{}]无6点库存记录，相关胎侧库存按0处理",
                    DateUtil.formatDate(scheduleDate), DateUtil.formatDate(yesterday));
        }
        return stockList.stream()
                .collect(Collectors.toMap(
                        TcStock::getSidewallCode,
                        stock -> {
                            BigDecimal result = stock.getStockQty() != null ? stock.getStockQty() : BigDecimal.ZERO;
                            BigDecimal badQty = stock.getBadQty() != null ? stock.getBadQty() : BigDecimal.ZERO;
                            BigDecimal adjustQty = stock.getAdjustQty() != null ? stock.getAdjustQty() : BigDecimal.ZERO;
                            result = BigDecimalUtils.sub(result, badQty);
                            result = BigDecimalUtils.add(result, adjustQty);
                            return result;
                        },
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 查询早班需求量（从成型计划表）。
     *
     * <p>查询排程日期当天的 T_CX_SCHEDULE_RESULT 早班（CLASS1）成型计划量，
     * 关联 T_MDM_CONSTRUCTION_INFO 获取胎侧标准长度（SIDEWALL_LENGTH），
     * 按胎侧编码汇总 Σ(CLASS1_PLAN_QTY × SIDEWALL_LENGTH)。
     * 当天无早班成型数据时按0处理。</p>
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param sidewallCodes   胎侧编码列表
     * @param useRecipe    是否按 RECIPE 模式（示方书版本）关联施工
     * @return 胎侧编码 -> 早班需求量 的映射
     */
    private Map<String, BigDecimal> queryFirstShiftDemand(String factoryCode, Date scheduleDate, List<String> sidewallCodes,
                                                          boolean useRecipe) {
        if (sidewallCodes == null || sidewallCodes.isEmpty()) {
            return new HashMap<>();
        }
        List<TcInventoryPredictQtyVo> rowList;
        try {
            rowList = useRecipe
                    ? tmEngineInventoryPredictMapper.selectFirstShiftDemandRowsByRecipe(factoryCode, scheduleDate, sidewallCodes)
                    : tmEngineInventoryPredictMapper.selectFirstShiftDemandRows(factoryCode, scheduleDate, sidewallCodes);
        } catch (RuntimeException ex) {
            log.error("查询早班需求量失败，factoryCode={}, scheduleDate={}, useRecipe={}",
                    factoryCode, DateUtil.formatDate(scheduleDate), useRecipe, ex);
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.firstShiftDemandQueryFailed"), ex);
        }
        if (rowList == null || rowList.isEmpty()) {
            log.info("排程日期[{}]当天无早班成型计划数据，早班需求量按0处理（useRecipe={}）", DateUtil.formatDate(scheduleDate), useRecipe);
            return new HashMap<>();
        }
        Map<String, BigDecimal> demandMap = new HashMap<>();
        for (TcInventoryPredictQtyVo row : rowList) {
            String sidewallCode = row.getSidewallCode();
            if (sidewallCode == null || sidewallCode.trim().isEmpty()) {
                continue;
            }
            demandMap.put(sidewallCode, toBigDecimal(row.getQty()));
        }
        return demandMap;
    }

    /**
     * 查询早班计划量（从T_TC_SCHEDULE_RESULT获取前一天夜班胎侧排程计划量）。
     *
     * <p>查询排程日期前一天的 T_TC_SCHEDULE_RESULT 夜班（CLASS3）胎侧计划量，
     * 按胎侧编码汇总 CLASS3_PLAN_QTY。前一天无夜班排程结果时按0处理。</p>
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param sidewallCodes   胎侧编码列表
     * @return 胎侧编码 -> 早班计划量 的映射
     */
    private Map<String, BigDecimal> queryFirstShiftPlan(String factoryCode, Date scheduleDate, List<String> sidewallCodes) {
        if (sidewallCodes == null || sidewallCodes.isEmpty()) {
            return new HashMap<>();
        }
        // 早班计划量取排程日期前一天的夜班（CLASS3）胎侧计划量，按胎侧编码汇总
        Date yesterday = DateUtil.offsetDay(scheduleDate, -1);
        List<TcInventoryPredictQtyVo> rowList;
        try {
            rowList = tmEngineInventoryPredictMapper.selectFirstShiftPlanRows(factoryCode, yesterday, sidewallCodes);
        } catch (RuntimeException ex) {
            log.error("查询早班计划量失败，factoryCode={}, scheduleDate={}",
                    factoryCode, DateUtil.formatDate(yesterday), ex);
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.firstShiftPlanQueryFailed"), ex);
        }
        if (rowList == null || rowList.isEmpty()) {
            log.info("排程日期[{}]前一天[{}]无夜班胎侧排程结果，早班计划量按0处理",
                    DateUtil.formatDate(scheduleDate), DateUtil.formatDate(yesterday));
            return new HashMap<>();
        }
        Map<String, BigDecimal> planMap = new HashMap<>();
        for (TcInventoryPredictQtyVo row : rowList) {
            String sidewallCode = row.getSidewallCode();
            if (sidewallCode == null || sidewallCode.trim().isEmpty()) {
                continue;
            }
            planMap.put(sidewallCode, toBigDecimal(row.getQty()));
        }
        return planMap;
    }

    /**
     * 将查询结果值转换为 BigDecimal。
     *
     * @param value 查询结果值
     * @return BigDecimal 值，null 时返回 0
     */
    private BigDecimal toBigDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
