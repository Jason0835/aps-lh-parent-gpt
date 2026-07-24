package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
/**
 * 分组指定生产信息对象
 *
 * @author ZLT
 * @date 20260713
 */
@Data
@Slf4j
public class GroupAppointProductionInfoVo implements Serializable {
    /**
     * 分组名称：必须项
     */
    private String groupName;
    /**
     * 成型机台编号
     */
    private String cxMachineCode;
    /**
     * 周期第N天：排产周期所处天数
     */
    private Integer monthStartDay;
    /**
     * 最大可分配生产天数
     */
    private Integer maxAllocationDay;
}
