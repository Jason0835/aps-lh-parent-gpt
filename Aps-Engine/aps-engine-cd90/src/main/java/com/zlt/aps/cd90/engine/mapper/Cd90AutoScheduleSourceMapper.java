package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

/**
 * 将各业务模块实体转换为直裁自动排程窄模型。
 */
@Component
public class Cd90AutoScheduleSourceMapper {

    /**
     * 转换成型排程，并保持CLASS1至CLASS8的固定顺序。
     *
     * @param source 成型排程实体
     * @return 成型排程窄模型
     */
    public Cd90FormingScheduleSource mapFormingSchedule(CxScheduleResult source) {
        return Cd90FormingScheduleSource.builder()
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
    public Cd90StockSource mapStock(Cd90Stock source) {
        BigDecimal quantity = decimal(source.getStockNum())
                .add(decimal(source.getModifyNum()))
                .subtract(decimal(source.getBadNum()));
        return Cd90StockSource.builder()
                .stockDate(toLocalDate(source.getStockDate()))
                .clothCode(source.getMaterialCode())
                .stockQuantity(quantity)
                .build();
    }

    /**
     * 转换库排状态。即使车数为0,也保留MES同步的帘布代号,供库排严格按帘布匹配。
     * <p>
     * MAX_CAR_NUM 必填(2026/06/24 变更):不同库排可不同,不再兜底推算;为空或非正时直接抛异常,
     * 由批次级数据先行检查在排程前拦截。
     * </p>
     *
     * @param source 库排限制实体
     * @return 库排状态
     */
    public Cd90StorageLaneState mapStorageLane(Cd90StorageLaneLimit source) {
        int vehicleCount = source.getCarNum() == null ? 0 : source.getCarNum();
        Integer maxCarNum = source.getMaxCarNum();
        if (maxCarNum == null || maxCarNum <= 0) {
            throw new IllegalArgumentException(
                    "库排 " + source.getStorageLaneCode() + " 未维护有效最大车数");
        }
        return Cd90StorageLaneState.builder()
                .laneCode(source.getStorageLaneCode())
                .clothCode(source.getMaterialCode())
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
}
