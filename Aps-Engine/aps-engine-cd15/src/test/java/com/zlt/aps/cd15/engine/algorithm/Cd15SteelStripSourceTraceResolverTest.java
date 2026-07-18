package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15EmbryoPlanSurplus;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 钢带成型来源追溯解析测试。 */
public class Cd15SteelStripSourceTraceResolverTest {

    private final Cd15SteelStripSourceTraceResolver resolver = new Cd15SteelStripSourceTraceResolver();

    @Test
    public void shouldUsePositivePlansAndDeduplicateSortedSourcesAndEmbryos() {
        Cd15FormingScheduleSource e1First = forming(
                "E1", "B2", "M2", quantities("100", "0"), recipes("R1", "R9"));
        Cd15FormingScheduleSource e1Duplicate = forming(
                "E1", "B1", "M1", quantities("50"), recipes("R1"));
        Cd15FormingScheduleSource e2 = forming(
                "E2", "B2", "M2", quantities("20"), recipes("R2"));
        Cd15FormingScheduleSource zeroPlan = forming(
                "E3", "B0", "M0", quantities("0"), recipes("R3"));

        Map<String, Cd15SteelStripSourceTrace> traces = resolver.resolve(
                Arrays.asList(e1First, e1Duplicate, e2, zeroPlan),
                Arrays.asList(construction("E1", "R1"), construction("E2", "R2"),
                        construction("E3", "R3")),
                Arrays.asList(surplus("E1", "10"), surplus("E2", "20"), surplus("E3", "999")));

        Cd15SteelStripSourceTrace trace = traces.get("C1");
        assertEquals("B1,B2", trace.getCxBatchNo());
        assertEquals("M1,M2", trace.getCxMachineCodes());
        assertEquals(new BigDecimal("30"), trace.getPlanSurplusQty());
    }

    @Test
    public void shouldReturnNullSurplusWhenAnyRelatedEmbryoIsMissing() {
        Map<String, Cd15SteelStripSourceTrace> traces = resolver.resolve(
                Arrays.asList(
                        forming("E1", "B1", "M1", quantities("10"), recipes("R1")),
                        forming("E2", "B2", "M2", quantities("20"), recipes("R2"))),
                Arrays.asList(construction("E1", "R1"), construction("E2", "R2")),
                Collections.singletonList(surplus("E1", "10")));

        assertNull(traces.get("C1").getPlanSurplusQty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectBatchCollectionLongerThanColumn() {
        resolver.resolve(
                Collections.singletonList(forming(
                        "E1", repeat("B", 501), "M1", quantities("10"), recipes("R1"))),
                Collections.singletonList(construction("E1", "R1")),
                Collections.singletonList(surplus("E1", "10")));
    }

    private Cd15FormingScheduleSource forming(String embryoCode, String batchNo,
                                              String machineCode,
                                              java.util.List<BigDecimal> quantities,
                                              java.util.List<String> recipeNos) {
        return Cd15FormingScheduleSource.builder()
                .embryoCode(embryoCode).cxBatchNo(batchNo).cxMachineCode(machineCode)
                .classPlanQuantities(quantities).classRecipeNos(recipeNos).build();
    }

    private Cd15ConstructionMaterial construction(String embryoCode, String version) {
        return Cd15ConstructionMaterial.builder().constructionCode(embryoCode)
                .constructionVersion(version).steelStripCode("C1").build();
    }

    private Cd15EmbryoPlanSurplus surplus(String embryoCode, String quantity) {
        return Cd15EmbryoPlanSurplus.builder().embryoCode(embryoCode)
                .planSurplusQuantity(new BigDecimal(quantity)).build();
    }

    private java.util.List<BigDecimal> quantities(String... values) {
        return Arrays.stream(values).map(BigDecimal::new).collect(java.util.stream.Collectors.toList());
    }

    private java.util.List<String> recipes(String... values) {
        return Arrays.asList(values);
    }

    private String repeat(String value, int count) {
        return String.join("", Collections.nCopies(count, value));
    }
}
