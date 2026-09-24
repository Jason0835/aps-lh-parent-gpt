package com.zlt.aps.lh.context;

import com.zlt.aps.lh.component.DedicatedType;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 单个月计划年月、版本下的结构专供只读快照。 */
@Getter
public class StructureDedicatedMachineContext implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 月计划结构名称。 */
    private final String structure;
    /** 全部、部分或未配置专供关系。 */
    private final DedicatedType type;
    /** 转产表中该结构的全部成型机。 */
    private final Set<String> formingMachines;
    /** 具有有效专供配置的成型机。 */
    private final Set<String> dedicatedFormingMachines;
    /** 对应专供硫化机的物理编码并集。 */
    private final Set<String> dedicatedVulcanizingMachines;

    /**
     * 冻结关系集合，避免候选试排修改基础数据。
     * @param structure 结构名称
     * @param formingMachines 全部成型机
     * @param dedicatedFormingMachines 已配置专供的成型机
     * @param dedicatedVulcanizingMachines 专供硫化机并集
     */
    public StructureDedicatedMachineContext(String structure, Set<String> formingMachines,
            Set<String> dedicatedFormingMachines, Set<String> dedicatedVulcanizingMachines) {
        this.structure = structure;
        this.formingMachines = Collections.unmodifiableSet(new LinkedHashSet<>(formingMachines));
        this.dedicatedFormingMachines = Collections.unmodifiableSet(new LinkedHashSet<>(dedicatedFormingMachines));
        this.dedicatedVulcanizingMachines = Collections.unmodifiableSet(new LinkedHashSet<>(dedicatedVulcanizingMachines));
        if (CollectionUtils.isEmpty(dedicatedFormingMachines)) {
            this.type = DedicatedType.NONE;
        } else if (dedicatedFormingMachines.equals(formingMachines)) {
            this.type = DedicatedType.EXCLUSIVE;
        } else {
            this.type = DedicatedType.PREFERRED;
        }
    }
}
