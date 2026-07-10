package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MpCxCapacityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** 默认整车容量 */
    public static final int DEFAULT_TRIP_CAPACITY = 12;

    /** 默认机台种类上限（被 BalancingService / TrialTaskProcessor 等引用） */
    public static final int DEFAULT_MAX_TYPES_PER_MACHINE = 4;

    /** 默认机台最大硫化机数（配比缺失时单台最多生产的硫化机数） */
    public static final int DEFAULT_MAX_LH_MACHINE_QTY = 10;

    /** 一天总秒数 */
    public static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** 秒转小时的除数 */
    public static final int SECONDS_PER_HOUR = 3600;

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
                : DEFAULT_TRIP_CAPACITY;
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
        return BigDecimal.valueOf(SECONDS_PER_DAY)
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
                .divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP);
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
        return BigDecimal.valueOf(SECONDS_PER_DAY)
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
                .multiply(BigDecimal.valueOf(SECONDS_PER_HOUR))
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
            total += (maxLh != null ? maxLh : DEFAULT_MAX_LH_MACHINE_QTY);
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
}
