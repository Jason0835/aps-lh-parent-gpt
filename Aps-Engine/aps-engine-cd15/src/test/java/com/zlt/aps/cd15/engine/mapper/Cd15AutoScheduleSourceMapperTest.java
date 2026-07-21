package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Cd15AutoScheduleSourceMapperTest {

    private final Cd15AutoScheduleSourceMapper mapper = new Cd15AutoScheduleSourceMapper();

    @Test
    public void shouldMapEightFormingShiftQuantities() {
        CxScheduleResult source = new CxScheduleResult();
        source.setCxBatchNo("CX001");
        source.setScheduleDate(Date.valueOf(LocalDate.of(2026, 6, 13)));
        source.setEmbryoCode("EM001");
        source.setCxMachineCode("G01");
        source.setClass1PlanQty(new BigDecimal("1"));
        source.setClass1RecipeNo("V1");
        source.setClass8PlanQty(new BigDecimal("8"));
        source.setClass8RecipeNo("V8");

        Cd15FormingScheduleSource result = mapper.mapFormingSchedule(source);

        assertEquals("CX001", result.getCxBatchNo());
        assertEquals("G01", result.getCxMachineCode());
        assertEquals(LocalDate.of(2026, 6, 13), result.getScheduleDate());
        assertEquals(8, result.getClassPlanQuantities().size());
        assertEquals(new BigDecimal("1"), result.getClassPlanQuantities().get(0));
        assertEquals(new BigDecimal("8"), result.getClassPlanQuantities().get(7));
        assertEquals("V1", result.getClassRecipeNos().get(0));
        assertEquals("V8", result.getClassRecipeNos().get(7));
    }

    @Test
    public void shouldCalculateAvailableStock() {
        Cd15Stock source = new Cd15Stock();
        source.setStockDate(Date.valueOf(LocalDate.of(2026, 6, 13)));
        source.setMaterialCode("CF001");
        source.setStockNum(100D);
        source.setModifyNum(5D);
        source.setBadNum(3D);

        Cd15StockSource result = mapper.mapStock(source);

        assertEquals("CF001", result.getSteelStripCode());
        assertEquals(new BigDecimal("102.0"), result.getStockQuantity());
    }

    @Test
    public void shouldKeepMaterialWhenStorageLaneIsEmpty() {
        Cd15StorageLaneLimit source = new Cd15StorageLaneLimit();
        source.setStorageLaneCode("L01");
        source.setMaterialCode("CF001");
        source.setCarNum(0);
        source.setMaxCarNum(7);

        Cd15StorageLaneState result = mapper.mapStorageLane(source);

        assertEquals("L01", result.getLaneCode());
        assertEquals(0, result.getVehicleCount());
        assertEquals(7, result.getMaxVehicleCount());
        assertEquals("CF001", result.getSteelStripCode());
    }
}
