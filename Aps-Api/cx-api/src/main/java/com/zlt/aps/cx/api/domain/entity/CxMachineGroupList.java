package com.zlt.aps.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 组别机台列对象 t_cx_group_machine_list
 *
 * @author zlt
 * @date 2021-12-16
 */
@Data
public class CxMachineGroupList extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属组别
     */
    @Excel(name = "所属组别ID")
    private Long groupId;

    /**
     * 成型机台编码
     */
    @Excel(name = "成型机台编码")
    private String cxMachineCode;

    private String cxMachineName;


    /**
     * 删除标识：0--正常，1-删除
     */
    private String delFlag;



}
