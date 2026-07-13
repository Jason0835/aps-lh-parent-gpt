package com.zlt.aps.nc.api.domain.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 内衬参数信息
 * </p>
 *
 * @author zlt
 * @since 2026-07-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_PARAMS")
@ApiModel(value = "NcParams对象", description = "内衬参数信息")
public class NcParams extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "ui.dj.params.column.factoryCode")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 产品品类
     */
    @Excel(name = "ui.dj.params.column.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 参数编码，编码规则：SYS+工序号+分组号+三位流水号，其中内衬的工序号为14，分组号暂时固定为01
     */
    @Excel(name = "ui.dj.params.column.paramCode")
    @ApiModelProperty(value = "参数编码", name = "paramCode")
    @TableField(value = "PARAM_CODE")
    private String paramCode;

    /**
     * 参数名称
     */
    @Excel(name = "ui.dj.params.column.paramName")
    @ApiModelProperty(value = "参数名称", name = "paramName")
    @TableField(value = "PARAM_NAME")
    private String paramName;

    /**
     * 业务分组--有业务关联的参数一个组
     */
    @ApiModelProperty(value = "业务分组", name = "businessGroup")
    @TableField(value = "BUSINESS_GROUP")
    private String businessGroup;

    /**
     * 数据类型: 0-字符型 1-整型 2-数值型 3-日期型 4-时间型 5-日期时间型 6-布尔型
     */
    @Excel(name = "ui.dj.params.column.dataType")
    @ApiModelProperty(value = "数据类型", name = "dataType")
    @TableField(value = "DATA_TYPE")
    private Integer dataType;

    /**
     * 默认值
     */
    @Excel(name = "ui.dj.params.column.defauleValue")
    @ApiModelProperty(value = "默认值", name = "defauleValue")
    @TableField(value = "DEFAULE_VALUE")
    private String defauleValue;

    /**
     * 参数值
     */
    @Excel(name = "ui.dj.params.column.paramValue")
    @ApiModelProperty(value = "参数值", name = "paramValue")
    @TableField(value = "PARAM_VALUE")
    private String paramValue;
}
