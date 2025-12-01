package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@ApiModel(value = "成型排程硫化机台关系表", description = "成型排程硫化机台关系表 ")
@Data
@TableName("T_CX_CHANGE_LH_MACHINE")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_CX_CHANGE_LH_MACHINE",dbType = DbType.ORACLE)
public class CxChangeLhMachine extends ApsBaseEntity {
    /** 主键ID，对应自增序列为：SEQ_CX_CHANGE_LH_MACHINE */
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "成型工单号")
    private String cxOrderNo;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 排程日期检索条件
     */
    private String scheduleDateStr;

    /**
     * 硫化机台编号
     */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 用来展示硫化机台拼接名称
     */
    @ApiModelProperty(value = "用来展示硫化机台拼接名称")
    private String  lhMachineNames;

    /**
     * 变更类型：1： 拆模换、2：点数换、3：合并收尾、4：拆模合并、5：左模收尾合并、6：右模收尾合并、
     */
    @ApiModelProperty(value = "变更类型")
    private String changeType;

    @ApiModelProperty(value = "胎胚库存")
    private Integer embryoStock;

    /**
     * 换模时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm",timezone = "GMT+8")
    @ApiModelProperty(value = "换模时间")
    private Date changeMoldTime;

    @ApiModelProperty(value = "使用模数")
    private Integer useMoldNum;

    /**
     * 变更类型：0：成型排程、1：增补计划'
     */
    @ApiModelProperty(value = "数据来源")
    private String dataSource;
}
