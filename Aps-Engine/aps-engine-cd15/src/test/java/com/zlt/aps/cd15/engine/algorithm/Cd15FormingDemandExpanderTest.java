package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * 成型宽表转钢带逐班需求明细测试。
 */
public class Cd15FormingDemandExpanderTest {

    private final Cd15FormingDemandExpander expander = new Cd15FormingDemandExpander();

    /**
     * CLASS1从成型排程日前一天早班开始，后续字段按8小时自然展开。
     */
    @Test
    public void shouldExpandClassFieldsIntoNaturalShifts() {
        Cd15FormingScheduleSource schedule = schedule("EM001", "10", "20", "30");
        Cd15ConstructionMaterial material = material("EM001", "CF001", "500");

        List<Cd15DemandShift> result = expander.expand(
                Collections.singletonList(schedule), Collections.singletonList(material));

        assertEquals(8, result.size());
        assertEquals(LocalDateTime.of(2026, 6, 12, 6, 0), result.get(0).getStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 22, 0), result.get(2).getStartTime());
        assertEquals("CLASS3", result.get(2).getClassField());
        assertEquals(new BigDecimal("15"), result.get(2).getSteelStripDemandQuantity());
    }

    /**
     * 同一钢带来自多个胎胚或多个施工层位时，按自然班次累计条数和米数。
     */
    @Test
    public void shouldAggregateSameMaterialByNaturalShift() {
        List<Cd15FormingScheduleSource> schedules = Arrays.asList(
                schedule("EM001", "10"),
                schedule("EM002", "20"));
        List<Cd15ConstructionMaterial> materials = Arrays.asList(
                material("EM001", "CF001", "500"),
                material("EM002", "CF001", "500"));

        List<Cd15DemandShift> result = expander.expand(schedules, materials);

        assertEquals(8, result.size());
        assertEquals("CF001", result.get(0).getSteelStripCode());
        assertEquals(new BigDecimal("30"), result.get(0).getFormingQuantity());
        assertEquals(new BigDecimal("15"), result.get(0).getSteelStripDemandQuantity());
    }

    /**
     * 同一胎胚存在多个施工版本时，每个成型CLASS按自己的recipe号匹配施工版本。
     */
    @Test
    public void shouldMatchConstructionVersionByClassRecipeNo() {
        Cd15FormingScheduleSource schedule = scheduleWithRecipes("EM001",
                new String[]{"10", "10", "10"},
                new String[]{"V1", "V2", null});
        List<Cd15ConstructionMaterial> materials = Arrays.asList(
                material("EM001", "V1", "CF001", "500"),
                material("EM001", "V2", "CF001", "900"));

        List<Cd15DemandShift> result = expander.expand(
                Collections.singletonList(schedule), materials);

        assertEquals(2, result.size());
        assertEquals("CLASS1", result.get(0).getClassField());
        assertEquals(new BigDecimal("5"), result.get(0).getSteelStripDemandQuantity());
        assertEquals("CLASS2", result.get(1).getClassField());
        assertEquals(new BigDecimal("9"), result.get(1).getSteelStripDemandQuantity());
    }

    /**
     * 同一钢带和大卷的不同裁断角度必须形成独立材料身份，不能合并需求。
     */
    @Test
    public void shouldKeepFifteenEighteenTwentyFourAndFiftyOneDegreeMaterialsSeparate() {
        Cd15FormingScheduleSource schedule = schedule("EM001", "10");
        List<Cd15ConstructionMaterial> materials = Arrays.asList(
                angleMaterial("15"), angleMaterial("18"),
                angleMaterial("24"), angleMaterial("51"));

        List<Cd15DemandShift> classOne = expander.expand(
                        Collections.singletonList(schedule), materials).stream()
                .filter(item -> "CLASS1".equals(item.getClassField()))
                .collect(Collectors.toList());

        assertEquals(4, classOne.size());
        assertEquals(Arrays.asList("15", "18", "24", "51"),
                classOne.stream().map(Cd15DemandShift::getCuttingAngle)
                        .collect(Collectors.toList()));
        assertEquals(4L, classOne.stream()
                .map(Cd15DemandShift::getMaterialKey).distinct().count());
    }
    /**
     * 主钢带层与左右层材料身份相同时必须按同一钢带需求累计。
     */
    @Test
    public void shouldAggregateNumberedAndLeftRightLayersByMaterialIdentity() {
        Cd15ConstructionMaterial numberedLayer = angleMaterial("15");
        numberedLayer.setLayerNo(1);
        Cd15ConstructionMaterial leftLayer = angleMaterial("15");
        leftLayer.setLayerNo(101);
        Cd15ConstructionMaterial rightLayer = angleMaterial("15");
        rightLayer.setLayerNo(102);

        List<Cd15DemandShift> classOne = expander.expand(
                        Collections.singletonList(schedule("EM001", "10")),
                        Arrays.asList(numberedLayer, leftLayer, rightLayer)).stream()
                .filter(item -> "CLASS1".equals(item.getClassField()))
                .collect(Collectors.toList());

        assertEquals(1, classOne.size());
        assertEquals(new BigDecimal("30"), classOne.get(0).getFormingQuantity());
        assertEquals(new BigDecimal("2.61"),
                classOne.get(0).getSteelStripDemandQuantity());
    }


    /**
     * 0计划量班次仍占用自然窗口位置，并标记为停产班次。
     */
    @Test
    public void shouldKeepZeroQuantityClassFields() {
        List<Cd15DemandShift> result = expander.expand(
                Collections.singletonList(schedule("EM001", "0")),
                Collections.singletonList(material("EM001", "CF001", "500")));

        assertEquals(8, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).getSteelStripDemandQuantity());
        assertEquals(true, result.get(0).isStopped());
    }

    private Cd15FormingScheduleSource schedule(String embryoCode, String... quantities) {
        String[] recipeNos = new String[8];
        Arrays.fill(recipeNos, "V1");
        return scheduleWithRecipes(embryoCode, quantities, recipeNos);
    }

    private Cd15FormingScheduleSource scheduleWithRecipes(String embryoCode,
                                                          String[] quantities,
                                                          String[] recipeNos) {
        BigDecimal[] values = new BigDecimal[8];
        Arrays.fill(values, BigDecimal.ZERO);
        for (int index = 0; index < quantities.length; index++) {
            values[index] = new BigDecimal(quantities[index]);
        }
        String[] recipes = new String[8];
        if (recipeNos != null) {
            System.arraycopy(recipeNos, 0, recipes, 0, Math.min(recipeNos.length, recipes.length));
        }
        return Cd15FormingScheduleSource.builder()
                .cxBatchNo("CX001")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .embryoCode(embryoCode)
                .classPlanQuantities(Arrays.asList(values))
                .classRecipeNos(Arrays.asList(recipes))
                .build();
    }

    private Cd15ConstructionMaterial angleMaterial(String cuttingAngle) {
        return Cd15ConstructionMaterial.builder()
                .constructionCode("EM001")
                .constructionVersion("V1")
                .steelStripCode("CF001")
                .bigRollCode("BR001")
                .cuttingAngle(cuttingAngle)
                .layerNo(1)
                .unitConsumeMillimeter(new BigDecimal("87"))
                .craftWidth(new BigDecimal("260"))
                .curlLength(new BigDecimal("80"))
                .build();
    }

    private Cd15ConstructionMaterial material(String constructionCode,
                                               String steelStripCode,
                                               String unitConsumeMillimeter) {
        return material(constructionCode, "V1", steelStripCode, unitConsumeMillimeter);
    }

    private Cd15ConstructionMaterial material(String constructionCode,
                                               String constructionVersion,
                                               String steelStripCode,
                                               String unitConsumeMillimeter) {
        return Cd15ConstructionMaterial.builder()
                .constructionCode(constructionCode)
                .constructionVersion(constructionVersion)
                .steelStripCode(steelStripCode)
                .layerNo(1)
                .unitConsumeMillimeter(new BigDecimal(unitConsumeMillimeter))
                .build();
    }
}
