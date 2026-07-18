package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.engine.model.Cd15LossRateRule;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 机台试算基础数据窄模型映射测试。
 */
public class Cd15MachineResourceMapperTest {

    private final Cd15MachineResourceMapper mapper = new Cd15MachineResourceMapper();

    @Test
    public void shouldMapMachineBindingAndRestriction() {
        Cd15MachineInfo machine = new Cd15MachineInfo();
        machine.setMachineCode("M1");
        machine.setStatus("1");
        machine.setOpenMachineClass("NIGHT");
        machine.setIsOutTwo("0");
        machine.setQuota(1200D);
        machine.setClothWidthMin(40.5D);
        machine.setClothWidthMax(60.5D);
        assertEquals(new BigDecimal("1200.0"), mapper.mapMachine(machine).getQuota());
        assertEquals(new BigDecimal("40.5"), mapper.mapMachine(machine).getClothWidthMin());
        assertEquals(new BigDecimal("60.5"), mapper.mapMachine(machine).getClothWidthMax());
        assertTrue(mapper.mapMachine(machine).isSplitCutSupported());
        machine.setIsOutTwo("1");
        assertFalse(mapper.mapMachine(machine).isSplitCutSupported());

        Cd15MachineRollMapping binding = new Cd15MachineRollMapping();
        binding.setBigRollCode("BR1");
        binding.setMachineCode("M1");
        binding.setShiftCode("01,02,03");
        assertEquals("BR1", mapper.mapBinding(binding).getBigRollCode());
        assertEquals("01,02,03", mapper.mapBinding(binding).getShiftCode());

        Cd15SpecifyMachine restriction = new Cd15SpecifyMachine();
        restriction.setSteelStripCode("CF1");
        restriction.setMachineCode("M1");
        restriction.setJobType("1");
        assertEquals("1", mapper.mapRestriction(restriction).getJobType());
    }

    @Test
    public void shouldKeepBlankDimensionsForGeneralLossRule() {
        Cd15LossSetting source = new Cd15LossSetting();
        source.setLossRate(2.5D);

        Cd15LossRateRule result = mapper.mapLossRule(source);

        assertEquals(null, result.getSteelStripCode());
        assertEquals(null, result.getMachineCode());
        assertEquals(new BigDecimal("2.5"), result.getLossRatePercent());
    }
}
