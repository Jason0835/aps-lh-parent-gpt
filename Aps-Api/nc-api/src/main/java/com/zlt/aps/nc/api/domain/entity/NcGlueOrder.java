package com.zlt.aps.nc.api.domain.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 胎侧胶料顺序维护
 * </p>
 *
 * @author zhangbinglin
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_GLUE_ORDER")
@ApiModel(value = "NcGlueOrder对象", description = "胎侧胶料顺序维护")
public class NcGlueOrder extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "胶料组别id，对应NC_GLUE_GROUP_ORDER表主键id")
    @TableField("GLUE_GROUP_ID")
    private Long glueGroupId;

    @ApiModelProperty(value = "胶料编号")
    @TableField("GLUE_CODE")
    private String glueCode;

    @ApiModelProperty(value = "生产顺序")
    @TableField("ORDER_NUM")
    private Integer orderNum;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;
}
