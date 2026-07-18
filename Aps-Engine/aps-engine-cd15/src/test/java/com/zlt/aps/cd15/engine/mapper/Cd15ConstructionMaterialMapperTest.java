package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 施工信息钢带层位拆解测试。
 */
public class Cd15ConstructionMaterialMapperTest {

    private final Cd15ConstructionMaterialMapper mapper = new Cd15ConstructionMaterialMapper();

    /**
     * 施工信息1至3层应展开为独立钢带代码和单耗记录。
     */
    @Test
    public void shouldExpandThreeSteelStripLayers() {
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("E001");
        construction.setBeltCode1("C1");
        construction.setBelt1Length(new BigDecimal("1000"));
        construction.setBeltCode2("C2");
        construction.setBelt2Length(new BigDecimal("1200"));
        construction.setBeltCode3("C3");
        construction.setBelt3Length(new BigDecimal("1400"));

        List<Cd15ConstructionMaterial> result = mapper.map(construction);

        assertEquals(3, result.size());
        assertEquals("C1", result.get(0).getSteelStripCode());
        assertEquals(new BigDecimal("1000"), result.get(0).getUnitConsumeMillimeter());
        assertEquals(3, result.get(2).getLayerNo());
    }

    /**
     * 代码存在但单耗缺失的层位仍进入映射，交由批次前置检查输出明确错误。
     */
    @Test
    public void incompleteLayerShouldRemainForPrecheck() {
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("E001");
        construction.setBeltCode1("C1");
        construction.setBelt1Length(new BigDecimal("1000"));
        construction.setBeltCode2("C2");

        List<Cd15ConstructionMaterial> result = mapper.map(construction);

        assertEquals(2, result.size());
        assertEquals(null, result.get(1).getUnitConsumeMillimeter());
    }

    /**
     * 施工CORD_SPEC只映射为大卷代码，并读取当前层位的斜裁宽度和大卷幅宽。
     */
    @Test
    public void shouldMapBigRollAndCutDimensions() {
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("EM001");
        construction.setConstructionVersion("V1");
        construction.setArticleCrownSpec("BR001");
        construction.setCordWidth(new BigDecimal("1400"));
        construction.setBeltCuttingAngle("15");
        construction.setBeltCode1("CF001");
        construction.setBeltCraft1(new BigDecimal("280.5"));
        construction.setBelt1Length(new BigDecimal("500"));

        Cd15ConstructionMaterial result = mapper.map(construction).get(0);

        assertEquals("BR001", result.getBigRollCode());
        assertEquals("V1", result.getConstructionVersion());
        assertEquals("CF001", result.getSteelStripCode());
        assertEquals(new BigDecimal("280.5"), result.getCraftWidth());
        assertEquals("15", result.getCuttingAngle());
    }
}
