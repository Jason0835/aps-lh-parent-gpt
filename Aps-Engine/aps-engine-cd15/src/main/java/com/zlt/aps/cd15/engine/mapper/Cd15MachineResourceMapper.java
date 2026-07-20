package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.engine.model.Cd15LossRateRule;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineRestriction;
import com.zlt.aps.cd15.engine.model.Cd15MachineRollBinding;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 机台试算基础数据窄模型映射器。
 */
@Component
public class Cd15MachineResourceMapper {

    public Cd15MachineResource mapMachine(Cd15MachineInfo source) {
        return Cd15MachineResource.builder()
                .machineCode(source.getMachineCode())
                .status(source.getStatus())
                .openMachineClass(source.getOpenMachineClass())
                .singleCutSupported("1".equals(source.getSingleCutFlag()))
                .splitCutSupported("1".equals(source.getSplitCutFlag()))
                .defaultCutMode(source.getDefaultCutMode())
                .singleShiftCapacity(decimal(source.getSingleShiftCapacity()))
                .splitShiftCapacity(decimal(source.getSplitShiftCapacity()))
                .clothWidthMax(decimal(source.getClothWidthMax()))
                .clothWidthMin(decimal(source.getClothWidthMin()))
                .build();
    }

    public Cd15MachineRollBinding mapBinding(Cd15MachineRollMapping source) {
        return Cd15MachineRollBinding.builder()
                .bigRollCode(source.getBigRollCode())
                .shiftCode(source.getShiftCode())
                .machineCode(source.getMachineCode())
                .build();
    }

    public Cd15MachineRestriction mapRestriction(Cd15SpecifyMachine source) {
        return Cd15MachineRestriction.builder()
                .steelStripCode(source.getSteelStripCode())
                .machineCode(source.getMachineCode())
                .jobType(source.getJobType())
                .build();
    }

    public Cd15LossRateRule mapLossRule(Cd15LossSetting source) {
        return Cd15LossRateRule.builder()
                .steelStripCode(source.getSteelStripCode())
                .machineCode(source.getMachineCode())
                .lossRatePercent(decimal(source.getLossRate()))
                .build();
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}
