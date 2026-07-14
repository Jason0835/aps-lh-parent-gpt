package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.List;

/**
 * 均衡分配对外返回结构：机台 -> 多条 EmbryoAssignment。
 *
 * @author APS Team
 */
@Data
public class BalancingResult {
    private List<MachineAssignment> assignments;
}
