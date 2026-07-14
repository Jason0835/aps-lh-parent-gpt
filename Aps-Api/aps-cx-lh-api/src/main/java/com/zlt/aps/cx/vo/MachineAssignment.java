package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;

/**
 * 单机台的分配汇总。
 *
 * @author APS Team
 */
@Data
public class MachineAssignment {
    private String machineCode;
    private List<EmbryoAssignment> embryoAssignments;
}
