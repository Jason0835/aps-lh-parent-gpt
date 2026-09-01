package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * S4.5 机台驱动阶段使用的轻量机台资源。
 *
 * <p>对象只引用现有机台运行态并冻结本轮基础收尾时间，不复制停机、清洗、模具、
 * 胶囊或结果账本。SKU 相关的真实可开产时间仍由正式新增时间轴逐候选计算。</p>
 *
 * @author APS
 */
public final class MachineResource {

    /** 同班次内固定按基础收尾时间、机台编码升序 */
    public static final Comparator<MachineResource> RESOURCE_ORDER = Comparator
            .comparing(MachineResource::getEndingTime, Comparator.nullsFirst(Date::compareTo))
            .thenComparing(MachineResource::getRepresentativeMachineCode,
                    Comparator.nullsLast(String::compareTo));

    /** 正式运行态机台引用 */
    private final MachineScheduleDTO machine;
    /** 单控 L/R 归一后的物理机台编码 */
    private final String physicalMachineCode;
    /** 本轮构建时的基础收尾时间 */
    private final Date endingTime;
    /** 当前物理机台可能声明的运行态编码 */
    private final List<String> declaredMachineCodes;

    public MachineResource(MachineScheduleDTO machine, List<String> declaredMachineCodes) {
        this.machine = Objects.requireNonNull(machine, "机台资源不能为空");
        this.physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                machine.getMachineCode());
        this.endingTime = machine.getEstimatedEndTime();
        this.declaredMachineCodes = declaredMachineCodes == null
                ? Collections.singletonList(machine.getMachineCode())
                : Collections.unmodifiableList(new ArrayList<String>(declaredMachineCodes));
    }

    public MachineScheduleDTO getMachine() {
        return machine;
    }

    public String getPhysicalMachineCode() {
        return physicalMachineCode;
    }

    public Date getEndingTime() {
        return endingTime;
    }

    public List<String> getDeclaredMachineCodes() {
        return declaredMachineCodes;
    }

    public String getRepresentativeMachineCode() {
        return Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())
                ? null : machine.getMachineCode();
    }
}
