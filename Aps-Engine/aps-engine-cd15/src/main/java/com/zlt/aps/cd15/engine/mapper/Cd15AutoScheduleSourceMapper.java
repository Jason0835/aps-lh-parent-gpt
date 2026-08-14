package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

/**
 * 将各业务模块实体转换为斜裁自动排程窄模型。
 */
@Component
public class Cd15AutoScheduleSourceMapper {

    /**
     * 转换成型排程，并保持CLASS1至CLASS8的固定顺序。
     *
     * @param source 成型排程实体
     * @return 成型排程窄模型
     */
    public Cd15FormingScheduleSource mapFormingSchedule(CxScheduleResult source) {
        return Cd15FormingScheduleSource.builder()
                .cxBatchNo(source.getCxBatchNo())
                .scheduleDate(toLocalDate(source.getScheduleDate()))
                .embryoCode(source.getEmbryoCode())
                .cxMachineCode(source.getCxMachineCode())
                .classPlanQuantities(Arrays.asList(
                        zeroIfNull(source.getClass1PlanQty()),
                        zeroIfNull(source.getClass2PlanQty()),
                        zeroIfNull(source.getClass3PlanQty()),
                        zeroIfNull(source.getClass4PlanQty()),
                        zeroIfNull(source.getClass5PlanQty()),
                        zeroIfNull(source.getClass6PlanQty()),
                        zeroIfNull(source.getClass7PlanQty()),
                        zeroIfNull(source.getClass8PlanQty())))
                .classRecipeNos(Arrays.asList(
                        source.getClass1RecipeNo(),
                        source.getClass2RecipeNo(),
                        source.getClass3RecipeNo(),
                        source.getClass4RecipeNo(),
                        source.getClass5RecipeNo(),
                        source.getClass6RecipeNo(),
                        source.getClass7RecipeNo(),
                        source.getClass8RecipeNo()))
                .build();
    }

    /**
     * 转换6点库存，可用库存等于库存量加修正量再减不良量。
     *
     * @param source 库存实体
     * @return 库存窄模型
     */
    public Cd15StockSource mapStock(Cd15Stock source) {
        BigDecimal quantity = decimal(source.getStockNum())
                .add(decimal(source.getModifyNum()))
                .subtract(decimal(source.getBadNum()));
        return Cd15StockSource.builder()
                .stockDate(toLocalDate(source.getStockDate()))
                .snapshotTime(toLocalDate(source.getStockDate()).atTime(6, 0))
                .steelStripCode(source.getMaterialCode())
                .stockQuantity(quantity)
                .build();
    }

    /**
     * 转换班次开始库存，基准时间直接使用班次开始时间。
     */
    public Cd15StockSource mapShiftStock(Cd15ShiftStock source) {
        BigDecimal quantity = decimal(source.getStockNum())
                .add(decimal(source.getModifyNum()))
                .subtract(decimal(source.getBadNum()));
        return Cd15StockSource.builder()
                .stockDate(toLocalDate(source.getStockDate()))
                .snapshotTime(toLocalDateTime(source.getShiftStartTime()))
                .steelStripCode(source.getMaterialCode())
                .stockQuantity(quantity)
                .build();
    }

    /**
     * 转换库排状态。即使车数为0,也保留MES同步的钢带代号,供库排严格按钢带匹配。
     * <p>
     * MAX_CAR_NUM 必填(2026/06/24 变更):不同库排可不同,不再兜底推算;为空或非正时直接抛异常,
     * 由批次级数据先行检查在排程前拦截。
     * </p>
     *
     * @param source 库排限制实体
     * @return 库排状态
     */
    public Cd15StorageLaneState mapStorageLane(Cd15StorageLaneLimit source) {
        int vehicleCount = source.getCarNum() == null ? 0 : source.getCarNum();
        Integer maxCarNum = source.getMaxCarNum();
        if (maxCarNum == null || maxCarNum <= 0) {
            throw new IllegalArgumentException(
                    "库排 " + source.getStorageLaneCode() + " 未维护有效最大车数");
        }
        return Cd15StorageLaneState.builder()
                .laneCode(source.getStorageLaneCode())
                .machineCode(source.getMachineCode())
                .steelStripCode(source.getMaterialCode())
                .vehicleCount(vehicleCount)
                .maxVehicleCount(maxCarNum)
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private LocalDate toLocalDate(java.util.Date value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(java.util.Date value) {
        return value == null ? null
                : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }
}
