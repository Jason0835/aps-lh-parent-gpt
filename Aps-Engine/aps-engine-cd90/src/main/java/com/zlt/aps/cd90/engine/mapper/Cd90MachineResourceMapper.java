package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import com.zlt.aps.cd90.engine.model.Cd90MachineResource;
import com.zlt.aps.cd90.engine.model.Cd90MachineRestriction;
import com.zlt.aps.cd90.engine.model.Cd90MachineRollBinding;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 机台试算基础数据窄模型映射器。
 */
@Component
public class Cd90MachineResourceMapper {

    public Cd90MachineResource mapMachine(Cd90MachineInfo source) {
        return Cd90MachineResource.builder()
                .machineCode(source.getMachineCode())
                .status(source.getStatus())
                .openMachineClass(source.getOpenMachineClass())
                .quota(decimal(source.getQuota()))
                .build();
    }

    public Cd90MachineRollBinding mapBinding(Cd90MachineRollMapping source) {
        return Cd90MachineRollBinding.builder()
                .bigRollCode(source.getBigRollCode())
                .clothCode(source.getCordFabricCode())
                .machineCode(source.getMachineCode())
                .build();
    }

    public Cd90MachineRestriction mapRestriction(Cd90SpecifyMachine source) {
        return Cd90MachineRestriction.builder()
                .clothCode(source.getClothCode())
                .machineCode(source.getMachineCode())
                .jobType(source.getJobType())
                .build();
    }

    public Cd90LossRateRule mapLossRule(Cd90LossSetting source) {
        return Cd90LossRateRule.builder()
                .clothCode(source.getClothCode())
                .machineCode(source.getMachineCode())
                .lossRatePercent(decimal(source.getLossRate()))
                .build();
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }
}
