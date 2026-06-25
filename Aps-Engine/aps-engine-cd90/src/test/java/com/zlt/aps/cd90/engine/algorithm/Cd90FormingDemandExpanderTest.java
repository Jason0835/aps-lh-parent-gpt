package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 成型宽表转帘布逐班需求明细测试。
 */
public class Cd90FormingDemandExpanderTest {

    private final Cd90FormingDemandExpander expander = new Cd90FormingDemandExpander();

    /**
     * CLASS1从成型排程日前一天早班开始，后续字段按8小时自然展开。
     */
    @Test
    public void shouldExpandClassFieldsIntoNaturalShifts() {
        Cd90FormingScheduleSource schedule = schedule("EM001", "10", "20", "30");
        Cd90ConstructionMaterial material = material("EM001", "CF001", "500");

        List<Cd90DemandShift> result = expander.expand(
                Collections.singletonList(schedule), Collections.singletonList(material));

        assertEquals(8, result.size());
        assertEquals(LocalDateTime.of(2026, 6, 12, 6, 0), result.get(0).getStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 22, 0), result.get(2).getStartTime());
        assertEquals("CLASS3", result.get(2).getClassField());
        assertEquals(new BigDecimal("15"), result.get(2).getClothDemandQuantity());
    }

    /**
     * 同一帘布来自多个胎胚或多个施工层位时，按自然班次累计条数和米数。
     */
    @Test
    public void shouldAggregateSameClothByNaturalShift() {
        List<Cd90FormingScheduleSource> schedules = Arrays.asList(
                schedule("EM001", "10"),
                schedule("EM002", "20"));
        List<Cd90ConstructionMaterial> materials = Arrays.asList(
                material("EM001", "CF001", "500"),
                material("EM001", "CF001", "300"),
                material("EM002", "CF001", "1000"));

        List<Cd90DemandShift> result = expander.expand(schedules, materials);

        assertEquals(8, result.size());
        assertEquals("CF001", result.get(0).getClothCode());
        assertEquals(new BigDecimal("30"), result.get(0).getFormingQuantity());
        assertEquals(new BigDecimal("28"), result.get(0).getClothDemandQuantity());
    }

    /**
     * 同一胎胚存在多个施工版本时，每个成型CLASS按自己的recipe号匹配施工版本。
     */
    @Test
    public void shouldMatchConstructionVersionByClassRecipeNo() {
        Cd90FormingScheduleSource schedule = scheduleWithRecipes("EM001",
                new String[]{"10", "10", "10"},
                new String[]{"V1", "V2", null});
        List<Cd90ConstructionMaterial> materials = Arrays.asList(
                material("EM001", "V1", "CF001", "500"),
                material("EM001", "V2", "CF001", "900"));

        List<Cd90DemandShift> result = expander.expand(
                Collections.singletonList(schedule), materials);

        assertEquals(2, result.size());
        assertEquals("CLASS1", result.get(0).getClassField());
        assertEquals(new BigDecimal("5"), result.get(0).getClothDemandQuantity());
        assertEquals("CLASS2", result.get(1).getClassField());
        assertEquals(new BigDecimal("9"), result.get(1).getClothDemandQuantity());
    }

    /**
     * 0计划量班次仍占用自然窗口位置，并标记为停产班次。
     */
    @Test
    public void shouldKeepZeroQuantityClassFields() {
        List<Cd90DemandShift> result = expander.expand(
                Collections.singletonList(schedule("EM001", "0")),
                Collections.singletonList(material("EM001", "CF001", "500")));

        assertEquals(8, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0).getClothDemandQuantity());
        assertEquals(true, result.get(0).isStopped());
    }

    private Cd90FormingScheduleSource schedule(String embryoCode, String... quantities) {
        String[] recipeNos = new String[8];
        Arrays.fill(recipeNos, "V1");
        return scheduleWithRecipes(embryoCode, quantities, recipeNos);
    }

    private Cd90FormingScheduleSource scheduleWithRecipes(String embryoCode,
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
        return Cd90FormingScheduleSource.builder()
                .cxBatchNo("CX001")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .embryoCode(embryoCode)
                .classPlanQuantities(Arrays.asList(values))
                .classRecipeNos(Arrays.asList(recipes))
                .build();
    }

    private Cd90ConstructionMaterial material(String constructionCode,
                                               String clothCode,
                                               String unitConsumeMillimeter) {
        return material(constructionCode, "V1", clothCode, unitConsumeMillimeter);
    }

    private Cd90ConstructionMaterial material(String constructionCode,
                                               String constructionVersion,
                                               String clothCode,
                                               String unitConsumeMillimeter) {
        return Cd90ConstructionMaterial.builder()
                .constructionCode(constructionCode)
                .constructionVersion(constructionVersion)
                .clothCode(clothCode)
                .layerNo(1)
                .unitConsumeMillimeter(new BigDecimal(unitConsumeMillimeter))
                .build();
    }
}
