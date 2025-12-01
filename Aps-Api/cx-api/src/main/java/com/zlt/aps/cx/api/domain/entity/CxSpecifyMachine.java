package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.ibatis.type.JdbcType;

import java.util.Date;

/**
 * 定点机台对象 t_cx_specify_machine
 *
 * @author zlt
 * @date 2021-07-21
 */
@ApiModel(value = "定点机台对象", description = "定点机台对象 ")
@Data
@TableName(value = "T_CX_SPECIFY_MACHINE")
@ToString
public class CxSpecifyMachine {

    private static final long serialVersionUID = 1L;

    @TableId(
            value = "ID",
            type = IdType.INPUT
    )
    private Long id;

    /**
     * SAP品号
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.specifyMachine.sapCode")
    @ApiModelProperty(value = "SAP品号")
    @TableField(value = "SAP_CODE")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.specifyMachine.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    @TableField(value = "EMBRYO_CODE")
    private String embryoCode;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.machine.machineName")
    @TableField(exist = false)
    private String machineName;

    /**
     * 线路
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路")
    @TableField(value = "LINE_TYPE")
    private String lineType;

    /**
     * 作业类型
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.jobType", dictType = "JOB_TYPE")
    @ApiModelProperty(value = "作业类型")
    @TableField(value = "JOB_TYPE")
    private String jobType;


    @ApiModelProperty("创建者")
    @TableField(
            value = "CREATE_BY",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.VARCHAR
    )
    private String createBy;
    @ApiModelProperty("创建时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    @TableField(
            value = "CREATE_TIME",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP
    )
    private Date createTime;
    @ApiModelProperty("更新者")
    @TableField(
            value = "UPDATE_BY",
            fill = FieldFill.INSERT_UPDATE,
            jdbcType = JdbcType.VARCHAR
    )
    private String updateBy;
    @ApiModelProperty("更新时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    @TableField(
            value = "UPDATE_TIME",
            fill = FieldFill.INSERT_UPDATE,
            jdbcType = JdbcType.TIMESTAMP
    )
    private Date updateTime;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "删除标识：0--正常，1-删除", position = 600)
    @TableField("DEL_FLAG")
    private String delFlag;
//    @Override
//    public String toString() {
//        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
//                .append("id", getId())
//                .append("sapCode", getSapCode())
//                .append("embryoCode", getEmbryoCode())
//                .append("machineCode", getMachineCode())
//                .append("lineType", getLineType())
//                .append("jobType", getJobType())
//                .append("createBy", getCreateBy())
//                .append("createTime", getCreateTime())
//                .append("updateBy", getUpdateBy())
//                .append("updateTime", getUpdateTime())
//                .append("delFlag", getDelFlag())
//                .append("remark", getRemark())
//                .toString();
//    }

    /**
     * 根据id是否为空给创建时间，创建人，更新时间，更新人赋值
     */
    public void setBaseVale(Long id) {
        try {
            if(id == null) {
                //id为空，表示为新增操作
                this.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
                this.setCreateBy(SecurityUtils.getUsername());
                this.setCreateTime(new Date());
            } else {
                //更新操作
                this.setUpdateBy(SecurityUtils.getUsername());
                this.setUpdateTime(new Date());
            }
        } catch (Exception e) {
            if (id == null) {
                //id为空，表示为新增操作
                this.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
                this.setCreateBy("system");
                this.setCreateTime(new Date());
            } else {
                //更新操作
                this.setUpdateBy("system");
                this.setUpdateTime(new Date());
            }
        }
    }
}
