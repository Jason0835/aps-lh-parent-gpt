package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineLossSettingMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineInfoMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineRollMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMaintenanceMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineSpecifyMachineMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90MachineResourceMapper;
import com.zlt.aps.cd90.engine.model.Cd90MachineResource;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
import com.zlt.aps.cd90.engine.service.Cd90MachineResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 当前班次机台试算基础数据加载实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90MachineResourceServiceImpl implements Cd90MachineResourceService {

    private final Cd90EngineMachineInfoMapper machineInfoMapper;
    private final Cd90EngineMachineRollMapper machineRollMapper;
    private final Cd90EngineSpecifyMachineMapper specifyMachineMapper;
    private final Cd90EngineLossSettingMapper lossSettingMapper;
    private final Cd90EngineMaintenanceMapper maintenanceMapper;
    private final Cd90MachineResourceMapper resourceMapper;

    @Override
    public Cd90MachineResourceSnapshot load(String factoryCode,
                                            LocalDateTime shiftStart,
                                            LocalDateTime shiftEnd) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(shiftStart, "班次开始时间不能为空");
        Assert.notNull(shiftEnd, "班次结束时间不能为空");
        if (!shiftEnd.isAfter(shiftStart)) {
            throw new IllegalArgumentException("班次结束时间必须晚于开始时间");
        }

        // 机台主数据先转换为只读窄模型，后续试算不直接修改数据库实体。
        List<Cd90MachineResource> machines = machineInfoMapper.selectList(
                        Wrappers.<Cd90MachineInfo>lambdaQuery()
                                .eq(Cd90MachineInfo::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90MachineInfo::getMachineCode))
                .stream().map(resourceMapper::mapMachine).collect(Collectors.toList());
        Map<String, Cd90MachineResource> machineByCode = machines.stream()
                .collect(Collectors.toMap(Cd90MachineResource::getMachineCode,
                        Function.identity(), (left, right) -> left));

        // 日期范围查询后再用时间区间重叠判断，兼容跨日班次和跨日检修。
        List<Cd90MachineMaintenancePlan> maintenances = maintenanceMapper.selectList(
                Wrappers.<Cd90MachineMaintenancePlan>lambdaQuery()
                        .eq(Cd90MachineMaintenancePlan::getFactoryCode, factoryCode)
                        .between(Cd90MachineMaintenancePlan::getDowntimeDate,
                                Date.valueOf(shiftStart.toLocalDate()),
                                Date.valueOf(shiftEnd.toLocalDate())));
        maintenances.stream()
                .filter(item -> overlaps(item, shiftStart, shiftEnd))
                .forEach(item -> markMaintenance(machineByCode.get(item.getMachineCode()), item));

        // 绑定、限制和损耗与机台一起冻结为本班快照，保证同班所有规格使用同一规则版本。
        Cd90MachineResourceSnapshot snapshot = Cd90MachineResourceSnapshot.builder()
                .machines(machines)
                .bindings(machineRollMapper.selectList(Wrappers.<Cd90MachineRollMapping>lambdaQuery()
                                .eq(Cd90MachineRollMapping::getFactoryCode, factoryCode))
                        .stream().map(resourceMapper::mapBinding).collect(Collectors.toList()))
                .restrictions(specifyMachineMapper.selectList(Wrappers.<Cd90SpecifyMachine>lambdaQuery()
                                .eq(Cd90SpecifyMachine::getFactoryCode, factoryCode))
                        .stream().map(resourceMapper::mapRestriction).collect(Collectors.toList()))
                .lossRateRules(lossSettingMapper.selectList(Wrappers.<Cd90LossSetting>lambdaQuery()
                                .eq(Cd90LossSetting::getFactoryCode, factoryCode))
                        .stream().map(resourceMapper::mapLossRule).collect(Collectors.toList()))
                .build();
        log.info("[直裁自动排程] 机台资源快照加载完成, factoryCode={}, shiftStart={}, shiftEnd={}, "
                        + "machineCount={}, bindingCount={}, restrictionCount={}, lossRuleCount={}",
                factoryCode, shiftStart, shiftEnd, snapshot.getMachines().size(),
                snapshot.getBindings().size(), snapshot.getRestrictions().size(),
                snapshot.getLossRateRules().size());
        return snapshot;
    }

    private boolean overlaps(Cd90MachineMaintenancePlan item,
                             LocalDateTime shiftStart,
                             LocalDateTime shiftEnd) {
        LocalDateTime start = toLocalDateTime(item.getDowntimeStartTime());
        LocalDateTime end = toLocalDateTime(item.getDowntimeEndTime());
        return start != null && end != null && start.isBefore(shiftEnd) && end.isAfter(shiftStart);
    }

    private void markMaintenance(Cd90MachineResource machine,
                                 Cd90MachineMaintenancePlan maintenance) {
        if (machine == null) {
            return;
        }
        LocalDateTime start = toLocalDateTime(maintenance.getDowntimeStartTime());
        LocalDateTime end = toLocalDateTime(maintenance.getDowntimeEndTime());
        // 同班存在多段检修时取最早开始和最晚结束，保守覆盖全部不可用区间。
        if (machine.getMaintenanceStart() == null || start.isBefore(machine.getMaintenanceStart())) {
            machine.setMaintenanceStart(start);
        }
        if (machine.getMaintenanceEnd() == null || end.isAfter(machine.getMaintenanceEnd())) {
            machine.setMaintenanceEnd(end);
        }
    }

    private LocalDateTime toLocalDateTime(java.util.Date value) {
        return value == null ? null : value.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
