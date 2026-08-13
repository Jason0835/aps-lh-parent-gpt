package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.api.enums.TmVersionMatchModeEnum;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.mapper.TmEngineInventoryPredictMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import com.zlt.aps.tm.engine.service.ITmInventoryPredictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面库存预测默认步骤服务。
 *
 * <p>负责读取库存和损耗相关数据并计算供应时长。
 * 第一个班的滚动库存使用14点预计库存，计算公式：
 * 14点预计库存 = 6点库存 - 早班需求量 + 早班计划量</p>
 *
 * <p>口径说明：
 * <ul>
 *   <li>6点库存：取排程日期前一天的 T_TM_STOCK 记录</li>
 *   <li>早班需求量：取排程日期当天的 T_CX_SCHEDULE_RESULT 早班(CLASS1)成型计划量，
 *       关联 T_MDM_CONSTRUCTION_INFO 获取胎面标准长度，按胎面编码汇总
 *       Σ(CLASS1_PLAN_QTY × TREAD_SHOULDER_LENGTH)；同一胎胚按胎面分组择一取施工版本
 *       （优先 BOM_DATA_VERSION 匹配，否则取最新有效记录），与主流程 selectFormingDemandRows 同口径，
 *       避免版本 join 失败导致早班需求归 0</li>
 *   <li>早班计划量：取排程日期前一天的 T_TM_SCHEDULE_RESULT 夜班(CLASS3)胎面计划量，
 *       按胎面编码汇总 CLASS3_PLAN_QTY</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmInventoryPredictService implements ITmInventoryPredictService {

    private final TmEngineStockMapper tmStockMapper;

    private final TmEngineInventoryPredictMapper tmEngineInventoryPredictMapper;

    @Override
    public void predict(TmScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }

        Date scheduleDate = context.getScheduleDate();
        if (scheduleDate == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_SCHEDULE_DATE_EMPTY.getDefaultMessage());
        }

        // 获取待排任务中的胎面编码列表
        List<String> treadCodes = context.getTaskDraftList().stream()
                .map(TmTaskDraft::getTreadCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (treadCodes.isEmpty()) {
            log.warn("没有待排任务，跳过库存预测");
            return;
        }

        String factoryCode = context.getFactoryCode();

        // 查询每个胎面的6点库存（取排程日期前一天的库存快照）
        Map<String, BigDecimal> sixClockStockMap = this.querySixClockStock(scheduleDate, treadCodes);
        this.handleMissingStock(context, treadCodes, sixClockStockMap, scheduleDate);

        // 查询早班需求量（当天早班成型消耗，按胎面标准长度折算）
        boolean useRecipe = this.isRecipeMode(context);
        Map<String, BigDecimal> firstShiftDemandMap = this.queryFirstShiftDemand(
                factoryCode, scheduleDate, treadCodes, useRecipe);

        // 查询早班计划量（前一天夜班胎面排程计划量）
        Map<String, BigDecimal> firstShiftPlanMap = this.queryFirstShiftPlan(factoryCode, scheduleDate, treadCodes);

        // 构建库存预测结果
        Map<String, TmStockForecast> stockForecastMap = new HashMap<>();
        for (String treadCode : treadCodes) {
            TmStockForecast forecast = new TmStockForecast();
            forecast.setTreadCode(treadCode);
            forecast.setSixClockStockQty(sixClockStockMap.getOrDefault(treadCode, BigDecimal.ZERO));
            forecast.setFirstShiftDemandQty(firstShiftDemandMap.getOrDefault(treadCode, BigDecimal.ZERO));
            forecast.setFirstShiftPlanQty(firstShiftPlanMap.getOrDefault(treadCode, BigDecimal.ZERO));
            forecast.calculateRollingStockQty();
            stockForecastMap.put(treadCode, forecast);
            // 打印6点库存计算公式
            log.info("[TM_INVENTORY_PREDICT] treadCode={}, 6点库存【stockQty】-不良数量【badQty】+调整数量【adjustQty】=实际库存【{}】",
                    treadCode, forecast.getSixClockStockQty());
            // 打印14点预计库存计算公式
            log.info("[TM_INVENTORY_PREDICT] treadCode={}, 14点预计库存【rollingStockQty】=6点库存【{}】-早班需求量【{}】+早班计划量【{}】=【{}】",
                    treadCode, forecast.getSixClockStockQty(), forecast.getFirstShiftDemandQty(),
                    forecast.getFirstShiftPlanQty(), forecast.getRollingStockQty());
        }

        context.setStockForecastMap(stockForecastMap);
        log.info("库存预测完成，共预测{}个胎面规格", stockForecastMap.size());
    }

    /**
     * 按参数处理缺少库存快照的胎面规格。
     *
     * @param context 排程上下文
     * @param treadCodes 待排胎面编码
     * @param stockMap 已查询库存映射
     * @param scheduleDate 排程日期
     * @throws ServiceException 策略为 ERROR 且存在缺失库存时抛出
     */
    private void handleMissingStock(TmScheduleContext context, List<String> treadCodes,
                                    Map<String, BigDecimal> stockMap, Date scheduleDate) {
        List<String> missingCodeList = treadCodes.stream()
                .filter(treadCode -> !stockMap.containsKey(treadCode))
                .collect(Collectors.toList());
        if (missingCodeList.isEmpty()) {
            return;
        }
        String policy = context.getParam(TmScheduleConstants.PARAM_STOCK_MISSING_POLICY).getEffectiveValue();
        String stockDate = DateUtil.formatDate(DateUtil.offsetDay(scheduleDate, -1));
        if ("ERROR".equalsIgnoreCase(policy)) {
            throw new ServiceException(MessageFormat.format(
                    I18nUtil.getMessage("ui.tm.schedule.stockMissingBlocked"),
                    stockDate, String.join(",", missingCodeList)));
        }
        for (String treadCode : missingCodeList) {
            log.warn("[TM_STOCK_MISSING] policy=ZERO, scheduleDate={}, treadCode={}",
                    DateUtil.formatDate(scheduleDate), treadCode);
        }
    }

    /**
     * 判断当前是否 RECIPE 模式（按示方书版本关联施工）。
     *
     * <p>读取数据加载阶段写入上下文的 {@code TM_VERSION_MATCH_MODE} 参数，默认 RECIPE；
     * 仅当显式配置为 BOM 时返回 false，保证与数据加载口径一致。</p>
     *
     * @param context 自动排程上下文
     * @return true 表示 RECIPE 模式
     */
    private boolean isRecipeMode(TmScheduleContext context) {
        if (context == null || context.getParamMap() == null) {
            return true;
        }
        TmParamValue value = context.getParamMap().get(TmScheduleConstants.PARAM_VERSION_MATCH_MODE);
        if (value == null) {
            return true;
        }
        String mode = value.getEffectiveValue();
        return TmVersionMatchModeEnum.BOM != TmVersionMatchModeEnum.resolve(mode);
    }

    /**
     * 查询6点库存。
     *
     * <p>取排程日期前一天的 T_TM_STOCK 记录作为6点库存快照。
     * 前一天无记录时该胎面库存按0处理并提示。</p>
     *
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @return 胎面编码 -> 6点库存 的映射
     */
    private Map<String, BigDecimal> querySixClockStock(Date scheduleDate, List<String> treadCodes) {
        // 6点库存取排程日期前一天的库存记录
        Date yesterday = DateUtil.offsetDay(scheduleDate, -1);
        LambdaQueryWrapper<TmStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmStock::getStockDate, yesterday)
                .in(TmStock::getTreadCode, treadCodes);

        List<TmStock> stockList = tmStockMapper.selectList(wrapper);
        if (stockList.isEmpty()) {
            log.warn("排程日期[{}]前一天[{}]无6点库存记录，相关胎面库存按0处理",
                    DateUtil.formatDate(scheduleDate), DateUtil.formatDate(yesterday));
        }
        return stockList.stream()
                .collect(Collectors.toMap(
                        TmStock::getTreadCode,
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
     * 关联 T_MDM_CONSTRUCTION_INFO 获取胎面标准长度（TREAD_SHOULDER_LENGTH），
     * 按胎面编码汇总 Σ(CLASS1_PLAN_QTY × TREAD_SHOULDER_LENGTH)。
     * 当天无早班成型数据时按0处理。</p>
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @param useRecipe    是否按 RECIPE 模式（示方书版本）关联施工
     * @return 胎面编码 -> 早班需求量 的映射
     */
    private Map<String, BigDecimal> queryFirstShiftDemand(String factoryCode, Date scheduleDate, List<String> treadCodes,
                                                          boolean useRecipe) {
        if (treadCodes == null || treadCodes.isEmpty()) {
            return new HashMap<>();
        }
        List<TmInventoryPredictQtyVo> rowList;
        try {
            rowList = useRecipe
                    ? tmEngineInventoryPredictMapper.selectFirstShiftDemandRowsByRecipe(factoryCode, scheduleDate, treadCodes)
                    : tmEngineInventoryPredictMapper.selectFirstShiftDemandRows(factoryCode, scheduleDate, treadCodes);
        } catch (RuntimeException ex) {
            log.error("查询早班需求量失败，factoryCode={}, scheduleDate={}, useRecipe={}",
                    factoryCode, DateUtil.formatDate(scheduleDate), useRecipe, ex);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.firstShiftDemandQueryFailed"), ex);
        }
        if (rowList == null || rowList.isEmpty()) {
            log.info("排程日期[{}]当天无早班成型计划数据，早班需求量按0处理（useRecipe={}）", DateUtil.formatDate(scheduleDate), useRecipe);
            return new HashMap<>();
        }
        Map<String, BigDecimal> demandMap = new HashMap<>();
        for (TmInventoryPredictQtyVo row : rowList) {
            String treadCode = row.getTreadCode();
            if (treadCode == null || treadCode.trim().isEmpty()) {
                continue;
            }
            demandMap.put(treadCode, toBigDecimal(row.getQty()));
        }
        return demandMap;
    }

    /**
     * 查询早班计划量（从T_TM_SCHEDULE_RESULT获取前一天夜班胎面排程计划量）。
     *
     * <p>查询排程日期前一天的 T_TM_SCHEDULE_RESULT 夜班（CLASS3）胎面计划量，
     * 按胎面编码汇总 CLASS3_PLAN_QTY。前一天无夜班排程结果时按0处理。</p>
     *
     * @param factoryCode  工厂编号
     * @param scheduleDate 排程日期
     * @param treadCodes   胎面编码列表
     * @return 胎面编码 -> 早班计划量 的映射
     */
    private Map<String, BigDecimal> queryFirstShiftPlan(String factoryCode, Date scheduleDate, List<String> treadCodes) {
        if (treadCodes == null || treadCodes.isEmpty()) {
            return new HashMap<>();
        }
        // 早班计划量取排程日期前一天的夜班（CLASS3）胎面计划量，按胎面编码汇总
        Date yesterday = DateUtil.offsetDay(scheduleDate, -1);
        List<TmInventoryPredictQtyVo> rowList;
        try {
            rowList = tmEngineInventoryPredictMapper.selectFirstShiftPlanRows(factoryCode, yesterday, treadCodes);
        } catch (RuntimeException ex) {
            log.error("查询早班计划量失败，factoryCode={}, scheduleDate={}",
                    factoryCode, DateUtil.formatDate(yesterday), ex);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.firstShiftPlanQueryFailed"), ex);
        }
        if (rowList == null || rowList.isEmpty()) {
            log.info("排程日期[{}]前一天[{}]无夜班胎面排程结果，早班计划量按0处理",
                    DateUtil.formatDate(scheduleDate), DateUtil.formatDate(yesterday));
            return new HashMap<>();
        }
        Map<String, BigDecimal> planMap = new HashMap<>();
        for (TmInventoryPredictQtyVo row : rowList) {
            String treadCode = row.getTreadCode();
            if (treadCode == null || treadCode.trim().isEmpty()) {
                continue;
            }
            planMap.put(treadCode, toBigDecimal(row.getQty()));
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
