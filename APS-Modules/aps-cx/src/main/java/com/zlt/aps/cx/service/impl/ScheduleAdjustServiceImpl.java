package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.CxKeyProductMapper;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxParamConfigMapper;
import com.zlt.aps.cx.mapper.CxScheduleDetailMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxShiftConfigMapper;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.mapper.CxStructureTreadConfigMapper;
import com.zlt.aps.cx.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.cx.mapper.MdmMonthPlanProductLhCapacityMapper;
import com.zlt.aps.cx.mapper.MdmMonthSurplusMapper;
import com.zlt.aps.cx.mapper.MdmStructureLhRatioMapper;
import com.zlt.aps.cx.mapper.MpCxCapacityConfigurationMapper;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.mapper.CxMaterialEndingMapper;
import com.zlt.aps.cx.service.ScheduleAdjustService;
import com.zlt.aps.cx.vo.ScheduleAdjustResultVo;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import com.zlt.aps.mp.api.domain.entity.MdmSkuScheduleCategory;
import com.zlt.aps.cx.mapper.MdmSkuScheduleCategoryMapper;
import com.zlt.aps.cx.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型排程计划调整服务实现
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleAdjustServiceImpl implements ScheduleAdjustService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_ENDING_DISCARD_THRESHOLD = 2;

    @Value("${aps.schedule.adjust.stock-hours-threshold:6}")
    private int stockHoursThreshold;

    @Value("${aps.schedule.adjust.before-handover-minutes:60}")
    private int beforeHandoverMinutes;

    @Value("${aps.schedule.adjust.tread-parking-warning-minutes:10}")
    private int treadParkingWarningMinutes;

    @Value("${aps.schedule.adjust.tread-parking-hours:4}")
    private int treadParkingHours;

    @Autowired
    private CxScheduleResultMapper scheduleResultMapper;

    @Autowired
    private CxScheduleDetailMapper scheduleDetailMapper;

    @Autowired
    private CxShiftConfigMapper shiftConfigMapper;

    @Autowired
    private CxStockMapper stockMapper;

    @Autowired
    private CxParamConfigMapper paramConfigMapper;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private MdmMaterialInfoMapper materialInfoMapper;

    @Autowired
    private MdmMonthPlanProductLhCapacityMapper lhCapacityMapper;

    @Autowired
    private MdmMonthSurplusMapper monthSurplusMapper;

    @Autowired
    private MdmStructureLhRatioMapper structureLhRatioMapper;

    @Autowired
    private MpCxCapacityConfigurationMapper capacityConfigMapper;

    @Autowired
    private CxStructureTreadConfigMapper structureTreadConfigMapper;

    @Autowired
    private CxKeyProductMapper keyProductMapper;

    @Autowired
    private CxMaterialEndingMapper materialEndingMapper;

    @Autowired
    private MdmSkuScheduleCategoryMapper skuScheduleCategoryMapper;

    @Autowired
    private MdmWorkCalendarMapper workCalendarMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleAdjustResultVo adjustByStockHours(String factoryCode, String scheduleDateStr, String shiftClass) {
        ScheduleAdjustResultVo result = new ScheduleAdjustResultVo();
        result.setSuccess(true);

        LocalDate scheduleDate = LocalDate.parse(scheduleDateStr, DATE_FMT);

        int threshold = getIntParamValue("ADJUST_STOCK_HOURS_THRESHOLD", stockHoursThreshold);

        log.info("交班库存时长调整开始：factory={}, date={}, shift={}, threshold={}h",
                factoryCode, scheduleDateStr, shiftClass, threshold);

        // 1. 查询主表数据
        List<CxScheduleResult> mainResults = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                        .eq(StringUtils.hasText(factoryCode), CxScheduleResult::getFactoryCode, factoryCode));

        if (CollectionUtils.isEmpty(mainResults)) {
            result.setSuccess(false);
            result.setMessage("未找到排程数据：date=" + scheduleDateStr);
            return result;
        }

        // 2. 查询班次配置，获取班次结束时间
        CxShiftConfig currentShiftConfig = shiftConfigMapper.selectOne(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getClassField, shiftClass)
                        .eq(CxShiftConfig::getIsActive, 1));

        if (currentShiftConfig == null) {
            result.setSuccess(false);
            result.setMessage("未找到班次配置：" + shiftClass);
            return result;
        }

        // 计算距离交班还有多少小时（当前时间到班次结束时间）
        LocalTime shiftEndTime = currentShiftConfig.getShiftEndTime();
        LocalTime now = LocalTime.now();
        int remainingMinutes = (int) java.time.Duration.between(now, shiftEndTime).toMinutes();
        if (remainingMinutes < 0) {
            // 跨午夜：加上24小时
            remainingMinutes += 24 * 60;
        }
        double remainingHours = Math.max(0, remainingMinutes / 60.0);

        // 3. 查询子表数据，按主表ID分组
        List<Long> mainIds = mainResults.stream().map(CxScheduleResult::getId).collect(Collectors.toList());
        List<CxScheduleDetail> allDetails = scheduleDetailMapper.selectList(
                new LambdaQueryWrapper<CxScheduleDetail>()
                        .in(CxScheduleDetail::getMainId, mainIds));

        Map<Long, List<CxScheduleDetail>> detailsByMainId = allDetails.stream()
                .collect(Collectors.groupingBy(CxScheduleDetail::getMainId));
        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));

        // 4. 构建硫化机台数和模数映射
        Map<String, Integer> embryoVulcanizeMachineCount = new HashMap<>();
        Map<String, Integer> embryoVulcanizeMoldCount = new HashMap<>();
        for (CxScheduleResult mr : mainResults) {
            if (mr.getEmbryoCode() != null) {
                embryoVulcanizeMachineCount.putIfAbsent(mr.getEmbryoCode(),
                        mr.getLhMachineQty() != null ? mr.getLhMachineQty().intValue() : 1);
                embryoVulcanizeMoldCount.putIfAbsent(mr.getEmbryoCode(), 1);
            }
        }

        // 5. 查询实时库存
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .eq(CxStock::getStockDate, java.sql.Date.valueOf(scheduleDate)));

        Map<String, Integer> stockMap = new HashMap<>();
        for (CxStock stock : stocks) {
            if (stock.getEmbryoCode() != null) {
                stockMap.put(stock.getEmbryoCode(), stock.getEffectiveStock());
            }
        }

        // 6. 遍历子表，按胎胚汇总当前班次的库存时长
        // key=embryoCode, value=EmbryoStockHours（聚合同胎胚不同车次的数据）
        Map<String, EmbryoStockHours> embryoStockHoursMap = new LinkedHashMap<>();

        for (CxScheduleResult mr : mainResults) {
            String embryoCode = mr.getEmbryoCode();
            List<CxScheduleDetail> details = detailsByMainId.getOrDefault(mr.getId(), Collections.emptyList());

            // 汇总该胎胚在当前班次的总计划量和车次信息
            BigDecimal totalPlanQty = BigDecimal.ZERO;
            int totalTripCapacity = 0;
            for (CxScheduleDetail detail : details) {
                BigDecimal planQty = getDetailClassPlanQty(detail, shiftClass);
                BigDecimal tripCapacity = getDetailClassTripCapacity(detail, shiftClass);
                if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                    totalPlanQty = totalPlanQty.add(planQty);
                    if (tripCapacity != null && tripCapacity.intValue() > 0) {
                        totalTripCapacity = tripCapacity.intValue();
                    }
                }
            }

            if (totalPlanQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 计算交班库存时长 = (实时库存 / 硫化机台数 / 模数) - 距交班时长
            Integer realtimeStock = stockMap.getOrDefault(embryoCode, 0);
            int machineCount = embryoVulcanizeMachineCount.getOrDefault(embryoCode, 1);
            int moldCount = embryoVulcanizeMoldCount.getOrDefault(embryoCode, 1);
            double stockHours = (double) realtimeStock / machineCount / moldCount - remainingHours;

            EmbryoStockHours esh = embryoStockHoursMap.get(embryoCode);
            if (esh == null) {
                esh = new EmbryoStockHours();
                esh.setMachineCode(mr.getCxMachineCode());
                esh.setEmbryoCode(embryoCode);
                esh.setMaterialCode(mr.getMaterialCode());
                esh.setMainId(mr.getId());
                embryoStockHoursMap.put(embryoCode, esh);
            }
            // 取库存时长最小的（最紧急的）
            if (esh.getStockHours() == null || stockHours < esh.getStockHours().doubleValue()) {
                esh.setStockHours(BigDecimal.valueOf(stockHours).setScale(2, RoundingMode.HALF_UP));
            }
            esh.setPlanQty(totalPlanQty);
            esh.setTripCapacity(totalTripCapacity > 0 ? totalTripCapacity : 11); // 默认整车11条
            esh.setRealtimeStock(realtimeStock);
            esh.setRemainingHours(remainingHours);
        }

        // 7. 分类：需要补车的 vs 库存时长最多的
        List<EmbryoStockHours> needAddList = embryoStockHoursMap.values().stream()
                .filter(e -> e.getStockHours().doubleValue() < threshold)
                .sorted(Comparator.comparingDouble(a -> a.getStockHours().doubleValue()))
                .collect(Collectors.toList());

        List<EmbryoStockHours> sortedByStockHours = embryoStockHoursMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getStockHours().doubleValue(), a.getStockHours().doubleValue()))
                .collect(Collectors.toList());

        // 8. 执行补车和减车：操作子表，然后回调主表
        Set<String> addedEmbryos = new HashSet<>();  // 避免同胎胚重复补车

        for (EmbryoStockHours needAdd : needAddList) {
            if (addedEmbryos.contains(needAdd.getEmbryoCode())) {
                continue;  // 同班次内同胎胚只补1车
            }

            int vehicleCapacity = needAdd.getTripCapacity();

            // 补1车：在子表中找到该胎胚最后一个车次，增加1车
            Long mainId = needAdd.getMainId();
            List<CxScheduleDetail> details = detailsByMainId.getOrDefault(mainId, Collections.emptyList());

            // 找到该胎胚当前班次最后一个有计划量的车次
            CxScheduleDetail lastDetail = null;
            int maxTripNo = 0;
            for (CxScheduleDetail detail : details) {
                if (!needAdd.getEmbryoCode().equals(detail.getEmbryoCode())) {
                    continue;
                }
                BigDecimal planQty = getDetailClassPlanQty(detail, shiftClass);
                String tripNoStr = getDetailClassTripNo(detail, shiftClass);
                int tripNo = parseTripNo(tripNoStr);
                if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0 && tripNo > maxTripNo) {
                    maxTripNo = tripNo;
                    lastDetail = detail;
                }
            }

            if (lastDetail == null) {
                continue;
            }

            BigDecimal beforeQty = getDetailClassPlanQty(lastDetail, shiftClass);
            BigDecimal afterQty = beforeQty.add(BigDecimal.valueOf(vehicleCapacity));

            // 检查加车后不超班产限制
            BigDecimal shiftPlanQty = getClassPlanQty(mainMap.get(mainId), shiftClass);
            BigDecimal maxShiftQty = getMaxShiftCapacity(mainMap.get(mainId), currentShiftConfig);
            if (maxShiftQty != null && shiftPlanQty.add(BigDecimal.valueOf(vehicleCapacity)).compareTo(maxShiftQty) > 0) {
                log.info("胎胚 {} 补1车将超出班产限制，跳过", needAdd.getEmbryoCode());
                continue;
            }

            // 更新子表：最后一个车次加整车容量
            setDetailClassPlanQty(lastDetail, shiftClass, afterQty);
            scheduleDetailMapper.updateById(lastDetail);

            addedEmbryos.add(needAdd.getEmbryoCode());

            ScheduleAdjustResultVo.TripAdjustItem added = new ScheduleAdjustResultVo.TripAdjustItem();
            added.setMachineCode(needAdd.getMachineCode());
            added.setEmbryoCode(needAdd.getEmbryoCode());
            added.setMaterialCode(needAdd.getMaterialCode());
            added.setShiftClass(shiftClass);
            added.setTripNo(maxTripNo);
            added.setBeforePlanQty(beforeQty.intValue());
            added.setAfterPlanQty(afterQty.intValue());
            added.setStockHours(needAdd.getStockHours());
            result.getAddedTrips().add(added);

            // 对同班次内库存时长最多的胎胚减1车
            for (Iterator<EmbryoStockHours> it = sortedByStockHours.iterator(); it.hasNext(); ) {
                EmbryoStockHours toRemove = it.next();
                if (toRemove.getEmbryoCode().equals(needAdd.getEmbryoCode())) {
                    continue;  // 不能减自己
                }
                if (toRemove.getPlanQty().compareTo(BigDecimal.valueOf(vehicleCapacity)) <= 0) {
                    continue;  // 计划量不足1车，不能减
                }

                // 找到该胎胚在子表中最后一个有计划量的车次，减整车容量
                Long removeMainId = toRemove.getMainId();
                List<CxScheduleDetail> removeDetails = detailsByMainId.getOrDefault(removeMainId, Collections.emptyList());

                CxScheduleDetail removeLastDetail = null;
                int removeMaxTripNo = 0;
                for (CxScheduleDetail detail : removeDetails) {
                    if (!toRemove.getEmbryoCode().equals(detail.getEmbryoCode())) {
                        continue;
                    }
                    BigDecimal planQty = getDetailClassPlanQty(detail, shiftClass);
                    String tripNoStr = getDetailClassTripNo(detail, shiftClass);
                    int tripNo = parseTripNo(tripNoStr);
                    if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0 && tripNo > removeMaxTripNo) {
                        removeMaxTripNo = tripNo;
                        removeLastDetail = detail;
                    }
                }

                if (removeLastDetail == null) {
                    continue;
                }

                BigDecimal removeBeforeQty = getDetailClassPlanQty(removeLastDetail, shiftClass);
                BigDecimal removeAfterQty = removeBeforeQty.subtract(BigDecimal.valueOf(vehicleCapacity));

                if (removeAfterQty.compareTo(BigDecimal.ZERO) < 0) {
                    continue;  // 减后不能为负
                }

                // 更新子表
                setDetailClassPlanQty(removeLastDetail, shiftClass, removeAfterQty);
                scheduleDetailMapper.updateById(removeLastDetail);

                // 更新内存中的planQty，避免下次再选中
                toRemove.setPlanQty(toRemove.getPlanQty().subtract(BigDecimal.valueOf(vehicleCapacity)));

                ScheduleAdjustResultVo.TripAdjustItem removed = new ScheduleAdjustResultVo.TripAdjustItem();
                removed.setMachineCode(toRemove.getMachineCode());
                removed.setEmbryoCode(toRemove.getEmbryoCode());
                removed.setMaterialCode(toRemove.getMaterialCode());
                removed.setShiftClass(shiftClass);
                removed.setTripNo(removeMaxTripNo);
                removed.setBeforePlanQty(removeBeforeQty.intValue());
                removed.setAfterPlanQty(removeAfterQty.intValue());
                removed.setStockHours(toRemove.getStockHours());
                result.getRemovedTrips().add(removed);

                break;  // 每补1车只减1次
            }
        }

        // 9. 回调主表：按子表重新汇总每个班次的计划量
        recalculateMainTableFromDetails(mainResults, detailsByMainId);

        result.getAdjustedShifts().add(shiftClass);
        result.setMessage("交班库存时长调整完成：补车" + result.getAddedTrips().size()
                + "项，减车" + result.getRemovedTrips().size() + "项");
        log.info("交班库存时长调整完成：{}", result.getMessage());
        return result;
    }

    /**
     * 根据子表数据重新汇总主表各班次计划量
     */
    private void recalculateMainTableFromDetails(List<CxScheduleResult> mainResults,
                                                 Map<Long, List<CxScheduleDetail>> detailsByMainId) {
        for (CxScheduleResult main : mainResults) {
            List<CxScheduleDetail> details = detailsByMainId.getOrDefault(main.getId(), Collections.emptyList());
            if (details.isEmpty()) {
                continue;
            }

            boolean changed = false;
            for (int classIdx = 1; classIdx <= 8; classIdx++) {
                String shiftClass = "CLASS" + classIdx;
                BigDecimal totalQty = BigDecimal.ZERO;

                for (CxScheduleDetail detail : details) {
                    BigDecimal qty = getDetailClassPlanQty(detail, shiftClass);
                    if (qty != null) {
                        totalQty = totalQty.add(qty);
                    }
                }

                BigDecimal currentQty = getClassPlanQty(main, shiftClass);
                if (currentQty == null) currentQty = BigDecimal.ZERO;

                if (totalQty.compareTo(currentQty) != 0) {
                    updateClassPlanQty(main.getId(), shiftClass, totalQty);
                    changed = true;
                    log.info("主表回写: ID={}, {} 从 {} 更新为 {}", main.getId(), shiftClass, currentQty, totalQty);
                }
            }

            if (changed) {
                // 重新读取更新后的主表记录
                CxScheduleResult updated = scheduleResultMapper.selectById(main.getId());
                if (updated != null) {
                    main.setClass1PlanQty(updated.getClass1PlanQty());
                    main.setClass2PlanQty(updated.getClass2PlanQty());
                    main.setClass3PlanQty(updated.getClass3PlanQty());
                    main.setClass4PlanQty(updated.getClass4PlanQty());
                    main.setClass5PlanQty(updated.getClass5PlanQty());
                    main.setClass6PlanQty(updated.getClass6PlanQty());
                    main.setClass7PlanQty(updated.getClass7PlanQty());
                    main.setClass8PlanQty(updated.getClass8PlanQty());
                }
            }
        }
    }

    /**
     * 获取子表车次容量（整车条数）- 子表级别，所有班次共用
     */
    private BigDecimal getDetailClassTripCapacity(CxScheduleDetail detail, String shiftClass) {
        return detail.getTripCapacity();
    }

    /**
     * 设置子表某班次计划量
     */
    private void setDetailClassPlanQty(CxScheduleDetail detail, String shiftClass, BigDecimal planQty) {
        switch (shiftClass) {
            case "CLASS1": detail.setClass1PlanQty(planQty); break;
            case "CLASS2": detail.setClass2PlanQty(planQty); break;
            case "CLASS3": detail.setClass3PlanQty(planQty); break;
            case "CLASS4": detail.setClass4PlanQty(planQty); break;
            case "CLASS5": detail.setClass5PlanQty(planQty); break;
            case "CLASS6": detail.setClass6PlanQty(planQty); break;
            case "CLASS7": detail.setClass7PlanQty(planQty); break;
            case "CLASS8": detail.setClass8PlanQty(planQty); break;
            default: break;
        }
    }

    /**
     * 获取班次最大产能限制（班次时长 * 小时产能）
     */
    private BigDecimal getMaxShiftCapacity(CxScheduleResult result, CxShiftConfig shiftConfig) {
        if (shiftConfig == null || shiftConfig.getShiftHours() == null) {
            return null;
        }
        // 班次时长(小时) * 整车条数(近似小时产能) 作为上限
        // 实际应根据参数配置获取，这里用班次时长 * 默认小时产能
        double shiftHours = shiftConfig.getShiftHours();
        // 默认小时产能约16条（根据胎胚规格不同，取保守值）
        return BigDecimal.valueOf(shiftHours * 16);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleAdjustResultVo rollingAdjust(String factoryCode, String scheduleDateStr, String shiftClass) {
        ScheduleAdjustResultVo result = new ScheduleAdjustResultVo();
        result.setSuccess(true);

        LocalDate scheduleDate = LocalDate.parse(scheduleDateStr, DATE_FMT);

        int warningMinutes = getIntParamValue("ADJUST_TREAD_PARKING_WARNING_MINUTES", treadParkingWarningMinutes);
        int parkHours = getIntParamValue("ADJUST_TREAD_PARKING_HOURS", treadParkingHours);

        log.info("计划滚动调整开始：factory={}, date={}, shift={}, warningMinutes={}, parkHours={}",
                factoryCode, scheduleDateStr, shiftClass, warningMinutes, parkHours);

        // 确定需要调整的班次范围
        List<String> adjustShifts = getAdjustShiftRange(shiftClass);
        result.setAdjustedShifts(adjustShifts);

        // 查询主表数据
        List<CxScheduleResult> mainResults = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                        .eq(StringUtils.hasText(factoryCode), CxScheduleResult::getFactoryCode, factoryCode));

        if (CollectionUtils.isEmpty(mainResults)) {
            result.setSuccess(false);
            result.setMessage("未找到排程数据：date=" + scheduleDateStr);
            return result;
        }

        // 查询班次配置
        List<CxShiftConfig> allShiftConfigs = shiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getIsActive, 1)
                        .orderByAsc(CxShiftConfig::getShiftOrder));

        Map<String, CxShiftConfig> shiftConfigMap = new HashMap<>();
        for (CxShiftConfig cfg : allShiftConfigs) {
            shiftConfigMap.put(cfg.getClassField(), cfg);
        }

        // 查询子表明细
        List<Long> mainIds = mainResults.stream().map(CxScheduleResult::getId).collect(Collectors.toList());
        List<CxScheduleDetail> allDetails = scheduleDetailMapper.selectList(
                new LambdaQueryWrapper<CxScheduleDetail>()
                        .in(CxScheduleDetail::getMainId, mainIds));

        Map<Long, CxScheduleResult> mainMap = mainResults.stream()
                .collect(Collectors.toMap(CxScheduleResult::getId, r -> r));
        Map<Long, List<CxScheduleDetail>> detailsByMainId = allDetails.stream()
                .collect(Collectors.groupingBy(CxScheduleDetail::getMainId));

        // 按机台分组，对每个机台在每个需调整班次内重新计算顺位和时间
        int resequencedCount = 0;
        for (CxScheduleResult main : mainResults) {
            List<CxScheduleDetail> details = detailsByMainId.getOrDefault(main.getId(), Collections.emptyList());
            if (details.isEmpty()) {
                continue;
            }

            for (CxScheduleDetail detail : details) {
                resequencedCount += recalculateSequenceAndTime(detail, main, adjustShifts,
                        shiftConfigMap, result, parkHours, warningMinutes);
            }

            // 保存子表更新
            for (CxScheduleDetail detail : details) {
                scheduleDetailMapper.updateById(detail);
            }
        }

        result.setResequencedCount(resequencedCount);
        result.setMessage("计划滚动调整完成：重置顺位" + resequencedCount + "条，预警"
                + result.getTreadWarnings().size() + "项");
        log.info("计划滚动调整完成：{}", result.getMessage());
        return result;
    }

    @Override
    public List<String> getAdjustShiftRange(String currentShiftClass) {
        int classIndex = getClassIndex(currentShiftClass);
        if (classIndex <= 0) {
            return Collections.emptyList();
        }

        List<String> adjustShifts = new ArrayList<>();
        switch (classIndex) {
            case 1:
                // T日早班：CLASS2~CLASS8
                for (int i = 2; i <= 8; i++) {
                    adjustShifts.add("CLASS" + i);
                }
                break;
            case 2:
                // T日中班：CLASS3~CLASS8
                for (int i = 3; i <= 8; i++) {
                    adjustShifts.add("CLASS" + i);
                }
                break;
            case 3:
                // T+1日夜班：CLASS4~CLASS8
                for (int i = 4; i <= 8; i++) {
                    adjustShifts.add("CLASS" + i);
                }
                break;
            default:
                // T+1日早班及以后：全部8个班（新版本）
                for (int i = 1; i <= 8; i++) {
                    adjustShifts.add("CLASS" + i);
                }
                break;
        }
        return adjustShifts;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 重新计算单个明细在指定班次的顺位和时间，并进行胎面供应判断
     */
    private int recalculateSequenceAndTime(CxScheduleDetail detail, CxScheduleResult main,
                                           List<String> adjustShifts,
                                           Map<String, CxShiftConfig> shiftConfigMap,
                                           ScheduleAdjustResultVo result,
                                           int parkHours, int warningMinutes) {
        int count = 0;
        int machineHourlyCapacity = 12; // 默认小时产能

        for (String shift : adjustShifts) {
            BigDecimal planQty = getDetailClassPlanQty(detail, shift);
            if (planQty == null || planQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            CxShiftConfig shiftConfig = shiftConfigMap.get(shift);
            if (shiftConfig == null) {
                continue;
            }

            // 获取当前排程日期对应此班次的日期，使用classIndex推算
            int classIdx = getClassIndex(shift);
            LocalDate shiftDate = determineShiftDate(main.getScheduleDate(), classIdx);

            LocalTime shiftStartTime = shiftConfig.getShiftStartTime();
            LocalDateTime shiftStart = LocalDateTime.of(shiftDate, shiftStartTime);

            // 计算胎面可供成型开始时间
            // 前班次开始时间 + (胎面库存+胎面计划的可供成型时长) + 胎面停放时间(固定4小时)
            LocalDateTime treadAvailableTime = calculateTreadAvailableTime(
                    shiftStart, main.getEmbryoCode(), parkHours, getClassIndex(shift));

            // 计算该车次在本班次的成型预计开始/结束时间
            String tripNoStr = getDetailClassTripNo(detail, shift);
            int tripNo = parseTripNo(tripNoStr);

            long offsetMinutes = (long) (tripNo - 1) * planQty.intValue() * 60 / machineHourlyCapacity;
            long tripDurationMinutes = (long) planQty.intValue() * 60 / machineHourlyCapacity;

            LocalDateTime formingStart = shiftStart.plusMinutes(offsetMinutes);
            LocalDateTime formingEnd = shiftStart.plusMinutes(offsetMinutes + tripDurationMinutes);

            // 更新子表时间字段
            setDetailClassTime(detail, shift, formingStart, formingEnd);

            // 胎面供应判断
            if (treadAvailableTime != null) {
                if (treadAvailableTime.isAfter(formingStart)) {
                    // 胎面供应不上，顺位后移
                    setDetailClassSequence(detail, shift, 99);
                    ScheduleAdjustResultVo.TreadSupplyWarning warning = new ScheduleAdjustResultVo.TreadSupplyWarning();
                    warning.setMachineCode(main.getCxMachineCode());
                    warning.setEmbryoCode(main.getEmbryoCode());
                    warning.setMaterialCode(main.getMaterialCode());
                    warning.setShiftClass(shift);
                    warning.setTripNo(tripNo);
                    warning.setFormingStartTime(formingStart.format(DATETIME_FMT));
                    warning.setTreadAvailableTime(treadAvailableTime.format(DATETIME_FMT));
                    warning.setWarningType("SUPPLY_UNAVAILABLE");
                    warning.setDescription("胎面供应不上：胎面可供时间" + treadAvailableTime.format(DATETIME_FMT)
                            + " > 成型开始时间" + formingStart.format(DATETIME_FMT));
                    result.getTreadWarnings().add(warning);
                } else if (treadAvailableTime.plusMinutes(warningMinutes).isAfter(formingStart)) {
                    // 胎面供应得上但停放时间不足
                    ScheduleAdjustResultVo.TreadSupplyWarning warning = new ScheduleAdjustResultVo.TreadSupplyWarning();
                    warning.setMachineCode(main.getCxMachineCode());
                    warning.setEmbryoCode(main.getEmbryoCode());
                    warning.setMaterialCode(main.getMaterialCode());
                    warning.setShiftClass(shift);
                    warning.setTripNo(tripNo);
                    warning.setFormingStartTime(formingStart.format(DATETIME_FMT));
                    warning.setTreadAvailableTime(treadAvailableTime.format(DATETIME_FMT));
                    warning.setWarningType("PARKING_INSUFFICIENT");
                    warning.setDescription("胎面供应得上但停放时间不足（<" + warningMinutes + "分钟）：成型机等待");
                    result.getTreadWarnings().add(warning);
                }
            }

            count++;
        }
        return count;
    }

    /**
     * 计算胎面可供成型开始时间
     *
     * <p>公式：前班次开始时间 +（胎面库存+胎面计划的可供成型时长）+ 胎面停放时间
     *
     * <p>TODO 后续接入真实胎面数据后改造：
     * <ol>
     *   <li>胎面实时库存（需确认数据来源：MES推送表 或 外部接口）</li>
     *   <li>胎面计划产出（需确认数据来源：APS胎面排程表 或 MES胎面计划表）</li>
     *   <li>胎面小时产能（用于将库存+计划量转为时间）</li>
     * </ol>
     * <p>当前为简化占位实现：仅使用固定停放时间，胎面库存和计划暂未接入。
     */
    private LocalDateTime calculateTreadAvailableTime(LocalDateTime shiftStart, String embryoCode,
                                                      int parkHours, int shiftIndex) {
        // TODO: 后续接入胎面库存实时数据后改为动态计算
        // 完整公式：shiftStart + (treadStock + treadPlanQty) / treadHourlyCapacity * 60 + parkHours
        return shiftStart.plusHours(parkHours);
    }

    /**
     * 根据班次索引确定班次对应的日期
     */
    private LocalDate determineShiftDate(Date scheduleDate, int classIndex) {
        LocalDate baseDate = scheduleDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        // CLASS1,2 = T日; CLASS3,4,5 = T+1日; CLASS6,7,8 = T+2日
        if (classIndex <= 2) {
            return baseDate;
        } else if (classIndex <= 5) {
            return baseDate.plusDays(1);
        } else {
            return baseDate.plusDays(2);
        }
    }

    /**
     * 获取主表某班次计划量
     */
    private BigDecimal getClassPlanQty(CxScheduleResult result, String shiftClass) {
        switch (shiftClass) {
            case "CLASS1": return result.getClass1PlanQty();
            case "CLASS2": return result.getClass2PlanQty();
            case "CLASS3": return result.getClass3PlanQty();
            case "CLASS4": return result.getClass4PlanQty();
            case "CLASS5": return result.getClass5PlanQty();
            case "CLASS6": return result.getClass6PlanQty();
            case "CLASS7": return result.getClass7PlanQty();
            case "CLASS8": return result.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 更新主表某班次计划量
     */
    private void updateClassPlanQty(Long resultId, String shiftClass, BigDecimal planQty) {
        CxScheduleResult entity = new CxScheduleResult();
        entity.setId(resultId);
        switch (shiftClass) {
            case "CLASS1": entity.setClass1PlanQty(planQty); break;
            case "CLASS2": entity.setClass2PlanQty(planQty); break;
            case "CLASS3": entity.setClass3PlanQty(planQty); break;
            case "CLASS4": entity.setClass4PlanQty(planQty); break;
            case "CLASS5": entity.setClass5PlanQty(planQty); break;
            case "CLASS6": entity.setClass6PlanQty(planQty); break;
            case "CLASS7": entity.setClass7PlanQty(planQty); break;
            case "CLASS8": entity.setClass8PlanQty(planQty); break;
            default: break;
        }
        scheduleResultMapper.updateById(entity);
    }

    /**
     * 获取子表某班次计划量
     */
    private BigDecimal getDetailClassPlanQty(CxScheduleDetail detail, String shiftClass) {
        switch (shiftClass) {
            case "CLASS1": return detail.getClass1PlanQty();
            case "CLASS2": return detail.getClass2PlanQty();
            case "CLASS3": return detail.getClass3PlanQty();
            case "CLASS4": return detail.getClass4PlanQty();
            case "CLASS5": return detail.getClass5PlanQty();
            case "CLASS6": return detail.getClass6PlanQty();
            case "CLASS7": return detail.getClass7PlanQty();
            case "CLASS8": return detail.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 获取子表车次号 - 子表级别，所有班次共用
     */
    private String getDetailClassTripNo(CxScheduleDetail detail, String shiftClass) {
        return detail.getTripNo();
    }

    /**
     * 设置子表某班次时间
     */
    private void setDetailClassTime(CxScheduleDetail detail, String shiftClass,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        java.sql.Timestamp startTs = startTime != null ? java.sql.Timestamp.valueOf(startTime) : null;
        java.sql.Timestamp endTs = endTime != null ? java.sql.Timestamp.valueOf(endTime) : null;
        switch (shiftClass) {
            case "CLASS1":
                detail.setClass1PlanStartTime(startTs);
                detail.setClass1PlanEndTime(endTs);
                break;
            case "CLASS2":
                detail.setClass2PlanStartTime(startTs);
                detail.setClass2PlanEndTime(endTs);
                break;
            case "CLASS3":
                detail.setClass3PlanStartTime(startTs);
                detail.setClass3PlanEndTime(endTs);
                break;
            case "CLASS4":
                detail.setClass4PlanStartTime(startTs);
                detail.setClass4PlanEndTime(endTs);
                break;
            case "CLASS5":
                detail.setClass5PlanStartTime(startTs);
                detail.setClass5PlanEndTime(endTs);
                break;
            case "CLASS6":
                detail.setClass6PlanStartTime(startTs);
                detail.setClass6PlanEndTime(endTs);
                break;
            case "CLASS7":
                detail.setClass7PlanStartTime(startTs);
                detail.setClass7PlanEndTime(endTs);
                break;
            case "CLASS8":
                detail.setClass8PlanStartTime(startTs);
                detail.setClass8PlanEndTime(endTs);
                break;
            default: break;
        }
    }

    /**
     * 设置子表某班次顺位
     */
    private void setDetailClassSequence(CxScheduleDetail detail, String shiftClass, int sequence) {
        switch (shiftClass) {
            case "CLASS1": detail.setClass1Sequence(sequence); break;
            case "CLASS2": detail.setClass2Sequence(sequence); break;
            case "CLASS3": detail.setClass3Sequence(sequence); break;
            case "CLASS4": detail.setClass4Sequence(sequence); break;
            case "CLASS5": detail.setClass5Sequence(sequence); break;
            case "CLASS6": detail.setClass6Sequence(sequence); break;
            case "CLASS7": detail.setClass7Sequence(sequence); break;
            case "CLASS8": detail.setClass8Sequence(sequence); break;
            default: break;
        }
    }

    /**
     * 解析车次号字符串为整数
     */
    private int parseTripNo(String tripNoStr) {
        if (tripNoStr == null || tripNoStr.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(tripNoStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private BigDecimal getClassPlanQty(CxScheduleResult result, int classIdx) {
        switch (classIdx) {
            case 1: return result.getClass1PlanQty();
            case 2: return result.getClass2PlanQty();
            case 3: return result.getClass3PlanQty();
            case 4: return result.getClass4PlanQty();
            case 5: return result.getClass5PlanQty();
            case 6: return result.getClass6PlanQty();
            case 7: return result.getClass7PlanQty();
            case 8: return result.getClass8PlanQty();
            default: return null;
        }
    }

    private void setClassPlanQty(CxScheduleResult main, int classIdx, BigDecimal planQty) {
        switch (classIdx) {
            case 1: main.setClass1PlanQty(planQty); break;
            case 2: main.setClass2PlanQty(planQty); break;
            case 3: main.setClass3PlanQty(planQty); break;
            case 4: main.setClass4PlanQty(planQty); break;
            case 5: main.setClass5PlanQty(planQty); break;
            case 6: main.setClass6PlanQty(planQty); break;
            case 7: main.setClass7PlanQty(planQty); break;
            case 8: main.setClass8PlanQty(planQty); break;
        }
    }

    private BigDecimal getDetailClassPlanQty(CxScheduleDetail detail, int classIdx) {
        switch (classIdx) {
            case 1: return detail.getClass1PlanQty();
            case 2: return detail.getClass2PlanQty();
            case 3: return detail.getClass3PlanQty();
            case 4: return detail.getClass4PlanQty();
            case 5: return detail.getClass5PlanQty();
            case 6: return detail.getClass6PlanQty();
            case 7: return detail.getClass7PlanQty();
            case 8: return detail.getClass8PlanQty();
            default: return null;
        }
    }

    private void setDetailClassPlanQty(CxScheduleDetail detail, int classIdx, BigDecimal planQty) {
        switch (classIdx) {
            case 1: detail.setClass1PlanQty(planQty); break;
            case 2: detail.setClass2PlanQty(planQty); break;
            case 3: detail.setClass3PlanQty(planQty); break;
            case 4: detail.setClass4PlanQty(planQty); break;
            case 5: detail.setClass5PlanQty(planQty); break;
            case 6: detail.setClass6PlanQty(planQty); break;
            case 7: detail.setClass7PlanQty(planQty); break;
            case 8: detail.setClass8PlanQty(planQty); break;
        }
    }

    private BigDecimal getClassFinishQty(CxScheduleResult main, int classIdx) {
        switch (classIdx) {
            case 1: return main.getClass1FinishQty();
            case 2: return main.getClass2FinishQty();
            case 3: return main.getClass3FinishQty();
            case 4: return main.getClass4FinishQty();
            case 5: return main.getClass5FinishQty();
            case 6: return main.getClass6FinishQty();
            case 7: return main.getClass7FinishQty();
            case 8: return main.getClass8FinishQty();
            default: return null;
        }
    }

    private Integer getLhClassFinishQty(LhScheduleResult lh, int classIdx) {
        switch (classIdx) {
            case 1: return lh.getClass1FinishQty();
            case 2: return lh.getClass2FinishQty();
            case 3: return lh.getClass3FinishQty();
            case 4: return lh.getClass4FinishQty();
            case 5: return lh.getClass5FinishQty();
            case 6: return lh.getClass6FinishQty();
            case 7: return lh.getClass7FinishQty();
            case 8: return lh.getClass8FinishQty();
            default: return null;
        }
    }

    private Integer getLhClassPlanQty(LhScheduleResult lh, int classIdx) {
        switch (classIdx) {
            case 1: return lh.getClass1PlanQty();
            case 2: return lh.getClass2PlanQty();
            case 3: return lh.getClass3PlanQty();
            case 4: return lh.getClass4PlanQty();
            case 5: return lh.getClass5PlanQty();
            case 6: return lh.getClass6PlanQty();
            case 7: return lh.getClass7PlanQty();
            case 8: return lh.getClass8PlanQty();
            default: return null;
        }
    }

    private String getParamValue(String paramCode) {
        try {
            CxParamConfig config = paramConfigMapper.selectOne(
                    new LambdaQueryWrapper<CxParamConfig>()
                            .eq(CxParamConfig::getParamCode, paramCode)
                            .eq(CxParamConfig::getIsActive, 1));
            if (config != null && StringUtils.hasText(config.getParamValue())) {
                return config.getParamValue().trim();
            }
        } catch (Exception e) {
            log.warn("读取参数 {} 失败：{}", paramCode, e.getMessage());
        }
        return null;
    }

    /**
     * 从班次字段名解析索引
     */
    private int getClassIndex(String classField) {
        if (classField != null && classField.startsWith("CLASS")) {
            try {
                return Integer.parseInt(classField.substring(5));
            } catch (NumberFormatException e) {
                log.warn("无法解析班次字段: {}", classField);
            }
        }
        return 0;
    }

    /**
     * 从数据库参数表获取整型配置值
     */
    private int getIntParamValue(String paramCode, int defaultValue) {
        try {
            CxParamConfig config = paramConfigMapper.selectOne(
                    new LambdaQueryWrapper<CxParamConfig>()
                            .eq(CxParamConfig::getParamCode, paramCode)
                            .eq(CxParamConfig::getIsActive, 1));
            if (config != null && StringUtils.hasText(config.getParamValue())) {
                return Integer.parseInt(config.getParamValue().trim());
            }
        } catch (Exception e) {
            log.warn("读取参数 {} 失败，使用默认值 {}：{}", paramCode, defaultValue, e.getMessage());
        }
        return defaultValue;
    }

    /**
     * 胎胚库存时长内部类
     */
    private static class EmbryoStockHours {
        private Long mainId;
        private String machineCode;
        private String embryoCode;
        private String materialCode;
        private BigDecimal stockHours;
        private BigDecimal planQty;
        private int tripCapacity;
        private int realtimeStock;
        private double remainingHours;

        public Long getMainId() { return mainId; }
        public void setMainId(Long mainId) { this.mainId = mainId; }
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
        public String getEmbryoCode() { return embryoCode; }
        public void setEmbryoCode(String embryoCode) { this.embryoCode = embryoCode; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public BigDecimal getStockHours() { return stockHours; }
        public void setStockHours(BigDecimal stockHours) { this.stockHours = stockHours; }
        public BigDecimal getPlanQty() { return planQty; }
        public void setPlanQty(BigDecimal planQty) { this.planQty = planQty; }
        public int getTripCapacity() { return tripCapacity; }
        public void setTripCapacity(int tripCapacity) { this.tripCapacity = tripCapacity; }
        public int getRealtimeStock() { return realtimeStock; }
        public void setRealtimeStock(int realtimeStock) { this.realtimeStock = realtimeStock; }
        public double getRemainingHours() { return remainingHours; }
        public void setRemainingHours(double remainingHours) { this.remainingHours = remainingHours; }
    }

    // ==================== 机台维度滚动重排程 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleAdjustResultVo rescheduleByMachine(String factoryCode, String scheduleDateStr,
                                                      String triggerShiftClass, String machineCode) {
        ScheduleAdjustResultVo result = new ScheduleAdjustResultVo();
        result.setSuccess(true);

        LocalDate scheduleDate = LocalDate.parse(scheduleDateStr, DATE_FMT);
        int triggerIdx = getClassIndex(triggerShiftClass);
        if (triggerIdx < 1 || triggerIdx > 7) {
            result.setSuccess(false);
            result.setMessage("触发班次只能为CLASS1~CLASS7，当前：" + triggerShiftClass);
            return result;
        }

        log.info("[机台滚动重排] factory={}, date={}, triggerClass={} (index={}), machine={}",
                factoryCode, scheduleDateStr, triggerShiftClass, triggerIdx, machineCode);

        RescheduleContext ctx = new RescheduleContext();
        ctx.scheduleDate = scheduleDate;
        ctx.factoryCode = factoryCode;
        ctx.machineCode = machineCode;
        ctx.triggerIdx = triggerIdx;

        loadRescheduleData(ctx);

        if (CollectionUtils.isEmpty(ctx.machineMainResults)) {
            result.setSuccess(false);
            result.setMessage("未找到机台 " + machineCode + " 在 " + scheduleDateStr + " 的排程数据");
            return result;
        }

        int nextIdx = triggerIdx + 1;
        int adjustClassIndex = nextIdx;

        applyFinishedShiftsToContext(ctx);

        restoreMachineOnlineEmbryos(ctx);

        adjustNextShiftByStockHours(ctx, result, adjustClassIndex);

        if (nextIdx < 8) {
            rescheduleRemainingShifts(ctx, result, nextIdx);
        }

        result.setMessage("机台滚动重排完成");
        log.info("[机台滚动重排] 完成, added={}, removed={}",
                result.getAddedTrips().size(), result.getRemovedTrips().size());
        return result;
    }

    // ==================== 内部上下文类 ====================

    private static class RescheduleContext {
        LocalDate scheduleDate;
        String factoryCode;
        String machineCode;
        int triggerIdx;

        List<CxShiftConfig> sortedShiftConfigs;
        Map<String, CxShiftConfig> classConfigMap;

        List<CxScheduleResult> machineMainResults;
        Map<Long, List<CxScheduleDetail>> detailsByMainId;
        Map<Long, CxScheduleResult> mainById;

        List<LhScheduleResult> lhScheduleResults;

        BigDecimal lossRate;
        String dayVulcanizationMode;

        Map<String, MdmMaterialInfo> materialInfoMap;
        Map<String, MdmWorkCalendar> calendarMap;
        Map<String, Integer> dailyLhCapacityMap;
        Map<String, String> embryoMaterialMap;

        Map<String, Integer> currentStockMap;
        Map<String, BigDecimal> formingRemainderMap;

        Map<String, Integer> tripCapacityMap;
        Map<String, Set<String>> structureMachineMap;

        Set<String> keyProductEmbryos;
        Set<String> mainProductEmbryos;

        Map<String, LocalDate> materialEndingMap;

        Map<String, Integer> structureLhMachineCount;
        Map<String, Integer> structureMoldCount;

        List<String> onlineEmbryos;

        Map<String, List<String>> materialEmbryosMap;
    }

    // ==================== 数据加载 ====================

    private void loadRescheduleData(RescheduleContext ctx) {
        loadShiftConfigs(ctx);
        loadScheduleResults(ctx);
        loadLhScheduleResults(ctx);
        loadParams(ctx);
        loadWorkCalendar(ctx);
        loadMaterialInfo(ctx);
        loadStocks(ctx);
        loadFormingRemainder(ctx);
        loadStructureAllocations(ctx);
        loadTripCapacity(ctx);
        loadKeyProducts(ctx);
        loadMainProducts(ctx);
        loadEndingDates(ctx);
        loadStructureLhConfig(ctx);
        buildMaterialEmbryoMapping(ctx);
    }

    private void loadShiftConfigs(RescheduleContext ctx) {
        List<CxShiftConfig> allConfigs = shiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getFactoryCode, ctx.factoryCode)
                        .orderByAsc(CxShiftConfig::getScheduleDay)
                        .orderByAsc(CxShiftConfig::getDayShiftOrder));
        ctx.sortedShiftConfigs = allConfigs;
        ctx.classConfigMap = new LinkedHashMap<>();
        for (CxShiftConfig sc : allConfigs) {
            String classField = "CLASS" + sc.getScheduleDay() + "_" + sc.getDayShiftOrder();
            ctx.classConfigMap.put(classField, sc);
        }
        log.info("[数据加载] 班次配置: {} 条", allConfigs.size());
    }

    private void loadScheduleResults(RescheduleContext ctx) {
        List<CxScheduleResult> allResults = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getFactoryCode, ctx.factoryCode)
                        .eq(CxScheduleResult::getScheduleDate, ctx.scheduleDate));
        ctx.machineMainResults = allResults.stream()
                .filter(r -> ctx.machineCode.equals(r.getCxMachineCode()))
                .collect(Collectors.toList());

        ctx.mainById = new HashMap<>();
        for (CxScheduleResult r : allResults) {
            ctx.mainById.put(r.getId(), r);
        }

        if (!CollectionUtils.isEmpty(ctx.machineMainResults)) {
            List<Long> mainIds = ctx.machineMainResults.stream()
                    .map(CxScheduleResult::getId).collect(Collectors.toList());
            List<CxScheduleDetail> allDetails = scheduleDetailMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleDetail>()
                            .in(CxScheduleDetail::getMainId, mainIds)
                            .orderByAsc(CxScheduleDetail::getMainId)
                            .orderByAsc(CxScheduleDetail::getTripNo));
            ctx.detailsByMainId = allDetails.stream()
                    .collect(Collectors.groupingBy(CxScheduleDetail::getMainId));
        } else {
            ctx.detailsByMainId = new HashMap<>();
        }
        log.info("[数据加载] 机台主表: {} 条, 子表分组: {} 组",
                ctx.machineMainResults.size(), ctx.detailsByMainId.size());
    }

    private void loadLhScheduleResults(RescheduleContext ctx) {
        ctx.lhScheduleResults = lhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<LhScheduleResult>()
                        .eq(LhScheduleResult::getFactoryCode, ctx.factoryCode)
                        .eq(LhScheduleResult::getScheduleDate,
                                java.sql.Date.valueOf(ctx.scheduleDate)));
        log.info("[数据加载] 硫化任务: {} 条", ctx.lhScheduleResults.size());
    }

    private void loadParams(RescheduleContext ctx) {
        String lossRateStr = getParamValue("SYS04020001");
        ctx.lossRate = StringUtils.hasText(lossRateStr)
                ? new BigDecimal(lossRateStr) : BigDecimal.ZERO;

        ctx.dayVulcanizationMode = getParamValue("SYS04010001");
        if (!StringUtils.hasText(ctx.dayVulcanizationMode)) {
            ctx.dayVulcanizationMode = "2";
        }
        log.info("[数据加载] 损耗率: {}, 日硫化量模式: {}", ctx.lossRate, ctx.dayVulcanizationMode);
    }

    private void loadWorkCalendar(RescheduleContext ctx) {
        List<MdmWorkCalendar> calendars = workCalendarMapper.selectList(
                new LambdaQueryWrapper<MdmWorkCalendar>()
                        .eq(MdmWorkCalendar::getProcCode, "CX"));
        ctx.calendarMap = new HashMap<>();
        if (calendars != null) {
            for (MdmWorkCalendar cal : calendars) {
                if (cal.getProductionDate() != null) {
                    java.time.LocalDate localDate = new java.sql.Date(
                            cal.getProductionDate().getTime()).toLocalDate();
                    String dateStr = localDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    ctx.calendarMap.put(dateStr, cal);
                }
            }
        }
    }

    private void loadMaterialInfo(RescheduleContext ctx) {
        ctx.materialInfoMap = new HashMap<>();
        ctx.dailyLhCapacityMap = new HashMap<>();
        ctx.embryoMaterialMap = new HashMap<>();

        List<MdmMaterialInfo> materials = materialInfoMapper.selectList(
                new LambdaQueryWrapper<MdmMaterialInfo>()
                        .eq(MdmMaterialInfo::getFactoryCode, ctx.factoryCode));
        for (MdmMaterialInfo m : materials) {
            ctx.materialInfoMap.put(m.getMaterialCode(), m);
            ctx.embryoMaterialMap.put(m.getEmbryoCode(), m.getMaterialCode());
        }

        List<MonthPlanProductLhCapacityVo> capacities = lhCapacityMapper.selectByFactoryCode(
                ctx.factoryCode);
        for (MonthPlanProductLhCapacityVo cap : capacities) {
            Integer val = selectLhCapacityByMode(cap, ctx.dayVulcanizationMode);
            if (val != null && val > 0) {
                ctx.dailyLhCapacityMap.put(cap.getMaterialCode(), val);
            }
        }
        log.info("[数据加载] 物料: {} 条, 日硫化量: {} 条",
                ctx.materialInfoMap.size(), ctx.dailyLhCapacityMap.size());
    }

    private Integer selectLhCapacityByMode(MonthPlanProductLhCapacityVo cap, String mode) {
        switch (mode) {
            case "1": return cap.getMesCapacity();
            case "3": return cap.getApsCapacity();
            default: return cap.getStandardCapacity();
        }
    }

    private void loadStocks(RescheduleContext ctx) {
        ctx.currentStockMap = new HashMap<>();
        if (stockMapper == null) {
            log.warn("[数据加载] stockMapper 不存在，跳过库存加载");
            return;
        }
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>());
        for (CxStock stock : stocks) {
            if (stock.getEmbryoCode() != null && stock.getStockNum() != null) {
                int current = ctx.currentStockMap.getOrDefault(stock.getEmbryoCode(), 0);
                ctx.currentStockMap.put(stock.getEmbryoCode(), current + stock.getStockNum());
            }
        }
        log.info("[数据加载] 库存: {} 条", ctx.currentStockMap.size());
    }

    private void loadFormingRemainder(RescheduleContext ctx) {
        ctx.formingRemainderMap = new HashMap<>();
        try {
            LocalDate date = ctx.scheduleDate;
            List<MdmMonthSurplus> surpluses = monthSurplusMapper.selectList(
                    new LambdaQueryWrapper<MdmMonthSurplus>()
                            .eq(MdmMonthSurplus::getFactoryCode, ctx.factoryCode)
                            .eq(MdmMonthSurplus::getYear, BigDecimal.valueOf(date.getYear()))
                            .eq(MdmMonthSurplus::getMonth, BigDecimal.valueOf(date.getMonthValue())));
            for (MdmMonthSurplus s : surpluses) {
                BigDecimal remainder = s.getPlanSurplusQty();
                if (remainder == null) {
                    remainder = BigDecimal.ZERO;
                }
                String materialCode = s.getMaterialCode();
                if (materialCode != null) {
                    ctx.formingRemainderMap.merge(materialCode, remainder, BigDecimal::add);
                }
            }
        } catch (Exception e) {
            log.warn("[数据加载] 成型余量加载失败：{}", e.getMessage());
        }
        log.info("[数据加载] 成型余量: {} 条", ctx.formingRemainderMap.size());
    }

    private void loadStructureAllocations(RescheduleContext ctx) {
        ctx.structureMachineMap = new HashMap<>();
        int year = ctx.scheduleDate.getYear();
        int month = ctx.scheduleDate.getMonthValue();
        List<MpCxCapacityConfiguration> configs = capacityConfigMapper.selectByYearAndMonth(
                ctx.factoryCode, year, month);
        for (MpCxCapacityConfiguration c : configs) {
            ctx.structureMachineMap
                    .computeIfAbsent(c.getStructureName(), k -> new HashSet<>())
                    .add(c.getCxMachineCode());
        }
        log.info("[数据加载] 结构分配: {} 条", configs.size());
    }

    private void loadTripCapacity(RescheduleContext ctx) {
        ctx.tripCapacityMap = new HashMap<>();
        List<CxStructureTreadConfig> configs = structureTreadConfigMapper.selectList(
                new LambdaQueryWrapper<CxStructureTreadConfig>()
                        .eq(CxStructureTreadConfig::getFactoryCode, ctx.factoryCode));
        for (CxStructureTreadConfig c : configs) {
            if (c.getTreadCount() != null && c.getTreadCount() > 0) {
                ctx.tripCapacityMap.put(c.getEmbryoCode(), c.getTreadCount());
            }
        }
        log.info("[数据加载] 胎胚容量: {} 条", ctx.tripCapacityMap.size());
    }

    private void loadKeyProducts(RescheduleContext ctx) {
        ctx.keyProductEmbryos = new HashSet<>();
        List<CxKeyProduct> products = keyProductMapper.selectList(
                new LambdaQueryWrapper<>());
        for (CxKeyProduct p : products) {
            ctx.keyProductEmbryos.add(p.getEmbryoCode());
        }
        log.info("[数据加载] 关键产品: {} 个", ctx.keyProductEmbryos.size());
    }

    private void loadMainProducts(RescheduleContext ctx) {
        ctx.mainProductEmbryos = new HashSet<>();
        List<MdmSkuScheduleCategory> categories = skuScheduleCategoryMapper.selectList(
                new LambdaQueryWrapper<MdmSkuScheduleCategory>()
                        .eq(MdmSkuScheduleCategory::getScheduleType, "01"));
        for (MdmSkuScheduleCategory c : categories) {
            ctx.mainProductEmbryos.add(c.getMaterialCode());
        }
        log.info("[数据加载] 主产品(SCHEDULE_TYPE=01): {} 个", ctx.mainProductEmbryos.size());
    }

    private void loadEndingDates(RescheduleContext ctx) {
        ctx.materialEndingMap = new HashMap<>();
        List<CxMaterialEnding> endings = materialEndingMapper.selectList(
                new LambdaQueryWrapper<CxMaterialEnding>()
                        .eq(CxMaterialEnding::getFactoryCode, ctx.factoryCode));
        for (CxMaterialEnding e : endings) {
            if (e.getPlannedEndingDate() != null) {
                ctx.materialEndingMap.put(e.getMaterialCode(),
                        e.getPlannedEndingDate());
            }
        }
        log.info("[数据加载] 收尾日期: {} 条", ctx.materialEndingMap.size());
    }

    private void loadStructureLhConfig(RescheduleContext ctx) {
        ctx.structureLhMachineCount = new HashMap<>();
        ctx.structureMoldCount = new HashMap<>();
        List<MdmStructureLhRatio> ratios = structureLhRatioMapper.selectList(
                new LambdaQueryWrapper<MdmStructureLhRatio>()
                        .eq(MdmStructureLhRatio::getFactoryCode, ctx.factoryCode));
        for (MdmStructureLhRatio r : ratios) {
            if (r.getLhMachineMaxQty() != null && r.getLhMachineMaxQty() > 0) {
                ctx.structureLhMachineCount.put(r.getStructureName(), r.getLhMachineMaxQty());
            }
        }
    }

    private void buildMaterialEmbryoMapping(RescheduleContext ctx) {
        ctx.materialEmbryosMap = new HashMap<>();
        for (LhScheduleResult lh : ctx.lhScheduleResults) {
            String mat = lh.getMaterialCode();
            String emb = lh.getEmbryoCode();
            ctx.materialEmbryosMap.computeIfAbsent(mat, k -> new ArrayList<>()).add(emb);
        }
    }

    // ==================== Phase A: 应用已完成班次 FINISH_QTY ====================

    private void applyFinishedShiftsToContext(RescheduleContext ctx) {
        for (int classIdx = 1; classIdx <= ctx.triggerIdx; classIdx++) {
            applySingleFinishedShift(ctx, classIdx);
        }
        log.info("[Phase A] 已完成班次 1~{} 的应用完成, 库存样本: {}",
                ctx.triggerIdx, sampleStock(ctx));
    }

    private void applySingleFinishedShift(RescheduleContext ctx, int classIdx) {
        Map<String, Integer> formingFinishByMaterial = new HashMap<>();
        for (CxScheduleResult main : ctx.machineMainResults) {
            BigDecimal finish = getClassFinishQty(main, classIdx);
            if (finish == null || finish.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String mat = main.getMaterialCode();
            formingFinishByMaterial.merge(mat, finish.intValue(), Integer::sum);
        }

        Map<String, Integer> vulcanizeFinishByMaterial = new HashMap<>();
        for (LhScheduleResult lh : ctx.lhScheduleResults) {
            Integer finish = getLhClassFinishQty(lh, classIdx);
            if (finish == null || finish <= 0) {
                continue;
            }
            String mat = lh.getMaterialCode();
            vulcanizeFinishByMaterial.merge(mat, finish, Integer::sum);
        }

        Set<String> allMaterials = new HashSet<>();
        allMaterials.addAll(formingFinishByMaterial.keySet());
        allMaterials.addAll(vulcanizeFinishByMaterial.keySet());

        for (String mat : allMaterials) {
            int forming = formingFinishByMaterial.getOrDefault(mat, 0);
            int vulcanize = vulcanizeFinishByMaterial.getOrDefault(mat, 0);

            int oldStock = ctx.currentStockMap.getOrDefault(mat, 0);
            int newStock = oldStock + forming - vulcanize;
            ctx.currentStockMap.put(mat, Math.max(0, newStock));

            BigDecimal oldRemainder = ctx.formingRemainderMap.getOrDefault(mat, BigDecimal.ZERO);
            BigDecimal newRemainder = oldRemainder.subtract(BigDecimal.valueOf(forming));
            ctx.formingRemainderMap.put(mat, newRemainder.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO : newRemainder);
        }
    }

    // ==================== 恢复机台-在线胎胚映射 ====================

    private void restoreMachineOnlineEmbryos(RescheduleContext ctx) {
        ctx.onlineEmbryos = new ArrayList<>();
        if (ctx.triggerIdx < 1) {
            return;
        }
        Set<String> embryoSet = new LinkedHashSet<>();
        int classIdx = ctx.triggerIdx;
        if (ctx.detailsByMainId != null) {
            for (List<CxScheduleDetail> details : ctx.detailsByMainId.values()) {
                for (CxScheduleDetail detail : details) {
                    BigDecimal planQty = getDetailClassPlanQty(detail, classIdx);
                    if (planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0) {
                        embryoSet.add(detail.getEmbryoCode());
                    }
                }
            }
        }
        ctx.onlineEmbryos = new ArrayList<>(embryoSet);
        log.info("[在线胎胚] 触发器前最后班次={}, 在线胎胚: {}", classIdx, ctx.onlineEmbryos);
    }

    // ==================== Phase B: 调整下一班次(补1车/减1车) ====================

    private void adjustNextShiftByStockHours(RescheduleContext ctx, ScheduleAdjustResultVo result,
                                             int adjustClassIdx) {
        List<EmbryoStockItem> items = buildStockItemsForAdjust(ctx, adjustClassIdx);
        if (CollectionUtils.isEmpty(items)) {
            log.info("[Phase B] 无需调整的胎胚 (adjustClass={})", adjustClassIdx);
            return;
        }

        EmbryoStockItem needAdd = null;
        EmbryoStockItem needSubtract = null;

        for (EmbryoStockItem item : items) {
            if (item.stockHours < stockHoursThreshold) {
                needAdd = item;
                break;
            }
        }

        if (needAdd != null) {
            for (int i = items.size() - 1; i >= 0; i--) {
                EmbryoStockItem item = items.get(i);
                if (!item.embryoCode.equals(needAdd.embryoCode)
                        && item.planQty > 0) {
                    needSubtract = item;
                    break;
                }
            }
        }

        if (needAdd == null) {
            log.info("[Phase B] 所有胎胚库存时长均 >= {} 小时，无需补车", stockHoursThreshold);
            return;
        }
        if (needSubtract == null) {
            log.info("[Phase B] 需要补车但无可减车胎胚，跳过调整");
            return;
        }

        int tripCap = needAdd.tripCapacity > 0 ? needAdd.tripCapacity : 1;

        doAddVehicle(ctx, adjustClassIdx, needAdd, tripCap, result);
        doSubtractVehicle(ctx, adjustClassIdx, needSubtract, tripCap, result);

        recalculateMainTableFromDetails(ctx);
        log.info("[Phase B] 调整完成: add={}(+{}), sub={}(-{})",
                needAdd.embryoCode, tripCap, needSubtract.embryoCode, tripCap);
    }

    private List<EmbryoStockItem> buildStockItemsForAdjust(RescheduleContext ctx, int classIdx) {
        List<EmbryoStockItem> items = new ArrayList<>();

        for (CxScheduleResult main : ctx.machineMainResults) {
            BigDecimal planQtyBd = getClassPlanQty(main, classIdx);
            int planQty = planQtyBd != null ? planQtyBd.intValue() : 0;
            if (planQty <= 0) {
                continue;
            }

            String embryoCode = main.getEmbryoCode();
            String materialCode = main.getMaterialCode();
            String structureName = main.getStructureName();

            if (shouldSkipAdjustment(ctx, main, embryoCode, materialCode, classIdx)) {
                continue;
            }

            int currentStock = ctx.currentStockMap.getOrDefault(materialCode, 0);
            Integer dailyLhCapacity = ctx.dailyLhCapacityMap.get(materialCode);
            if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
                continue;
            }

            int lhMachineCount = ctx.structureLhMachineCount.getOrDefault(structureName, 1);
            int moldCount = getStructureMoldCount(ctx, materialCode);

            double stockHours = calculateStockHoursValue(currentStock, lhMachineCount,
                    moldCount, dailyLhCapacity);

            int tripCapacity = ctx.tripCapacityMap.getOrDefault(embryoCode, 1);

            EmbryoStockItem item = new EmbryoStockItem();
            item.mainId = main.getId();
            item.embryoCode = embryoCode;
            item.materialCode = materialCode;
            item.structureName = structureName;
            item.stockHours = stockHours;
            item.planQty = planQty;
            item.tripCapacity = tripCapacity;
            item.currentStock = currentStock;
            items.add(item);
        }

        items.sort(Comparator.comparingDouble((EmbryoStockItem i) -> i.stockHours));
        return items;
    }

    private boolean shouldSkipAdjustment(RescheduleContext ctx, CxScheduleResult main,
                                         String embryoCode, String materialCode, int classIdx) {
        if (isEndingTask(ctx, main, materialCode, classIdx)) {
            return true;
        }
        if (isClosedShiftTask(ctx, classIdx)) {
            return true;
        }
        if (ctx.keyProductEmbryos.contains(embryoCode) && isOpenStartShift(ctx, classIdx)) {
            return true;
        }
        return false;
    }

    private boolean isEndingTask(RescheduleContext ctx, CxScheduleResult main,
                                 String materialCode, int classIdx) {
        LocalDate endingDate = ctx.materialEndingMap.get(materialCode);
        if (endingDate == null) {
            return false;
        }
        if (endingDate.isBefore(ctx.scheduleDate)) {
            return true;
        }

        BigDecimal formingRemainder = ctx.formingRemainderMap.getOrDefault(materialCode, BigDecimal.ZERO);
        if (formingRemainder.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        int vulcanizeDemand = 0;
        for (int i = classIdx; i <= 8; i++) {
            for (LhScheduleResult lh : ctx.lhScheduleResults) {
                if (materialCode.equals(lh.getMaterialCode())) {
                    vulcanizeDemand += getLhClassPlanQty(lh, i);
                }
            }
        }
        if (vulcanizeDemand <= 0) {
            return true;
        }
        return false;
    }

    private boolean isClosedShiftTask(RescheduleContext ctx, int classIdx) {
        CxShiftConfig sc = getShiftConfigByClassOrder(ctx, classIdx);
        if (sc == null || ctx.calendarMap == null) {
            return false;
        }
        Integer scheduleDay = sc.getScheduleDay();
        Integer dayShiftOrder = sc.getDayShiftOrder();
        if (scheduleDay == null || dayShiftOrder == null) {
            return false;
        }
        String dateStr = getDateForScheduleDay(ctx.scheduleDate, scheduleDay);
        MdmWorkCalendar calendar = ctx.calendarMap.get(dateStr);
        if (calendar == null) {
            return false;
        }
        return "0".equals(getCalendarDayShiftFlag(calendar, dayShiftOrder));
    }

    private String getDateForScheduleDay(LocalDate scheduleDate, int scheduleDay) {
        if (scheduleDay == 1) return scheduleDate.format(DATE_FMT);
        return scheduleDate.plusDays(scheduleDay - 1).format(DATE_FMT);
    }

    private String getCalendarDayShiftFlag(MdmWorkCalendar calendar, Integer dayShiftOrder) {
        if (dayShiftOrder == null) return null;
        switch (dayShiftOrder) {
            case 1: return calendar.getOneShiftFlag();
            case 2: return calendar.getTwoShiftFlag();
            case 3: return calendar.getThreeShiftFlag();
            default: return null;
        }
    }

    private boolean isOpenStartShift(RescheduleContext ctx, int classIdx) {
        CxShiftConfig sc = getShiftConfigByClassOrder(ctx, classIdx);
        if (sc == null || ctx.calendarMap == null) {
            return false;
        }
        Integer scheduleDay = sc.getScheduleDay();
        Integer dayShiftOrder = sc.getDayShiftOrder();
        if (scheduleDay == null || dayShiftOrder == null) {
            return false;
        }
        String dateStr = getDateForScheduleDay(ctx.scheduleDate, scheduleDay);
        MdmWorkCalendar calendar = ctx.calendarMap.get(dateStr);
        if (calendar == null) {
            return false;
        }
        String currentFlag = getCalendarDayShiftFlag(calendar, dayShiftOrder);
        if (!"1".equals(currentFlag)) {
            return false;
        }
        int prevOrder = getShiftConfigOrder(ctx, sc) - 1;
        for (CxShiftConfig prev : ctx.sortedShiftConfigs) {
            if (getShiftConfigOrder(ctx, prev) == prevOrder) {
                Integer prevDay = prev.getScheduleDay();
                Integer prevOrder2 = prev.getDayShiftOrder();
                if (prevDay == null || prevOrder2 == null) {
                    return false;
                }
                String prevDateStr = getDateForScheduleDay(ctx.scheduleDate, prevDay);
                MdmWorkCalendar prevCalendar = ctx.calendarMap.get(prevDateStr);
                if (prevCalendar == null) {
                    return false;
                }
                return "0".equals(getCalendarDayShiftFlag(prevCalendar, prevOrder2));
            }
        }
        return false;
    }

    private double calculateStockHoursValue(int currentStock, int lhMachineCount,
                                            int moldCount, int dailyLhCapacity) {
        if (dailyLhCapacity <= 0 || lhMachineCount <= 0 || moldCount <= 0) {
            return Double.MAX_VALUE;
        }
        double singleTireSeconds = 86400.0 / dailyLhCapacity;
        return (double) currentStock * singleTireSeconds / 3600.0 / lhMachineCount / moldCount;
    }

    private int getStructureMoldCount(RescheduleContext ctx, String materialCode) {
        for (LhScheduleResult lh : ctx.lhScheduleResults) {
            if (materialCode.equals(lh.getMaterialCode()) && lh.getMouldQty() != null) {
                return lh.getMouldQty();
            }
        }
        return 1;
    }

    private void doAddVehicle(RescheduleContext ctx, int classIdx,
                              EmbryoStockItem item, int qty, ScheduleAdjustResultVo result) {
        List<CxScheduleDetail> details = ctx.detailsByMainId.get(item.mainId);
        if (CollectionUtils.isEmpty(details)) {
            return;
        }
        CxScheduleDetail lastDetail = details.get(details.size() - 1);
        BigDecimal oldPlan = getDetailClassPlanQty(lastDetail, classIdx);
        BigDecimal oldPlanSafe = oldPlan != null ? oldPlan : BigDecimal.ZERO;
        setDetailClassPlanQty(lastDetail, classIdx, oldPlanSafe.add(BigDecimal.valueOf(qty)));

        scheduleDetailMapper.updateById(lastDetail);

        addTripAdjustItem(result, item.machineCode, item.embryoCode, item.materialCode,
                classIdx, lastDetail.getTripNo(), oldPlanSafe.intValue(),
                oldPlanSafe.add(BigDecimal.valueOf(qty)).intValue(), item.stockHours);
    }

    private void doSubtractVehicle(RescheduleContext ctx, int classIdx,
                                   EmbryoStockItem item, int qty, ScheduleAdjustResultVo result) {
        List<CxScheduleDetail> details = ctx.detailsByMainId.get(item.mainId);
        if (CollectionUtils.isEmpty(details)) {
            return;
        }
        BigDecimal remaining = BigDecimal.valueOf(qty);
        for (CxScheduleDetail detail : details) {
            BigDecimal oldPlan = getDetailClassPlanQty(detail, classIdx);
            BigDecimal oldPlanSafe = oldPlan != null ? oldPlan : BigDecimal.ZERO;
            if (oldPlanSafe.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal subtract = oldPlanSafe.min(remaining);
            setDetailClassPlanQty(detail, classIdx, oldPlanSafe.subtract(subtract));
            scheduleDetailMapper.updateById(detail);
            remaining = remaining.subtract(subtract);

            addTripAdjustItem(result, item.machineCode, item.embryoCode, item.materialCode,
                    classIdx, detail.getTripNo(), oldPlanSafe.intValue(),
                    oldPlanSafe.subtract(subtract).intValue(), item.stockHours);

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
    }

    private void recalculateMainTableFromDetails(RescheduleContext ctx) {
        for (CxScheduleResult main : ctx.machineMainResults) {
            List<CxScheduleDetail> details = ctx.detailsByMainId.get(main.getId());
            if (CollectionUtils.isEmpty(details)) {
                continue;
            }
            recalculateMainRowFromDetails(main, details);
            scheduleResultMapper.updateById(main);
        }
    }

    private void recalculateMainRowFromDetails(CxScheduleResult main, List<CxScheduleDetail> details) {
        for (int i = 1; i <= 8; i++) {
            BigDecimal total = BigDecimal.ZERO;
            for (CxScheduleDetail d : details) {
                BigDecimal planQty = getDetailClassPlanQty(d, i);
                if (planQty != null) {
                    total = total.add(planQty);
                }
            }
            setClassPlanQty(main, i, total.compareTo(BigDecimal.ZERO) > 0 ? total : BigDecimal.ZERO);
        }
    }

    // ==================== Phase C: 重排剩余班次 ====================

    private void rescheduleRemainingShifts(RescheduleContext ctx, ScheduleAdjustResultVo result,
                                           int adjustClassIdx) {
        int startShift = adjustClassIdx + 1;
        if (startShift > 8) {
            log.info("[Phase C] 无剩余班次需要重排 (start={})", startShift);
            return;
        }

        Map<String, EmbryoReschedulePlan> embryoPlans = buildEmbryoReschedulePlans(ctx);

        Map<String, BigDecimal> materialUsedFormingRemainder = new HashMap<>();

        for (int classIdx = startShift; classIdx <= 8; classIdx++) {
            log.info("[Phase C] 重排班次: CLASS{}", classIdx);
            rescheduleSingleShift(ctx, classIdx, embryoPlans, materialUsedFormingRemainder);
            materialUsedFormingRemainder.clear();
        }

        clearUnusedShiftPlans(ctx, startShift);

        recalculateMainTableFromDetails(ctx);
        log.info("[Phase C] 重排完成: shifts={}~8", startShift);
    }

    private Map<String, EmbryoReschedulePlan> buildEmbryoReschedulePlans(RescheduleContext ctx) {
        Map<String, EmbryoReschedulePlan> plans = new LinkedHashMap<>();

        for (CxScheduleResult main : ctx.machineMainResults) {
            String embryoCode = main.getEmbryoCode();
            String materialCode = main.getMaterialCode();
            String structureName = main.getStructureName();

            if (!canEmbryoRunOnMachine(ctx, structureName)) {
                continue;
            }

            EmbryoReschedulePlan plan = plans.get(embryoCode);
            if (plan == null) {
                plan = new EmbryoReschedulePlan();
                plan.mainId = main.getId();
                plan.embryoCode = embryoCode;
                plan.materialCode = materialCode;
                plan.structureName = structureName;
                plan.tripCapacity = ctx.tripCapacityMap.getOrDefault(embryoCode, 1);
                plan.lhMachineCount = ctx.structureLhMachineCount.getOrDefault(structureName, 1);
                plan.moldCount = getStructureMoldCount(ctx, materialCode);
                plan.dailyLhCapacity = ctx.dailyLhCapacityMap.getOrDefault(materialCode, 0);
                plans.put(embryoCode, plan);
            }
        }

        for (EmbryoReschedulePlan plan : plans.values()) {
            plan.isMainProduct = ctx.mainProductEmbryos.contains(plan.materialCode);
            plan.isEndingTask = computeIsEndingTask(ctx, plan);
        }

        return plans;
    }

    private boolean computeIsEndingTask(RescheduleContext ctx, EmbryoReschedulePlan plan) {
        BigDecimal formingRemainder = ctx.formingRemainderMap.getOrDefault(
                plan.materialCode, BigDecimal.ZERO);
        if (formingRemainder.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        LocalDate endingDate = ctx.materialEndingMap.get(plan.materialCode);
        if (endingDate != null && endingDate.isBefore(ctx.scheduleDate)) {
            return true;
        }

        return false;
    }

    private int handleEndingLastBatch(RescheduleContext ctx, EmbryoReschedulePlan plan,
                                      int actualRemaining) {
        plan.isLastEndingBatch = true;

        if (!plan.isMainProduct && actualRemaining <= DEFAULT_ENDING_DISCARD_THRESHOLD) {
            plan.endingAbandoned = true;
            plan.endingAbandonedQty = actualRemaining;
            log.info("[Phase C 收尾] 非主销舍弃 material={}, qty={}", plan.materialCode, actualRemaining);
            return 0;
        }

        if (!plan.isMainProduct) {
            int prod = applyLossRate(actualRemaining, ctx.lossRate);
            log.info("[Phase C 收尾] 非主销按实量 material={}, actualRemaining={}, plannedProd={}",
                    plan.materialCode, actualRemaining, prod);
            return prod;
        }

        int base = calculatePlannedProduction(actualRemaining, ctx.lossRate, plan.tripCapacity);
        int result = Math.max(base, plan.tripCapacity);
        log.info("[Phase C 收尾] 主销补整车 material={}, actualRemaining={}, result={}",
                plan.materialCode, actualRemaining, result);
        return result;
    }

    private int applyLossRate(int quantity, BigDecimal lossRate) {
        if (lossRate.compareTo(BigDecimal.ZERO) <= 0) {
            return quantity;
        }
        BigDecimal divisor = BigDecimal.ONE.subtract(
                lossRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return quantity;
        }
        return BigDecimal.valueOf(quantity).divide(divisor, 0, RoundingMode.UP).intValue();
    }

    private boolean canEmbryoRunOnMachine(RescheduleContext ctx, String structureName) {
        Set<String> machines = ctx.structureMachineMap.get(structureName);
        return machines != null && machines.contains(ctx.machineCode);
    }

    private void rescheduleSingleShift(RescheduleContext ctx, int classIdx,
                                       Map<String, EmbryoReschedulePlan> embryoPlans,
                                       Map<String, BigDecimal> materialUsedFormingRemainder) {
        CxShiftConfig shiftConfig = getShiftConfigByClassOrder(ctx, classIdx);
        if (shiftConfig == null) {
            log.info("[Phase C] 班次 {} 无配置，跳过", classIdx);
            return;
        }
        if (isClosedShiftTask(ctx, classIdx)) {
            log.info("[Phase C] 班次 {} 停产，跳过", classIdx);
            return;
        }

        int shiftCapacity = getShiftMaxCapacity(shiftConfig);
        if (shiftCapacity <= 0) {
            log.info("[Phase C] 班次 {} 产能为0，跳过", classIdx);
            return;
        }

        List<ShiftAllocationItem> allocations = new ArrayList<>();

        for (EmbryoReschedulePlan plan : embryoPlans.values()) {
            plan.classPlanQty = 0;
        }

        List<EmbryoReschedulePlan> sortedPlans = sortPlansByPriority(embryoPlans, classIdx);

        BigDecimal usedForming = materialUsedFormingRemainder
                .getOrDefault("__shift__", BigDecimal.ZERO);

        for (EmbryoReschedulePlan plan : sortedPlans) {
            if (usedForming.intValue() >= shiftCapacity) {
                break;
            }

            int netDemand = calculateNetDemand(ctx, plan, classIdx, materialUsedFormingRemainder);
            if (netDemand <= 0) {
                continue;
            }

            int plannedProd;
            boolean skipVehicleRounding = false;

            if (plan.isEndingTask && !plan.isLastEndingBatch) {
                BigDecimal totalForming = ctx.formingRemainderMap.getOrDefault(
                        plan.materialCode, BigDecimal.ZERO);
                BigDecimal usedInShift = materialUsedFormingRemainder.getOrDefault(
                        plan.materialCode, BigDecimal.ZERO);
                int actualRemaining = totalForming.subtract(usedInShift).intValue();

                if (actualRemaining > 0 && netDemand >= actualRemaining) {
                    plannedProd = handleEndingLastBatch(ctx, plan, actualRemaining);
                    if (plannedProd <= 0) {
                        continue;
                    }
                    if (!plan.isMainProduct) {
                        skipVehicleRounding = true;
                    }
                } else {
                    plannedProd = calculatePlannedProduction(netDemand, ctx.lossRate, plan.tripCapacity);
                }
            } else {
                plannedProd = calculatePlannedProduction(netDemand, ctx.lossRate, plan.tripCapacity);
            }

            if (plannedProd <= 0) {
                continue;
            }

            int remaining = shiftCapacity - usedForming.intValue();
            int allocated = Math.min(plannedProd, remaining);

            if (!skipVehicleRounding) {
                if (allocated < plan.tripCapacity && allocated > 0) {
                    allocated = 0;
                }
                if (allocated > 0) {
                    int vehicles = allocated / plan.tripCapacity;
                    allocated = vehicles * plan.tripCapacity;
                }
            }

            if (allocated <= 0) {
                continue;
            }

            plan.classPlanQty = allocated;
            plan.classAllocatedQty = allocated;
            usedForming = usedForming.add(BigDecimal.valueOf(allocated));

            materialUsedFormingRemainder.merge(plan.materialCode,
                    BigDecimal.valueOf(allocated), BigDecimal::add);

            ShiftAllocationItem alloc = new ShiftAllocationItem();
            alloc.mainId = plan.mainId;
            alloc.embryoCode = plan.embryoCode;
            alloc.materialCode = plan.materialCode;
            alloc.allocatedQty = allocated;
            alloc.tripCapacity = plan.tripCapacity;
            allocations.add(alloc);
        }

        materialUsedFormingRemainder.put("__shift__", usedForming);

        applyPhaseCAllocationsToDetail(ctx, classIdx, allocations);

        updateContextAfterPhaseCShift(ctx, allocations, classIdx, embryoPlans);
    }

    private CxShiftConfig getShiftConfigByClassOrder(RescheduleContext ctx, int classOrder) {
        for (CxShiftConfig sc : ctx.sortedShiftConfigs) {
            Integer order = getShiftClassOrder(sc);
            if (order != null && order == classOrder) {
                return sc;
            }
        }
        return null;
    }

    private Integer getShiftClassOrder(CxShiftConfig sc) {
        int scheduleDay = sc.getScheduleDay();
        int dayShiftOrder = sc.getDayShiftOrder();
        switch (scheduleDay) {
            case 1: return dayShiftOrder == 1 ? 1 : (dayShiftOrder == 2 ? 2 : (dayShiftOrder == 3 ? 3 : null));
            case 2: return dayShiftOrder == 1 ? 4 : (dayShiftOrder == 2 ? 5 : (dayShiftOrder == 3 ? 6 : null));
            case 3: return dayShiftOrder == 1 ? 7 : (dayShiftOrder == 2 ? 8 : null);
            default: return null;
        }
    }

    private int getShiftMaxCapacity(CxShiftConfig shiftConfig) {
        Integer shiftHours = shiftConfig.getShiftHours();
        return shiftHours != null ? shiftHours : 0;
    }

    private List<EmbryoReschedulePlan> sortPlansByPriority(
            Map<String, EmbryoReschedulePlan> plans, int classIdx) {
        List<EmbryoReschedulePlan> list = new ArrayList<>(plans.values());
        list.sort((a, b) -> {
            int machineCompare = Integer.compare(
                    b.lhMachineCount, a.lhMachineCount);
            if (machineCompare != 0) return machineCompare;

            double stockA = calculateStockHoursValue(
                    a.currentStock, a.lhMachineCount, a.moldCount, a.dailyLhCapacity);
            double stockB = calculateStockHoursValue(
                    b.currentStock, b.lhMachineCount, b.moldCount, b.dailyLhCapacity);
            return Double.compare(stockA, stockB);
        });
        return list;
    }

    private int calculateNetDemand(RescheduleContext ctx, EmbryoReschedulePlan plan,
                                   int classIdx, Map<String, BigDecimal> materialUsedFormingRemainder) {
        int vulcanizeDemand = 0;
        for (LhScheduleResult lh : ctx.lhScheduleResults) {
            if (plan.materialCode.equals(lh.getMaterialCode())) {
                vulcanizeDemand += getLhClassPlanQty(lh, classIdx);
            }
        }

        int currentStock = ctx.currentStockMap.getOrDefault(plan.materialCode, 0);
        int netDemand = Math.max(0, vulcanizeDemand - currentStock);

        BigDecimal formingRemainder = ctx.formingRemainderMap.getOrDefault(
                plan.materialCode, BigDecimal.ZERO);
        BigDecimal used = materialUsedFormingRemainder.getOrDefault(
                plan.materialCode, BigDecimal.ZERO);
        BigDecimal available = formingRemainder.subtract(used);
        int availableInt = available.compareTo(BigDecimal.ZERO) > 0
                ? available.intValue() : 0;

        netDemand = Math.min(netDemand, availableInt);
        return netDemand;
    }

    private int calculatePlannedProduction(int netDemand, BigDecimal lossRate, int tripCapacity) {
        if (lossRate.compareTo(BigDecimal.ZERO) <= 0) {
            return netDemand;
        }
        BigDecimal divisor = BigDecimal.ONE.subtract(
                lossRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return netDemand;
        }
        BigDecimal production = BigDecimal.valueOf(netDemand).divide(divisor, 0, RoundingMode.UP);
        int prodInt = production.intValue();
        if (tripCapacity > 0) {
            int vehicles = (int) Math.ceil((double) prodInt / tripCapacity);
            return vehicles * tripCapacity;
        }
        return prodInt;
    }

    private void applyPhaseCAllocationsToDetail(RescheduleContext ctx, int classIdx,
                                                List<ShiftAllocationItem> allocations) {
        for (ShiftAllocationItem alloc : allocations) {
            List<CxScheduleDetail> details = ctx.detailsByMainId.get(alloc.mainId);
            if (CollectionUtils.isEmpty(details)) {
                continue;
            }

            int remaining = alloc.allocatedQty;
            for (int i = 0; i < details.size() && remaining > 0; i++) {
                CxScheduleDetail detail = details.get(i);
                int tripCap = detail.getTripCapacity() != null
                        ? detail.getTripCapacity().intValue() : alloc.tripCapacity;
                if (tripCap <= 0) tripCap = alloc.tripCapacity;
                if (remaining >= tripCap) {
                    setDetailClassPlanQty(detail, classIdx, BigDecimal.valueOf(tripCap));
                    scheduleDetailMapper.updateById(detail);
                    remaining -= tripCap;
                }
            }
        }
    }

    private void updateContextAfterPhaseCShift(RescheduleContext ctx,
                                               List<ShiftAllocationItem> allocations,
                                               int classIdx,
                                               Map<String, EmbryoReschedulePlan> embryoPlans) {
        for (ShiftAllocationItem alloc : allocations) {
            int oldStock = ctx.currentStockMap.getOrDefault(alloc.materialCode, 0);
            int newStock = oldStock + alloc.allocatedQty;
            ctx.currentStockMap.put(alloc.materialCode, newStock);

            BigDecimal oldRemainder = ctx.formingRemainderMap.getOrDefault(
                    alloc.materialCode, BigDecimal.ZERO);
            BigDecimal newRemainder = oldRemainder.subtract(
                    BigDecimal.valueOf(alloc.allocatedQty));
            ctx.formingRemainderMap.put(alloc.materialCode,
                    newRemainder.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newRemainder);
        }

        for (EmbryoReschedulePlan plan : embryoPlans.values()) {
            int stock = ctx.currentStockMap.getOrDefault(plan.materialCode, 0);
            plan.currentStock = stock;
        }
    }

    private void clearUnusedShiftPlans(RescheduleContext ctx, int startShift) {
        for (List<CxScheduleDetail> details : ctx.detailsByMainId.values()) {
            for (CxScheduleDetail detail : details) {
                boolean changed = false;
                for (int i = startShift; i <= 8; i++) {
                    BigDecimal existing = getDetailClassPlanQty(detail, i);
                    if (existing != null && existing.compareTo(BigDecimal.ZERO) > 0) {
                        setDetailClassPlanQty(detail, i, BigDecimal.ZERO);
                        changed = true;
                    }
                }
                if (changed) {
                    scheduleDetailMapper.updateById(detail);
                }
            }
        }
    }

    private void addTripAdjustItem(ScheduleAdjustResultVo result, String machineCode,
                                   String embryoCode, String materialCode, int classIdx,
                                   String tripNo, int beforeQty, int afterQty, double stockHours) {
        ScheduleAdjustResultVo.TripAdjustItem item = new ScheduleAdjustResultVo.TripAdjustItem();
        item.setMachineCode(machineCode);
        item.setEmbryoCode(embryoCode);
        item.setMaterialCode(materialCode);
        item.setShiftClass("CLASS" + classIdx);
        item.setTripNo(tripNo != null ? Integer.valueOf(tripNo) : null);
        item.setBeforePlanQty(beforeQty);
        item.setAfterPlanQty(afterQty);
        item.setStockHours(BigDecimal.valueOf(stockHours));
        if (afterQty > beforeQty) {
            result.getAddedTrips().add(item);
        } else {
            result.getRemovedTrips().add(item);
        }
    }

    /**
     * Phase C 内部类 - 胎胚重排计划
     */
    private static class EmbryoReschedulePlan {
        Long mainId;
        String embryoCode;
        String materialCode;
        String structureName;
        int tripCapacity;
        int lhMachineCount;
        int moldCount;
        int dailyLhCapacity;
        int currentStock;
        int classAllocatedQty;
        int classPlanQty;

        boolean isEndingTask;
        boolean isMainProduct;
        boolean isLastEndingBatch;
        boolean endingAbandoned;
        int endingAbandonedQty;
    }

    /**
     * Phase C 内部类 - 班次分配项
     */
    private static class ShiftAllocationItem {
        Long mainId;
        String embryoCode;
        String materialCode;
        int allocatedQty;
        int tripCapacity;
    }

    /**
     * Phase B 内部类 - 胎胚库存项
     */
    private static class EmbryoStockItem {
        Long mainId;
        String embryoCode;
        String materialCode;
        String structureName;
        String machineCode;
        double stockHours;
        int planQty;
        int tripCapacity;
        int currentStock;
    }

    private int getShiftConfigOrder(RescheduleContext ctx, CxShiftConfig sc) {
        for (int i = 0; i < ctx.sortedShiftConfigs.size(); i++) {
            if (ctx.sortedShiftConfigs.get(i) == sc) {
                return i + 1;
            }
        }
        return 0;
    }

    private String sampleStock(RescheduleContext ctx) {
        if (ctx.currentStockMap.isEmpty()) return "empty";
        Map.Entry<String, Integer> first = ctx.currentStockMap.entrySet().iterator().next();
        return first.getKey() + "=" + first.getValue();
    }
}
