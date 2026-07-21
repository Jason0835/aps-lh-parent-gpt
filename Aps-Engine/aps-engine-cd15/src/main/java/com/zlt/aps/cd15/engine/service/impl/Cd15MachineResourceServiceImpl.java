package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineAngleWidthMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineLossSettingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineInfoMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineRollMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMaintenanceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineSpecifyMachineMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15MachineResourceMapper;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.service.Cd15MachineResourceService;
import com.zlt.aps.common.core.constant.ApsConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
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
public class Cd15MachineResourceServiceImpl implements Cd15MachineResourceService {

    private final Cd15EngineMachineInfoMapper machineInfoMapper;
    private final Cd15EngineMachineRollMapper machineRollMapper;
    private final Cd15EngineSpecifyMachineMapper specifyMachineMapper;
    private final Cd15EngineLossSettingMapper lossSettingMapper;
    private final Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper;
    private final Cd15EngineMaintenanceMapper maintenanceMapper;
    private final Cd15MachineResourceMapper resourceMapper;

    @Override
    public Cd15MachineResourceSnapshot load(String factoryCode,
                                            LocalDateTime shiftStart,
                                            LocalDateTime shiftEnd) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(shiftStart, "班次开始时间不能为空");
        Assert.notNull(shiftEnd, "班次结束时间不能为空");
        if (!shiftEnd.isAfter(shiftStart)) {
            throw new IllegalArgumentException("班次结束时间必须晚于开始时间");
        }

        // 机台主数据先转换为只读窄模型，后续试算不直接修改数据库实体。
        List<Cd15MachineResource> machines = machineInfoMapper.selectList(
                        Wrappers.<Cd15MachineInfo>lambdaQuery()
                                .eq(Cd15MachineInfo::getFactoryCode, factoryCode)
                                .eq(Cd15MachineInfo::getStatus, ApsConstant.APS_STRING_1)
                                .orderByAsc(Cd15MachineInfo::getMachineCode))
                .stream().map(resourceMapper::mapMachine).collect(Collectors.toList());
        Map<String, Cd15MachineResource> machineByCode = machines.stream()
                .collect(Collectors.toMap(Cd15MachineResource::getMachineCode,
                        Function.identity(), (left, right) -> left));

        // 日期范围查询后再用时间区间重叠判断，兼容跨日班次和跨日检修。
        List<Cd15MachineMaintenancePlan> maintenances = maintenanceMapper.selectList(
                Wrappers.<Cd15MachineMaintenancePlan>lambdaQuery()
                        .eq(Cd15MachineMaintenancePlan::getFactoryCode, factoryCode)
                        .between(Cd15MachineMaintenancePlan::getDowntimeDate,
                                Date.valueOf(shiftStart.toLocalDate()),
                                Date.valueOf(shiftEnd.toLocalDate())));
        maintenances.stream()
                .filter(item -> overlaps(item, shiftStart, shiftEnd))
                .collect(Collectors.groupingBy(Cd15MachineMaintenancePlan::getMachineCode))
                .forEach((machineCode, plans) -> this.applyMaintenance(
                        machineByCode.get(machineCode), plans, shiftStart, shiftEnd));

        // 绑定、限制和损耗与机台一起冻结为本班快照，保证同班所有规格使用同一规则版本。
        Cd15MachineResourceSnapshot snapshot = Cd15MachineResourceSnapshot.builder()
                .machines(machines)
                .bindings(machineRollMapper.selectList(Wrappers.<Cd15MachineRollMapping>lambdaQuery()
                                .eq(Cd15MachineRollMapping::getFactoryCode, factoryCode))
                        .stream().map(resourceMapper::mapBinding).collect(Collectors.toList()))
                .restrictions(specifyMachineMapper.selectList(Wrappers.<Cd15SpecifyMachine>lambdaQuery()
                                .eq(Cd15SpecifyMachine::getFactoryCode, factoryCode))
                        .stream().map(resourceMapper::mapRestriction).collect(Collectors.toList()))
                .lossRateRules(lossSettingMapper.selectList(Wrappers.<Cd15LossSetting>lambdaQuery()
                                .eq(Cd15LossSetting::getFactoryCode, factoryCode))
                        .stream().map(resourceMapper::mapLossRule).collect(Collectors.toList()))
                .angleWidthMaxByAngle(angleWidthMappingMapper.selectList(
                                Wrappers.<Cd15AngleWidthMapping>lambdaQuery()
                                        .eq(Cd15AngleWidthMapping::getFactoryCode, factoryCode))
                        .stream()
                        .filter(item -> item.getCutAngle() != null
                                && item.getClothWidthMax() != null
                                && item.getClothWidthMax().signum() > 0)
                        .collect(Collectors.toMap(
                                item -> item.getCutAngle().trim(),
                                Cd15AngleWidthMapping::getClothWidthMax,
                                (first, second) -> first)))
                .build();
        log.info("[斜裁自动排程] 机台资源快照加载完成, factoryCode={}, shiftStart={}, shiftEnd={}, "
                        + "machineCount={}, bindingCount={}, restrictionCount={}, lossRuleCount={}, angleCount={}",
                factoryCode, shiftStart, shiftEnd, snapshot.getMachines().size(),
                snapshot.getBindings().size(), snapshot.getRestrictions().size(),
                snapshot.getLossRateRules().size(), snapshot.getAngleWidthMaxByAngle().size());
        return snapshot;
    }

    private boolean overlaps(Cd15MachineMaintenancePlan item,
                             LocalDateTime shiftStart,
                             LocalDateTime shiftEnd) {
        LocalDateTime start = toLocalDateTime(item.getDowntimeStartTime());
        LocalDateTime end = toLocalDateTime(item.getDowntimeEndTime());
        return start != null && end != null && start.isBefore(shiftEnd) && end.isAfter(shiftStart);
    }

    /**
     * 合并当前班次内相互重叠的检修区间，只扣一次真实重叠时长。
     */
    private void applyMaintenance(
            Cd15MachineResource machine,
            List<Cd15MachineMaintenancePlan> plans,
            LocalDateTime shiftStart,
            LocalDateTime shiftEnd) {
        if (machine == null || plans == null || plans.isEmpty()) {
            return;
        }
        List<LocalDateTime[]> intervals = plans.stream()
                .map(item -> new LocalDateTime[]{
                        this.toLocalDateTime(item.getDowntimeStartTime()),
                        this.toLocalDateTime(item.getDowntimeEndTime())})
                .filter(interval -> interval[0] != null && interval[1] != null)
                .map(interval -> new LocalDateTime[]{
                        interval[0].isBefore(shiftStart) ? shiftStart : interval[0],
                        interval[1].isAfter(shiftEnd) ? shiftEnd : interval[1]})
                .filter(interval -> interval[0].isBefore(interval[1]))
                .sorted(java.util.Comparator.comparing(interval -> interval[0]))
                .collect(Collectors.toList());
        if (intervals.isEmpty()) {
            return;
        }
        LocalDateTime mergedStart = intervals.get(0)[0];
        LocalDateTime mergedEnd = intervals.get(0)[1];
        LocalDateTime firstStart = mergedStart;
        LocalDateTime lastEnd = mergedEnd;
        long unavailableSeconds = 0L;
        for (int index = 1; index < intervals.size(); index++) {
            LocalDateTime currentStart = intervals.get(index)[0];
            LocalDateTime currentEnd = intervals.get(index)[1];
            if (!currentStart.isAfter(mergedEnd)) {
                if (currentEnd.isAfter(mergedEnd)) {
                    mergedEnd = currentEnd;
                }
            } else {
                unavailableSeconds += java.time.Duration.between(
                        mergedStart, mergedEnd).getSeconds();
                mergedStart = currentStart;
                mergedEnd = currentEnd;
            }
            if (currentEnd.isAfter(lastEnd)) {
                lastEnd = currentEnd;
            }
        }
        unavailableSeconds += java.time.Duration.between(
                mergedStart, mergedEnd).getSeconds();
        machine.setMaintenanceStart(firstStart);
        machine.setMaintenanceEnd(lastEnd);
        machine.setMaintenanceSeconds(Math.toIntExact(unavailableSeconds));
    }

    private LocalDateTime toLocalDateTime(java.util.Date value) {
        return value == null ? null : value.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
