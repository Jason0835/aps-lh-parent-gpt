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
                .classPlanQuantities(Arrays.asList(
                        zeroIfNull(source.getClass1PlanQty()),
                        zeroIfNull(source.getClass2PlanQty()),
                        zeroIfNull(source.getClass3PlanQty()),
                        zeroIfNull(source.getClass4PlanQty()),
                        zeroIfNull(source.getClass5PlanQty()),
                        zeroIfNull(source.getClass6PlanQty()),
                        zeroIfNull(source.getClass7PlanQty()),
                        zeroIfNull(source.getClass8PlanQty())))
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
     * 转换库排状态。车数为0时物料占用必须清空，避免空库排被误判为专用库排。
     *
     * @param source 库排限制实体
     * @return 库排状态
     */
    public Cd90StorageLaneState mapStorageLane(Cd90StorageLaneLimit source) {
        int vehicleCount = source.getCarNum() == null ? 0 : source.getCarNum();
        int maxVehicleCount = source.getMaxCarNum() == null
                ? vehicleCount + (source.getAvailableCarNum() == null ? 0 : source.getAvailableCarNum())
                : source.getMaxCarNum();
        return Cd90StorageLaneState.builder()
                .laneCode(source.getStorageLaneCode())
                .clothCode(vehicleCount > 0 ? source.getMaterialCode() : null)
                .vehicleCount(vehicleCount)
                .maxVehicleCount(maxVehicleCount)
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
