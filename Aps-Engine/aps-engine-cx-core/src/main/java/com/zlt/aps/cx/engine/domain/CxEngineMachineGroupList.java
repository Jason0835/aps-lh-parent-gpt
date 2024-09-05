package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型机台组列表
 */
@Data
public class CxEngineMachineGroupList extends ApsBaseEntity {
    /** 主键ID */
    @ApiModelProperty(value = "列表ID")
    private Long id;

    /**
     * 组别ID
     */
    private Long groupId;

    /** 机台组名 */
    @ApiModelProperty(value = "机台组名")
    private String groupName;

    /** 成型机台编码 */
    @ApiModelProperty(value = "成型机台编码")
    private String cxMachineCode;

    /** 成型机台名称 */
    @ApiModelProperty(value = "成型机台名称")
    private String cxMachineName;

    /** 可投产班数 */
    @ApiModelProperty(value = "可投产班数")
    private Double productShift;
}
