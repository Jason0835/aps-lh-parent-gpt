package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 垫胶定点机台表
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_DJ_SPECIFY_MACHINE")
public class DjSpecifyMachine extends BaseEntity{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "垫胶代码")
    @TableField("PADDING_CODE")
    private String paddingCode;

    @ApiModelProperty(value = "机台编码（对应T_NC_MACHINE_INFO表code）")
    @TableField("MACHINE_CODE")
    private Long machineCode;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @TableField("LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @TableField("JOB_TYPE")
    private String jobType;
}
