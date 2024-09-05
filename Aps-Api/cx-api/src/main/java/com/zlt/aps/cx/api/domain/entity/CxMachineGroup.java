package com.zlt.aps.cx.api.domain.entity;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 成型机组对象 t_cx_machine_group
 * 
 * @author zlt
 * @date 2021-12-16
 */
@ApiModel(value = "成型机组对象", description = "成型机组对象 ")
@Data
public class CxMachineGroup extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 机台组名 */
    @Excel(name = "ui.data.column.machineGroup.groupName")
    @ApiModelProperty(value = "机台组名")
    private String groupName;

    /** 可投产班数 */
    @Excel(name = "ui.data.column.machineGroup.productShift")
    @ApiModelProperty(value = "可投产班数")
    private Long productShift;

    /** 删除标识：0--正常，1-删除 */
    @ApiModelProperty(value = "删除标识")
    private String delFlag;


    /** 组别机台列信息 */
    private List<CxMachineGroupList> cxMachineGroupListList;

    private String[] machineCodes;



}
