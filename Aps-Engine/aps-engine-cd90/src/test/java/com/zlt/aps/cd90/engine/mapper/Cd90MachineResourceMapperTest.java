package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 机台试算基础数据窄模型映射测试。
 */
public class Cd90MachineResourceMapperTest {

    private final Cd90MachineResourceMapper mapper = new Cd90MachineResourceMapper();

    @Test
    public void shouldMapMachineBindingAndRestriction() {
        Cd90MachineInfo machine = new Cd90MachineInfo();
        machine.setMachineCode("M1");
        machine.setStatus("0");
        machine.setOpenMachineClass("NIGHT");
        machine.setQuota(1200D);
        assertEquals(new BigDecimal("1200.0"), mapper.mapMachine(machine).getQuota());

        Cd90MachineRollMapping binding = new Cd90MachineRollMapping();
        binding.setBigRollCode("BR1");
        binding.setCordFabricCode("CF1");
        binding.setMachineCode("M1");
        assertEquals("BR1", mapper.mapBinding(binding).getBigRollCode());

        Cd90SpecifyMachine restriction = new Cd90SpecifyMachine();
        restriction.setClothCode("CF1");
        restriction.setMachineCode("M1");
        restriction.setJobType("1");
        assertEquals("1", mapper.mapRestriction(restriction).getJobType());
    }

    @Test
    public void shouldKeepBlankDimensionsForGeneralLossRule() {
        Cd90LossSetting source = new Cd90LossSetting();
        source.setLossRate(2.5D);

        Cd90LossRateRule result = mapper.mapLossRule(source);

        assertEquals(null, result.getClothCode());
        assertEquals(null, result.getMachineCode());
        assertEquals(new BigDecimal("2.5"), result.getLossRatePercent());
    }
}
