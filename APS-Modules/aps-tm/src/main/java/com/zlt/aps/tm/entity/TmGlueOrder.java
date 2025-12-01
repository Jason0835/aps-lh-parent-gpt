package com.zlt.aps.tm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 胎面胶料顺序维护
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TM_GLUE_ORDER")
@ApiModel(value = "TmGlueOrder对象", description = "胎面胶料顺序维护")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class TmGlueOrder extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "胶料组别id，对应TM_GLUE_GROUP_ORDER表主键id")
    @TableField("GLUE_GROUP_ID")
    private Long glueGroupId;

    @ApiModelProperty(value = "胶料编号")
    @TableField("GLUE_CODE")
    private String glueCode;

    @ApiModelProperty(value = "生产顺序")
    @TableField("ORDER_NUM")
    private Integer orderNum;

    @ApiModelProperty(value = "机台ID")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;


}
