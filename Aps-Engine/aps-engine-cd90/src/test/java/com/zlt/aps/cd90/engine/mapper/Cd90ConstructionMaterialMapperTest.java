package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 施工信息帘布层位拆解测试。
 */
public class Cd90ConstructionMaterialMapperTest {

    private final Cd90ConstructionMaterialMapper mapper = new Cd90ConstructionMaterialMapper();

    /**
     * 施工信息1至3层应展开为独立帘布代码和单耗记录。
     */
    @Test
    public void shouldExpandThreeTireFabricLayers() {
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("E001");
        construction.setTireFabricCode1("C1");
        construction.setTireFabricLength1(new BigDecimal("1000"));
        construction.setTireFabricCode2("C2");
        construction.setTireFabricLength2(new BigDecimal("1200"));
        construction.setTireFabricCode3("C3");
        construction.setTireFabricLength3(new BigDecimal("1400"));

        List<Cd90ConstructionMaterial> result = mapper.map(construction);

        assertEquals(3, result.size());
        assertEquals("C1", result.get(0).getClothCode());
        assertEquals(new BigDecimal("1000"), result.get(0).getUnitConsumeMillimeter());
        assertEquals(3, result.get(2).getLayerNo());
    }

    /**
     * 代码和单耗任一缺失的层位不进入排程输入。
     */
    @Test
    public void incompleteLayerShouldBeIgnored() {
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("E001");
        construction.setTireFabricCode1("C1");
        construction.setTireFabricLength1(new BigDecimal("1000"));
        construction.setTireFabricCode2("C2");

        List<Cd90ConstructionMaterial> result = mapper.map(construction);

        assertEquals(1, result.size());
    }

    /**
     * 施工CORD_SPEC只映射为大卷代码，并读取当前层位的直裁宽度和大卷幅宽。
     */
    @Test
    public void shouldMapBigRollAndCutDimensions() {
        MdmConstructionInfo construction = new MdmConstructionInfo();
        construction.setConstructionCode("EM001");
        construction.setConstructionVersion("V1");
        construction.setCordSpec("BR001");
        construction.setCordWidth(new BigDecimal("1400"));
        construction.setTireFabricCode1("CF001");
        construction.setTireFabricCraft1("280.5");
        construction.setTireFabricLength1(new BigDecimal("500"));

        Cd90ConstructionMaterial result = mapper.map(construction).get(0);

        assertEquals("BR001", result.getBigRollCode());
        assertEquals("V1", result.getConstructionVersion());
        assertEquals("CF001", result.getClothCode());
        assertEquals(new BigDecimal("1400"), result.getCordWidth());
        assertEquals(new BigDecimal("280.5"), result.getCraftWidth());
        assertEquals("280.5", result.getCraftWidthRaw());
    }
}
