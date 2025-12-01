package com.zlt.aps.monthplan.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.bytebuddy.implementation.bind.annotation.FieldValue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 分厂月生产计划排产结果-最终版本(包含调整单调整的结果)
 *
 * @author ZLT
 * @data 20250214
 */
@Data
public class FactoryMonthPlanProdFinalQueryDto implements Serializable {
    /**
     * 排产日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排产日", name = "productionDate")
    private Date productionDate;
    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 胎别
     */
    @ApiModelProperty(value = "胎别", name = "productTypeCode")
    private String productTypeCode;

    /**
     * 销售生产需求计划版本
     */
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 分厂排产版本
     */
    @ApiModelProperty(value = "分厂排产版本", name = "productionVersion")
    private String productionVersion;
    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    private String productCode;

    /**
     * 规格描述
     */
    @ApiModelProperty(value = "规格描述", name = "productDesc")
    private String productDesc;

    /**
     * 库位类别
     */
    @ApiModelProperty(value = "库位类别", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "channel")
    private String brand;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;
    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /**
     * 是否跨月
     */
    @ApiModelProperty(value = "是否跨月", name = "crossMonth")
    @TableField(exist = false)
    private Boolean crossMonth;
}