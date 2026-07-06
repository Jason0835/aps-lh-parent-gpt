package com.zlt.aps.cx.service.engine;

import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
 *   <li>{@link #calculateRequiredCars}：所需车数计算</li>
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
     * <p>不含 standardCapacity 回退逻辑。如需回退请使用 TaskGroupService 中的
     * {@code resolveSingleMoldDailyLhCapacity} 方法。
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
}
