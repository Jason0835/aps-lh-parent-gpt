package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Cd90AutoScheduleSourceMapperTest {

    private final Cd90AutoScheduleSourceMapper mapper = new Cd90AutoScheduleSourceMapper();

    @Test
    void shouldMapEightFormingShiftQuantities() {
        CxScheduleResult source = new CxScheduleResult();
        source.setCxBatchNo("CX001");
        source.setScheduleDate(Date.valueOf(LocalDate.of(2026, 6, 13)));
        source.setEmbryoCode("EM001");
        source.setClass1PlanQty(new BigDecimal("1"));
        source.setClass1RecipeNo("V1");
        source.setClass8PlanQty(new BigDecimal("8"));
        source.setClass8RecipeNo("V8");

        Cd90FormingScheduleSource result = mapper.mapFormingSchedule(source);

        assertEquals("CX001", result.getCxBatchNo());
        assertEquals(LocalDate.of(2026, 6, 13), result.getScheduleDate());
        assertEquals(8, result.getClassPlanQuantities().size());
        assertEquals(new BigDecimal("1"), result.getClassPlanQuantities().get(0));
        assertEquals(new BigDecimal("8"), result.getClassPlanQuantities().get(7));
        assertEquals("V1", result.getClassRecipeNos().get(0));
        assertEquals("V8", result.getClassRecipeNos().get(7));
    }

    @Test
    void shouldCalculateAvailableStock() {
        Cd90Stock source = new Cd90Stock();
        source.setStockDate(Date.valueOf(LocalDate.of(2026, 6, 13)));
        source.setMaterialCode("CF001");
        source.setStockNum(100D);
        source.setModifyNum(5D);
        source.setBadNum(3D);

        Cd90StockSource result = mapper.mapStock(source);

        assertEquals("CF001", result.getClothCode());
        assertEquals(new BigDecimal("102.0"), result.getStockQuantity());
    }

    @Test
    void shouldClearMaterialWhenStorageLaneIsEmpty() {
        Cd90StorageLaneLimit source = new Cd90StorageLaneLimit();
        source.setStorageLaneCode("L01");
        source.setMaterialCode("CF001");
        source.setCarNum(0);
        source.setMaxCarNum(7);

        Cd90StorageLaneState result = mapper.mapStorageLane(source);

        assertEquals("L01", result.getLaneCode());
        assertEquals(0, result.getVehicleCount());
        assertEquals(7, result.getMaxVehicleCount());
        assertNull(result.getClothCode());
    }
}
