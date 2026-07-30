package com.zlt.aps.cd15.component;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Cd15ScheduleResultExportAssemblerTest {

    private final Cd15ScheduleResultExportAssembler assembler =
            new Cd15ScheduleResultExportAssembler();

    @Test
    void assembleRowsMergesFourDisplayedShiftsAndKeepsAngleDimension() {
        Cd15ScheduleResult previous = schedule(
                "G1101", "DLC13001_2#", "CSS24524", "18");
        previous.setClass3PlanQty(100D);
        previous.setClass3FinishQty(90D);

        Cd15ScheduleResult current = schedule(
                "G1101", "DLC13001_2#", "CSS24524", "18");
        current.setUnitConsumeMillimeter(new BigDecimal("3124"));
        current.setPlanSurplusQty(new BigDecimal("12"));
        current.setStorageLaneCode("G11-6,G11-19");
        current.setCxMachineCodes("H1101,H1205");
        current.setClass1PlanQty(200D);
        current.setClass1FinishQty(180D);
        current.setClass2PlanQty(300D);
        current.setClass2FinishQty(280D);
        current.setClass3PlanQty(400D);
        current.setClass3FinishQty(0D);

        Cd15ScheduleResult otherAngle = schedule(
                "G1101", "DLC13001_2#", "CSS24524", "24");
        otherAngle.setClass1PlanQty(50D);

        List<Map<String, Object>> rows = this.assembler.assembleRows(
                Collections.singletonList(previous),
                Arrays.asList(current, otherAngle),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());

        assertEquals(2, rows.size());
        Map<String, Object> row = find(rows, "18");
        assertEquals(new BigDecimal("100.0"),
                row.get("previousClass3PlanQty"));
        assertEquals(new BigDecimal("200.0"), row.get("class1PlanQty"));
        assertEquals(new BigDecimal("300.0"), row.get("class2PlanQty"));
        assertEquals(new BigDecimal("400.0"), row.get("class3PlanQty"));
        assertEquals(new BigDecimal("0.0"), row.get("class3FinishQty"));
        assertEquals(new BigDecimal("3.124"), row.get("unitConsume"));
        assertEquals(new BigDecimal("1000.0"),
                row.get("fourShiftPlanQty"));
        assertEquals("18", row.get("cuttingAngle"));
    }

    @Test
    void assembleRowsAggregatesStockAndExactConstructionMatches() {
        Cd15ScheduleResult current = schedule(
                "G1101", "DLC13001_2#", "CSS24524", "18");
        current.setClass1PlanQty(100D);

        Cd15Stock stock = new Cd15Stock();
        stock.setMaterialCode("DLC13001_2#");
        stock.setStockNum(500D);
        stock.setModifyNum(20D);
        stock.setBadNum(5D);

        CxScheduleResult forming = new CxScheduleResult();
        forming.setEmbryoCode("E1");
        forming.setClass1RecipeNo("V1");
        forming.setClass1PlanQty(new BigDecimal("800"));
        forming.setClass2RecipeNo("MISSING");
        forming.setClass2PlanQty(new BigDecimal("100"));

        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("E1");
        construction.setConstructionVersion("V1");
        construction.setBeltCode1("DLC13001_2#");
        construction.setBeltName1("385/65R22.5-JY598零度");

        List<Map<String, Object>> rows = this.assembler.assembleRows(
                Collections.emptyList(),
                Collections.singletonList(current),
                Collections.singletonList(stock),
                Collections.singletonList(forming),
                Collections.singletonList(construction));

        assertEquals(1, rows.size());
        assertEquals(new BigDecimal("515.0"), rows.get(0).get("stockQty"));
        assertEquals(new BigDecimal("800"),
                rows.get(0).get("formingPlanQty"));
        assertEquals("385/65R22.5-JY598零度",
                rows.get(0).get("structureName"));
    }

    @Test
    void assembleRowsFiltersEmptyRowsAndBuildsTemplateDates() {
        Cd15ScheduleResult empty = schedule(
                "G1101", "DLC13001_2#", "CSS24524", "18");

        assertEquals(Collections.emptyList(),
                this.assembler.assembleRows(
                        Collections.emptyList(),
                        Collections.singletonList(empty),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()));

        Map<String, Object> tableMap = this.assembler.buildTableMap(
                DateUtil.parse("2026-06-06"));
        assertEquals("2026年06月06日", tableMap.get("planDate"));
        assertEquals("06/05", tableMap.get("previousDate1"));
        assertEquals("06/06", tableMap.get("scheduleDate1"));
    }

    @Test
    void assembleRowsKeepsPreviousOnlyRows() {
        Cd15ScheduleResult previousOnly = schedule(
                "G1201", "DNC52751_1#", "CSS24524", "24");
        previousOnly.setClass3PlanQty(120D);

        Map<String, Object> row = this.assembler.assembleRows(
                Collections.singletonList(previousOnly),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()).get(0);

        assertEquals(new BigDecimal("120.0"),
                row.get("previousClass3PlanQty"));
        assertNull(row.get("class1PlanQty"));
    }

    private static Cd15ScheduleResult schedule(
            String machineCode,
            String steelStripCode,
            String bigRollCode,
            String cuttingAngle) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setMachineCode(machineCode);
        result.setSteelStripCode(steelStripCode);
        result.setBigRollCode(bigRollCode);
        result.setCuttingAngle(cuttingAngle);
        return result;
    }

    private static Map<String, Object> find(
            List<Map<String, Object>> rows, String cuttingAngle) {
        return rows.stream()
                .filter(row ->
                        cuttingAngle.equals(row.get("cuttingAngle")))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }
}
