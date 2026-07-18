package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/** 按钢带解析供成型机台数和备库深度测试。 */
public class Cd15SteelStripDepthResolverTest {

    private final Cd15SteelStripDepthResolver resolver = new Cd15SteelStripDepthResolver();

    @Test
    public void shouldCountDistinctFormingMachinesAndMatchDecimalDepth() {
        Map<String, BigDecimal> result = resolver.resolve(Arrays.asList(
                        schedule("G01"), schedule("G01"), schedule("G02")),
                Collections.singletonList(material()),
                Collections.singletonList(config(2, "EQ", "2.5")));

        assertEquals(new BigDecimal("2.5"), result.get("C01"));
    }

    @Test
    public void shouldResolveDifferentDepthForEachCloth() {
        Cd15ConstructionMaterial secondMaterial = Cd15ConstructionMaterial.builder()
                .constructionCode("E02")
                .constructionVersion("V2")
                .steelStripCode("C02")
                .unitConsumeMillimeter(new BigDecimal("500"))
                .build();
        Cd15FormingScheduleSource secondSchedule = Cd15FormingScheduleSource.builder()
                .cxBatchNo("CX002")
                .cxMachineCode("G02")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .embryoCode("E02")
                .classPlanQuantities(Collections.singletonList(new BigDecimal("100")))
                .classRecipeNos(Collections.singletonList("V2"))
                .build();

        Map<String, BigDecimal> result = resolver.resolve(Arrays.asList(
                        schedule("G01"), schedule("G03"), secondSchedule),
                Arrays.asList(material(), secondMaterial),
                Arrays.asList(config(1, "EQ", "1.5"), config(2, "EQ", "2.5")));

        assertEquals(new BigDecimal("2.5"), result.get("C01"));
        assertEquals(new BigDecimal("1.5"), result.get("C02"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectPositiveDemandWithoutFormingMachineCode() {
        resolver.resolve(Collections.singletonList(schedule(null)),
                Collections.singletonList(material()),
                Collections.singletonList(config(1, "EQ", "2.5")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMultipleMatchingDepthRules() {
        resolver.resolve(Arrays.asList(schedule("G01"), schedule("G02")),
                Collections.singletonList(material()),
                Arrays.asList(config(2, "EQ", "2.5"), config(1, "GE", "3")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingDepthRule() {
        resolver.resolve(Collections.singletonList(schedule("G01")),
                Collections.singletonList(material()),
                Collections.singletonList(config(2, "EQ", "2.5")));
    }

    private Cd15FormingScheduleSource schedule(String machineCode) {
        return Cd15FormingScheduleSource.builder()
                .cxBatchNo("CX001")
                .cxMachineCode(machineCode)
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .embryoCode("E01")
                .classPlanQuantities(Arrays.asList(new BigDecimal("100"), BigDecimal.ZERO))
                .classRecipeNos(Arrays.asList("V1", "V1"))
                .build();
    }

    private Cd15ConstructionMaterial material() {
        return Cd15ConstructionMaterial.builder()
                .constructionCode("E01")
                .constructionVersion("V1")
                .steelStripCode("C01")
                .unitConsumeMillimeter(new BigDecimal("500"))
                .build();
    }

    private Cd15DepthConfig config(int machineQty, String range, String depth) {
        Cd15DepthConfig config = new Cd15DepthConfig();
        config.setMachineQty(machineQty);
        config.setMachineRange(range);
        config.setDepthClassQty(new BigDecimal(depth));
        return config;
    }
}
