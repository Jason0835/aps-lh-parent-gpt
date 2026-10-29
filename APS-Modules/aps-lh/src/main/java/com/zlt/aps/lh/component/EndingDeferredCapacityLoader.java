package com.zlt.aps.lh.component;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.impl.DefaultProductionShutdownStrategy;
import com.zlt.aps.lh.engine.strategy.support.EndingDeferredCapacitySnapshot;
import com.zlt.aps.lh.mapper.MdmDevicePlanShutMapper;
import com.zlt.aps.lh.mapper.MdmWorkCalendarMapper;
import com.zlt.aps.lh.service.impl.LhCleaningScheduleService;
import com.zlt.aps.lh.service.impl.LhDeviceStopPlanScheduleService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 在基础数据就绪后构建延期评估专用时间轴，补读未来日历与设备计划且隔离全部运行态。
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Component
public class EndingDeferredCapacityLoader {
    /**
     * 工作日历只读查询。
     */
    @Resource
    private MdmWorkCalendarMapper workCalendarMapper;
    /**
     * 设备来源计划只读查询。
     */
    @Resource
    private MdmDevicePlanShutMapper devicePlanShutMapper;
    /**
     * 原05/06维修准备算法，只在独立上下文发布约束。
     */
    @Resource
    private LhDeviceStopPlanScheduleService deviceStopPlanScheduleService;
    /**
     * 原清洗计划解析，独立消费模拟清洗名额。
     */
    @Resource
    private LhCleaningScheduleService cleaningScheduleService;
    /**
     * 复用工厂工作日历和开停产规则。
     */
    @Resource
    private DefaultProductionShutdownStrategy productionShutdownStrategy;

    /**
     * 冻结延期产能输入，不修改原班次、设备计划和机台运行态。
     *
     * @param source 已完成标准机台初始化的正式上下文
     */
    public void loadAndAttach(LhScheduleContext source) {
        if (source.getScheduleWindowShifts().isEmpty()) {
            return;
        }
        List<LhShiftConfigVO> shifts = this.buildProjectionShifts(source);
        LhScheduleContext projection = this.createProjectionContext(source, shifts);
        Date lookupStart = LhScheduleTimeUtil.addDays(source.getScheduleDate(), -1);
        Date lookupEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        List<MdmWorkCalendar> calendars = workCalendarMapper.selectList(new LambdaQueryWrapper<MdmWorkCalendar>()
                .eq(MdmWorkCalendar::getFactoryCode, source.getFactoryCode())
                .eq(MdmWorkCalendar::getProcCode, LhScheduleConstant.PROC_CODE_LH)
                .ge(MdmWorkCalendar::getProductionDate, lookupStart)
                .lt(MdmWorkCalendar::getProductionDate, LhScheduleTimeUtil.addDays(lookupEnd, 1)));
        projection.setWorkCalendarList(calendars);
        List<MdmDevicePlanShut> stops = devicePlanShutMapper.selectList(new LambdaQueryWrapper<MdmDevicePlanShut>()
                .eq(MdmDevicePlanShut::getFactoryCode, source.getFactoryCode())
                .lt(MdmDevicePlanShut::getBeginDate, lookupEnd)
                .gt(MdmDevicePlanShut::getEndDate, lookupStart));
        projection.setDevicePlanShutList(deviceStopPlanScheduleService.prepareStopPlans(projection, stops));
        Map<String, List<MachineCleaningWindowDTO>> cleaning = cleaningScheduleService.initializePlans(projection);
        projection.getMachineScheduleMap().forEach((machineCode, machine) -> machine.setCleaningWindowList(
                new ArrayList<MachineCleaningWindowDTO>(cleaning.getOrDefault(machineCode, new ArrayList<>()))));
        productionShutdownStrategy.prepareOpenStopContext(projection);
        EndingDeferredCapacitySnapshot snapshot = new EndingDeferredCapacitySnapshot();
        snapshot.setWindowEndTime(source.getScheduleWindowShifts().stream().map(LhShiftConfigVO::getShiftEndDateTime)
                .max(Date::compareTo).get());
        snapshot.setDeferredEndTime(lookupEnd);
        snapshot.setCapacityContext(projection);
        source.setEndingDeferredCapacitySnapshot(snapshot);
        if (log.isInfoEnabled()) {
            log.info("SKU收尾延期产能快照完成, factoryCode: {}, T: {}, delayDays: {}, windowEnd: {}, "
                            + "deferredEnd: {}, projectionShifts: {}, calendars: {}, stopPlans: {}",
                    source.getFactoryCode(), source.getScheduleDate(), source.getScheduleConfig().getSkuEndingDeferredExpansionDays(),
                    snapshot.getWindowEndTime(), lookupEnd, shifts.size(), calendars.size(), stops.size());
        }
    }

    /**
     * 复用窗口末业务日的工厂班次模板，顺延X天生成独立计算槽位，不增加结果字段。
     *
     * @param source 正式班次窗口
     * @return 正式模板副本及延期班次，序号只用于隔离容量图
     */
    List<LhShiftConfigVO> buildProjectionShifts(LhScheduleContext source) {
        List<LhShiftConfigVO> shifts = source.getScheduleWindowShifts().stream()
                .map(shift -> BeanUtil.copyProperties(shift, LhShiftConfigVO.class)).collect(Collectors.toList());
        Date lastWorkDate = shifts.stream().map(LhShiftConfigVO::getWorkDate).max(Date::compareTo).get();
        List<LhShiftConfigVO> templates = shifts.stream().filter(shift -> Objects.equals(lastWorkDate, shift.getWorkDate()))
                .sorted(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime)).collect(Collectors.toList());
        int index = shifts.stream().mapToInt(LhShiftConfigVO::getShiftIndex).max().getAsInt();
        for (int day = 1; day <= source.getScheduleConfig().getSkuEndingDeferredExpansionDays(); day++) {
            for (LhShiftConfigVO template : templates) {
                LhShiftConfigVO shift = BeanUtil.copyProperties(template, LhShiftConfigVO.class);
                shift.setShiftIndex(++index);
                shift.setDateOffset(template.getDateOffset() + day);
                shifts.add(shift);
            }
        }
        return shifts;
    }

    /**
     * 显式复制公共产能依赖，所有数量、维修、清洗和额度运行态使用新上下文默认实例。
     *
     * @param source 正式基础数据
     * @param shifts 独立班次
     * @return 可独立初始化设备约束的计算上下文
     */
    private LhScheduleContext createProjectionContext(LhScheduleContext source, List<LhShiftConfigVO> shifts) {
        LhScheduleContext projection = new LhScheduleContext();
        projection.setFactoryCode(source.getFactoryCode());
        projection.setScheduleDate(source.getScheduleDate());
        projection.setScheduleTargetDate(source.getScheduleTargetDate());
        projection.setWindowEndDate(shifts.get(shifts.size() - 1).getWorkDate());
        projection.setScheduleWindowShifts(shifts);
        projection.setScheduleConfig(source.getScheduleConfig());
        projection.setLhParamsMap(new LinkedHashMap<>(source.getLhParamsMap()));
        projection.setMachineInfoMap(new LinkedHashMap<>(source.getMachineInfoMap()));
        projection.setMachineOnlineInfoMap(new LinkedHashMap<>(source.getMachineOnlineInfoMap()));
        projection.setSkuMouldRelMap(new LinkedHashMap<>(source.getSkuMouldRelMap()));
        projection.setSkuLhCapacityMap(new LinkedHashMap<>(source.getSkuLhCapacityMap()));
        projection.setMaterialInfoMap(new LinkedHashMap<>(source.getMaterialInfoMap()));
        projection.setMonthPlanList(new ArrayList<>(source.getMonthPlanList()));
        Map<String, MachineScheduleDTO> machines = new LinkedHashMap<>();
        source.getMachineScheduleMap().forEach((code, machine) -> {
            MachineScheduleDTO copied = BeanUtil.copyProperties(machine, MachineScheduleDTO.class);
            copied.setMaintenanceWindowList(machine.getMaintenanceWindowList().stream()
                    .map(window -> BeanUtil.copyProperties(window, MachineMaintenanceWindowDTO.class)).collect(Collectors.toList()));
            copied.setCleaningWindowList(new ArrayList<>());
            machines.put(code, copied);
        });
        projection.setMachineScheduleMap(machines);
        return projection;
    }
}
