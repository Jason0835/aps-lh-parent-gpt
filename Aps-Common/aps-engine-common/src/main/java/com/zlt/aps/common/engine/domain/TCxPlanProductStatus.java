package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型计划投产状态表
 * @TableName T_CX_PLAN_PRODUCT_STATUS
 */
@Data
@ApiModel(value="成型月度计划投产状态信息表", description="成型月度计划投产状态信息表")
public class TCxPlanProductStatus extends ApsBaseEntity {
    private  static final  long serialVersionUID=1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 生产排程记录主计划版本号,年+月+日+01，02 */
    @ApiModelProperty(value = "生产排程记录主计划版本号,年+月+日+01，02")
    private String monthPlanApsVersion;

    @ApiModelProperty(value = "月度计划明细ID串,多条记录用,分隔")
    private String monthPlanIds;

    /** SAP品号 */
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /** 成型胎胚代码 */
    @ApiModelProperty(value = "成型胎胚代码")
    private String embryoCode;

    /** 规格寸口 */
    @ApiModelProperty(value = "规格寸口")
    private Double specDimension;

    /** 计划总量，不同库位的同SAP+胎胚代码 进行合并汇总 */
    @ApiModelProperty(value = "计划总量")
    private Integer monthPlanTotalQty;

    /** 开始时间序号 */
    @ApiModelProperty(value = "开始时间格式yyyyMMdd")
    private String beginDate;

    /** 结束时间序号 */
    @ApiModelProperty(value = "结束时间格式yyyyMMdd")
    private String endDate;

    /** 投产状态：0，未投产；1，已投产； */
    @ApiModelProperty(value = "投产状态：0，未投产；1，已投产；2：待发布；")
    private  String productStatus;

    /** 标记不投产，0:正常；1：标记不投产 */
    @ApiModelProperty(value = "标记不投产，0:正常；1：标记不投产；")
    private String markUnProduct;

    /** 投产明细描述 */
    @ApiModelProperty(value = "投产明细描述")
    private String productDetail;

    /** 施工信息版本 */
    @ApiModelProperty(value = "施工信息版本")
    private String bomDataVersion;

    /** 月度计划表特殊要求 */
    @ApiModelProperty(value = "月度计划表特殊要求")
    private String specialRequirements;
}