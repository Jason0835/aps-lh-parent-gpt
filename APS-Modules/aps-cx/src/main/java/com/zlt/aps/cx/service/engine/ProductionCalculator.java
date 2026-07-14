package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产量与产能计算工具 — 被 TaskGroupService、CoreScheduleAlgorithmServiceImpl 调用。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link #getTripCapacity}：按结构+胎胚匹配整车条数（T_CX_STRUCTURE_TREAD_CONFIG）</li>
 *   <li>{@link #roundToVehicle}：待排条数向上取整到整车</li>
 *   <li>{@link #calculateSingleTireMoldSeconds}：单胎单模硫化时长（秒）</li>
 *   <li>{@link #calculateStockHours}：库存可供硫化时长（小时）</li>
 *   <li>{@link #calculateTimePerTire}：单胎生产耗时（秒，含结构配比）</li>
 *   <li>{@link #getDoubleMoldDailyLhCapacity} / {@link #getSingleMoldDailyLhCapacity}：日硫化量获取</li>
 *   <li>{@link #resolveSingleMoldDailyLhCapacity}：单模日硫化量解析（含 standardCapacity 回退）</li>
 *   <li>{@link #calculateRequiredCars}：所需车数计算</li>
 *   <li>{@link #calculateSpaceAllowedProduction}：立库空间维度允许产量</li>
 *   <li>{@link #calculateTaskStockHours}：任务级预计班后库存可供硫化时长</li>
 *   <li>{@link #calculateMaxStockForCapHours}：时间维度封顶对应最大库存量</li>
 *   <li>{@link #calculateStructureTotalMaxLh} / {@link #calculateStructureAvgRatio}：结构产能计算</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ProductionCalculator {

    /**
     * 正常任务的整车取整：将待排条数向上取整到整车（胎面）。
     *
     * @param stripQuantity 待排条数
     * @param tripCapacity  整车条数（胎面每车条数）
     * @return 整车取整后的条数
     */
    public int roundToVehicle(int stripQuantity, int tripCapacity) {
        if (stripQuantity <= 0 || tripCapacity <= 0) {
            return 0;
        }
        int trips = (int) Math.ceil((double) stripQuantity / tripCapacity);
        return trips * tripCapacity;
    }

    /**
     * 获取整车容量（按结构+胎胚匹配 T_CX_STRUCTURE_TREAD_CONFIG）。
     */
    public int getTripCapacity(String structureName, String embryoCode, ScheduleContextVo context) {
        if (context.getStructureShiftCapacities() != null && structureName != null) {
            for (CxStructureTreadConfig capacity : context.getStructureShiftCapacities()) {
                if (structureName.equals(capacity.getStructureCode())
                        && (embryoCode == null || embryoCode.equals(capacity.getEmbryoCode()))) {
                    if (capacity.getTreadCount() != null && capacity.getTreadCount() > 0) {
                        return capacity.getTreadCount();
                    }
                }
            }
        }
        return context.getDefaultTripCapacity() != null
                ? context.getDefaultTripCapacity()
                : ScheduleConstants.DEFAULT_TRIP_CAPACITY;
    }

    // ==================== 硫化时长与产能计算 ====================

    /**
     * 计算单胎单模硫化时长（秒）。
     *
     * <p>公式：86400 / singleMoldDailyLhCapacity
     *
     * <p><b>注意</b>：参数是<b>单模</b>日硫化量（双模值已÷2）。
     * 不可用于 {@code calculateClosingRequiredStockV2}（该处使用 double 精度且含结构配比）。
     *
     * @param singleMoldDailyLhCapacity 单模日硫化量（双模值÷2）
     * @return 单胎单模硫化时长（秒），参数≤0时返回 ZERO
     */
    public BigDecimal calculateSingleTireMoldSeconds(int singleMoldDailyLhCapacity) {
        if (singleMoldDailyLhCapacity <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_DAY)
                .divide(BigDecimal.valueOf(singleMoldDailyLhCapacity), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算库存可供硫化时长（小时）。
     *
     * <p>公式：stock × (86400 / singleMoldDailyLhCapacity) / moldQty / 3600
     *
     * @param stock                     库存量（或预计班后库存）
     * @param singleMoldDailyLhCapacity 单模日硫化量（双模值÷2）
     * @param moldQty                   模数
     * @return 可供硫化时长（小时），参数无效时返回 ZERO
     */
    public BigDecimal calculateStockHours(int stock, int singleMoldDailyLhCapacity, int moldQty) {
        if (singleMoldDailyLhCapacity <= 0 || moldQty <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal singleTireMoldSeconds = calculateSingleTireMoldSeconds(singleMoldDailyLhCapacity);
        return BigDecimal.valueOf(stock)
                .multiply(singleTireMoldSeconds)
                .divide(BigDecimal.valueOf(moldQty), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算单胎生产耗时（秒，含结构配比）。
     *
     * <p>公式：86400 / (avgRatio × doubleMoldDailyLhCapacity)
     *
     * <p><b>注意</b>：参数是<b>双模</b>日硫化量（原始值，不÷2），与 {@link #calculateStockHours} 不同。
     *
     * @param avgRatio                 结构平均硫化配比
     * @param doubleMoldDailyLhCapacity 双模日硫化量（原始值）
     * @return 单胎耗时（秒），参数无效时返回 ZERO
     */
    public BigDecimal calculateTimePerTire(BigDecimal avgRatio, int doubleMoldDailyLhCapacity) {
        if (doubleMoldDailyLhCapacity <= 0
                || avgRatio == null || avgRatio.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_DAY)
                .divide(avgRatio.multiply(BigDecimal.valueOf(doubleMoldDailyLhCapacity)), 2, RoundingMode.HALF_UP);
    }

    // ==================== 日硫化量获取 ====================

    /**
     * 获取物料的双模日硫化量（原始值，不÷2）。
     *
     * <p>从 {@link ScheduleContextVo#getMaterialLhCapacityMap()} 读取 dayVulcanizationQty。
     * 不含 standardCapacity 回退逻辑。
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 双模日硫化量，无效时返回 null
     */
    public Integer getDoubleMoldDailyLhCapacity(String materialCode, ScheduleContextVo context) {
        if (context.getMaterialLhCapacityMap() == null || materialCode == null) {
            return null;
        }
        MonthPlanProductLhCapacityVo capacityVo = context.getMaterialLhCapacityMap().get(materialCode);
        if (capacityVo != null) {
            return capacityVo.getDayVulcanizationQty();
        }
        return null;
    }

    /**
     * 获取物料的单模日硫化量（双模值÷2）。
     *
     * <p>不含 standardCapacity 回退逻辑。如需回退请使用
     * {@link #resolveSingleMoldDailyLhCapacity} 方法。
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 单模日硫化量，无效时返回 0
     */
    public int getSingleMoldDailyLhCapacity(String materialCode, ScheduleContextVo context) {
        Integer doubleMold = getDoubleMoldDailyLhCapacity(materialCode, context);
        if (doubleMold != null && doubleMold > 0) {
            return doubleMold / 2;
        }
        return 0;
    }

    // ==================== 车数计算 ====================

    /**
     * 计算所需车数（向上取整）。
     *
     * @param quantity     产量
     * @param tripCapacity 整车容量
     * @return 车数，参数≤0时返回 0
     */
    public int calculateRequiredCars(int quantity, int tripCapacity) {
        if (quantity <= 0 || tripCapacity <= 0) {
            return 0;
        }
        return (quantity + tripCapacity - 1) / tripCapacity;
    }

    // ==================== 立库库容管控计算 ====================

    /**
     * 计算空间维度允许的最大产量
     *
     * <p>公式：允许产量 = max(0, 原始产量 - 超出量)，其中超出量 = 预计总库存 - 预警线
     *
     * @param originalProduction  原始计划产量
     * @param totalProjectedStock 加入本任务后的预计总库存
     * @param warehouseThreshold  库容预警线
     * @return 空间维度允许产量
     */
    public int calculateSpaceAllowedProduction(int originalProduction, int totalProjectedStock, int warehouseThreshold) {
        int overAmount = totalProjectedStock - warehouseThreshold;
        return Math.max(0, originalProduction - overAmount);
    }

    /**
     * 计算任务级预计班后库存可供硫化时长（维度二核心计算）
     *
     * <p>公式：可供硫化时长 = (分配库存 + 成型产出 - 硫化消耗) × 单胎单模时长 / 模数 / 3600
     *
     * @param materialCode    物料编码
     * @param context         排程上下文
     * @param allocatedStock  任务分配库存
     * @param production      成型产出
     * @param vulcConsumption 硫化消耗
     * @param moldQty         模数
     * @return 可供硫化时长（小时），null = 无法计算（参数缺失）
     */
    public BigDecimal calculateTaskStockHours(String materialCode, ScheduleContextVo context,
                                              int allocatedStock, int production, int vulcConsumption, int moldQty) {
        if (moldQty <= 0) {
            return null;
        }
        int singleMoldLhCap = getSingleMoldDailyLhCapacity(materialCode, context);
        if (singleMoldLhCap <= 0) {
            return null;
        }
        int projectedStock = allocatedStock + production - vulcConsumption;
        return calculateStockHours(projectedStock, singleMoldLhCap, moldQty);
    }

    /**
     * 计算时间维度封顶对应的最大库存量
     *
     * <p>公式：最大库存 = 封顶时长 × 3600 × 模数 / 单胎单模时长
     *
     * @param stockHoursCap   封顶阈值（小时）
     * @param moldQty         模数
     * @param singleMoldLhCap 单模日硫化量
     * @return 封顶时长对应的最大库存量
     */
    public int calculateMaxStockForCapHours(int stockHoursCap, int moldQty, int singleMoldLhCap) {
        BigDecimal singleTireMoldSeconds = calculateSingleTireMoldSeconds(singleMoldLhCap);
        return BigDecimal.valueOf(stockHoursCap)
                .multiply(BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR))
                .multiply(BigDecimal.valueOf(moldQty))
                .divide(singleTireMoldSeconds, 0, RoundingMode.UP)
                .intValue();
    }

    // ==================== 日硫化量解析（含回退） ====================

    /**
     * 获取物料的日硫化产能 VO
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 产能 VO，不存在时返回 null
     */
    public MonthPlanProductLhCapacityVo getMaterialLhCapacityVo(String materialCode,
                                                                ScheduleContextVo context) {
        if (context.getMaterialLhCapacityMap() == null || materialCode == null) {
            return null;
        }
        return context.getMaterialLhCapacityMap().get(materialCode);
    }

    /**
     * 解析物料的单模日硫化量（含 standardCapacity 回退）
     *
     * <p>优先使用 dayVulcanizationQty（÷2 转单模），回退到 standardCapacity。
     * 与 {@link #getSingleMoldDailyLhCapacity} 的区别：本方法含回退逻辑。
     *
     * @param materialCode 物料编码
     * @param context      排程上下文
     * @return 单模日硫化量，无效时返回 0
     */
    public int resolveSingleMoldDailyLhCapacity(String materialCode, ScheduleContextVo context) {
        MonthPlanProductLhCapacityVo vo = getMaterialLhCapacityVo(materialCode, context);
        if (vo == null) {
            return 0;
        }
        if (vo.getDayVulcanizationQty() != null && vo.getDayVulcanizationQty() > 0) {
            return vo.getDayVulcanizationQty() / 2;
        }
        if (vo.getStandardCapacity() != null && vo.getStandardCapacity() > 0) {
            return vo.getStandardCapacity();
        }
        return 0;
    }

    // ==================== 结构产能计算 ====================

    /**
     * 获取机台的硫化机数上限
     *
     * <p>从结构配比表中按 机台类型编码|结构名称 查找。
     *
     * @param machineCode   成型机台编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 硫化机数上限，未配置时返回 null
     */
    public Integer getMachineLhMaxQty(String machineCode, String structureName, ScheduleContextVo context) {
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    return lhRatio.getLhMachineMaxQty();
                }
            }
        }
        return null;
    }

    /**
     * 计算结构推荐机台的总硫化机数上限
     *
     * @param machines      推荐机台配置列表
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 总硫化机数上限
     */
    public int calculateStructureTotalMaxLh(
            List<MpCxCapacityConfiguration> machines, String structureName, ScheduleContextVo context) {
        int total = 0;
        for (MpCxCapacityConfiguration config : machines) {
            Integer maxLh = getMachineLhMaxQty(config.getCxMachineCode(), structureName, context);
            total += (maxLh != null ? maxLh : ScheduleConstants.DEFAULT_MAX_LH_MACHINE_QTY);
        }
        return total;
    }

    /**
     * 计算结构推荐机台的平均硫化配比
     *
     * @param machines      推荐机台配置列表
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 平均配比，无机台时返回 1
     */
    public BigDecimal calculateStructureAvgRatio(
            List<MpCxCapacityConfiguration> machines, String structureName, ScheduleContextVo context) {
        if (machines.isEmpty()) {
            return BigDecimal.ONE;
        }
        BigDecimal totalRatio = BigDecimal.ZERO;
        for (MpCxCapacityConfiguration config : machines) {
            Integer ratio = getMachineLhMaxQty(config.getCxMachineCode(), structureName, context);
            totalRatio = totalRatio.add(BigDecimal.valueOf(ratio != null && ratio > 0 ? ratio : 1));
        }
        return totalRatio.divide(BigDecimal.valueOf(machines.size()), 4, RoundingMode.HALF_UP);
    }

    // ==================== 班次计划量获取 ====================

    /**
     * 根据班次索引获取硫化记录的计划量
     *
     * @param lhResult   硫化记录
     * @param classIndex 班次索引（1~8）
     * @return 对应班次的计划量，无效时返回 null
     */
    public Integer getClassPlanQtyByIndex(LhScheduleResult lhResult, int classIndex) {
        switch (classIndex) {
            case 1: return lhResult.getClass1PlanQty();
            case 2: return lhResult.getClass2PlanQty();
            case 3: return lhResult.getClass3PlanQty();
            case 4: return lhResult.getClass4PlanQty();
            case 5: return lhResult.getClass5PlanQty();
            case 6: return lhResult.getClass6PlanQty();
            case 7: return lhResult.getClass7PlanQty();
            case 8: return lhResult.getClass8PlanQty();
            default: return null;
        }
    }

    // ==================== 硫化余量判断 ====================

    /**
     * 判断物料的硫化余量是否已耗尽（<=0 视为超产）
     *
     * @param materialCode    物料编码
     * @param monthSurplusMap 月度硫化余量映射
     * @return true=已耗尽（跳过该物料的库存分配）
     */
    public boolean isVulcanizeSurplusExhausted(String materialCode,
                                               Map<String, MdmMonthSurplus> monthSurplusMap) {
        if (materialCode == null || monthSurplusMap == null) {
            return false;
        }
        MdmMonthSurplus monthSurplus = monthSurplusMap.get(materialCode);
        return monthSurplus != null
                && monthSurplus.getPlanSurplusQty() != null
                && monthSurplus.getPlanSurplusQty().compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * 从 monthSurplusMap 读取物料的硫化余量数值
     *
     * @param materialCode    物料编码
     * @param monthSurplusMap 月度硫化余量映射
     * @return 硫化余量，无记录时返回 Integer.MAX_VALUE（无上限约束）
     */
    public int getVulcanizingSurplus(String materialCode,
                                     Map<String, MdmMonthSurplus> monthSurplusMap) {
        if (materialCode == null || monthSurplusMap == null) {
            return Integer.MAX_VALUE;
        }
        MdmMonthSurplus monthSurplus = monthSurplusMap.get(materialCode);
        if (monthSurplus != null && monthSurplus.getPlanSurplusQty() != null) {
            return monthSurplus.getPlanSurplusQty().intValue();
        }
        return Integer.MAX_VALUE;
    }

    // ==================== 参数解析与转换 ====================

    /**
     * 日硫化量计算模式转换（工厂参数表字母 -> 成型参数表数字）
     *
     * <p>M -> 1（MES日硫化量），S -> 2（标准日硫化量），A -> 3（APS日硫化量）
     *
     * @param mpValue 工厂参数表的原始值
     * @return 转换后的数字编码，无法识别时原样返回
     */
    public String convertDayVulcanizationMode(String mpValue) {
        if (mpValue == null) return null;
        switch (mpValue.toUpperCase().trim()) {
            case "M": return "1";
            case "S": return "2";
            case "A": return "3";
            default: return mpValue;
        }
    }

    /**
     * 解析机台最大胎胚种类数参数（格式：H15,3;H14,5）
     *
     * @param value 参数值（分号分隔，逗号分隔前缀和数值）
     * @return 机台编码前缀 -> 最大胎胚种类数
     */
    public Map<String, Integer> parseMachineMaxTypes(String value) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (value == null || value.trim().isEmpty()) return result;
        for (String seg : value.trim().split(";")) {
            String[] parts = seg.trim().split(",");
            if (parts.length == 2) {
                try {
                    String prefix = parts[0].trim();
                    int maxTypes = Integer.parseInt(parts[1].trim());
                    if (!prefix.isEmpty() && maxTypes > 0) {
                        result.put(prefix, maxTypes);
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析机台最大胎胚种类数分段失败: {}", seg);
                }
            }
        }
        return result;
    }

    /**
     * 灵活解析日期时间字符串，时间部分支持 H:mm 与 HH:mm
     *
     * @param dateTimeStr 如 "2026-05-19 5:30" 或 "2026-05-19 05:30"
     * @return 解析后的 LocalDateTime；无法解析时返回 null
     */
    public LocalDateTime parseFlexibleDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        String trimmed = dateTimeStr.trim();
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm"};
        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // try next pattern
            }
        }
        log.warn("无法解析日期时间: {}", dateTimeStr);
        return null;
    }

    // ==================== 月计划产量计算 ====================

    /**
     * 计算从当前日到目标日之间的可生产量
     *
     * <p>遍历该物料的所有月计划记录，累加 [fromDay, toDay] 区间内每天的排产量
     *
     * @param plans   该物料的月计划列表
     * @param fromDay 起始日（含）
     * @param toDay   截止日（含）
     * @return 区间内计划排产总量
     */
    public int calculateProducibleQty(List<FactoryMonthPlanProductionFinalResult> plans, int fromDay, int toDay) {
        int total = 0;
        for (FactoryMonthPlanProductionFinalResult plan : plans) {
            int planBegin = plan.getBeginDay() != null ? plan.getBeginDay() : 1;
            int planEnd = plan.getEndDay() != null ? plan.getEndDay() : 31;
            int start = Math.max(fromDay, planBegin);
            int end = Math.min(toDay, planEnd);
            for (int day = start; day <= end; day++) {
                Integer dayQty = plan.getDayQty(day);
                if (dayQty != null && dayQty > 0) {
                    total += dayQty;
                }
            }
        }
        return total;
    }

    // ==================== 班次字段解析 ====================

    /**
     * 从班次配置中解析班次索引。
     *
     * @param shiftConfig 班次配置
     * @return 班次索引 (1-8)，解析失败返回 0
     */
    public int parseClassIndex(CxShiftConfig shiftConfig) {
        if (shiftConfig == null || shiftConfig.getClassField() == null) {
            return 0;
        }
        return parseClassIndex(shiftConfig.getClassField());
    }

    /**
     * 从 CLASS 字段字符串中解析班次索引。
     *
     * @param classField 班次字段（如 "CLASS1"）
     * @return 班次索引 (1-8)，解析失败返回 0
     */
    public int parseClassIndex(String classField) {
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
     * 按班次索引设置硫化记录的计划量。
     *
     * @param lhResult   硫化记录
     * @param classIndex 班次索引（1~8）
     * @param value      计划量
     */
    public void setClassPlanQtyByIndex(LhScheduleResult lhResult, int classIndex, int value) {
        switch (classIndex) {
            case 1: lhResult.setClass1PlanQty(value); break;
            case 2: lhResult.setClass2PlanQty(value); break;
            case 3: lhResult.setClass3PlanQty(value); break;
            case 4: lhResult.setClass4PlanQty(value); break;
            case 5: lhResult.setClass5PlanQty(value); break;
            case 6: lhResult.setClass6PlanQty(value); break;
            case 7: lhResult.setClass7PlanQty(value); break;
            case 8: lhResult.setClass8PlanQty(value); break;
            default: break;
        }
    }

    /**
     * 按班次索引追加分析文本到硫化记录。
     *
     * @param lhResult   硫化记录
     * @param classIndex 班次索引（1~8）
     * @param text       分析文本
     */
    public void appendClassAnalysisByIndex(LhScheduleResult lhResult, int classIndex, String text) {
        String original;
        switch (classIndex) {
            case 1: original = lhResult.getClass1Analysis();
                lhResult.setClass1Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 2: original = lhResult.getClass2Analysis();
                lhResult.setClass2Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 3: original = lhResult.getClass3Analysis();
                lhResult.setClass3Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 4: original = lhResult.getClass4Analysis();
                lhResult.setClass4Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 5: original = lhResult.getClass5Analysis();
                lhResult.setClass5Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 6: original = lhResult.getClass6Analysis();
                lhResult.setClass6Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 7: original = lhResult.getClass7Analysis();
                lhResult.setClass7Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            case 8: original = lhResult.getClass8Analysis();
                lhResult.setClass8Analysis(original != null && !original.isEmpty() ? original + "," + text : text); break;
            default: break;
        }
    }

    // ==================== 硫化消耗计算 ====================

    /**
     * 计算单条硫化记录在当天所有班次的硫化消耗总量。
     *
     * <p>遍历当天班次配置，按 CLASS 字段获取对应计划量并累加。
     *
     * @param lhResult  硫化记录
     * @param dayShifts 当天班次配置
     * @return 硫化消耗总量
     */
    public int getVulcanizingConsumptionForDay(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts) {
        int total = 0;
        if (dayShifts == null || dayShifts.isEmpty()) {
            return total;
        }
        for (CxShiftConfig shiftConfig : dayShifts) {
            int classIndex = parseClassIndex(shiftConfig);
            if (classIndex > 0) {
                Integer planQty = getClassPlanQtyByIndex(lhResult, classIndex);
                if (planQty != null && planQty > 0) {
                    total += planQty;
                }
            }
        }
        return total;
    }

    // ==================== 机台小时产能 ====================

    /**
     * 计算机台小时产能。
     *
     * <p>公式：hourlyCapacity = 3600 / (86400 / (配比 × 日硫化量))
     *
     * <p>内部复用 {@link #calculateTimePerTire} 计算单胎耗时。
     *
     * @param machineCode   机台编码
     * @param materialCode  物料编码
     * @param structureName 结构名称
     * @param context       排程上下文
     * @return 小时产能（条/小时），无法计算时返回 12
     */
    public int calculateHourlyCapacity(String machineCode, String materialCode,
                                       String structureName, ScheduleContextVo context) {
        Integer dailyLhCapacity = getDoubleMoldDailyLhCapacity(materialCode, context);
        if (dailyLhCapacity == null || dailyLhCapacity <= 0) {
            return ScheduleConstants.DEFAULT_TRIP_CAPACITY;
        }

        int ratio = 1;
        if (context.getStructureLhRatioMap() != null && structureName != null && machineCode != null) {
            Map<String, String> machineTypeCodeMap = context.getMachineTypeCodeMap();
            String machineTypeCode = machineTypeCodeMap != null ? machineTypeCodeMap.get(machineCode) : null;
            if (machineTypeCode != null) {
                MdmStructureLhRatio lhRatio = context.getStructureLhRatioMap().get(machineTypeCode + "|" + structureName);
                if (lhRatio != null && lhRatio.getLhMachineMaxQty() != null && lhRatio.getLhMachineMaxQty() > 0) {
                    ratio = lhRatio.getLhMachineMaxQty();
                }
            }
        }

        BigDecimal timePerTire = calculateTimePerTire(BigDecimal.valueOf(ratio), dailyLhCapacity);
        if (timePerTire.compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.valueOf(ScheduleConstants.SECONDS_PER_HOUR)
                    .divide(timePerTire, 0, RoundingMode.FLOOR)
                    .intValue();
        }
        return ScheduleConstants.DEFAULT_TRIP_CAPACITY;
    }
}
