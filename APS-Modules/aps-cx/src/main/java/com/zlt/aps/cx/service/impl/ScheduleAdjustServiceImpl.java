package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.ScheduleAdjustService;
import com.zlt.aps.cx.vo.ScheduleAdjustResultVo;
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
     * 获取子表某班次车次容量（整车条数）
     */
    private BigDecimal getDetailClassTripCapacity(CxScheduleDetail detail, String shiftClass) {
        switch (shiftClass) {
            case "CLASS1": return detail.getClass1TripCapacity();
            case "CLASS2": return detail.getClass2TripCapacity();
            case "CLASS3": return detail.getClass3TripCapacity();
            case "CLASS4": return detail.getClass4TripCapacity();
            case "CLASS5": return detail.getClass5TripCapacity();
            case "CLASS6": return detail.getClass6TripCapacity();
            case "CLASS7": return detail.getClass7TripCapacity();
            case "CLASS8": return detail.getClass8TripCapacity();
            default: return null;
        }
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
     * 获取子表某班次车次号
     */
    private String getDetailClassTripNo(CxScheduleDetail detail, String shiftClass) {
        switch (shiftClass) {
            case "CLASS1": return detail.getClass1TripNo();
            case "CLASS2": return detail.getClass2TripNo();
            case "CLASS3": return detail.getClass3TripNo();
            case "CLASS4": return detail.getClass4TripNo();
            case "CLASS5": return detail.getClass5TripNo();
            case "CLASS6": return detail.getClass6TripNo();
            case "CLASS7": return detail.getClass7TripNo();
            case "CLASS8": return detail.getClass8TripNo();
            default: return null;
        }
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
}
