package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 硫化日计划调整需求实体。
 */
@Data
@TableName("t_lh_day_plan_adjust_require")
@ApiModel(value = "硫化日计划调整需求", description = "硫化日计划调整需求及列表行转列对象")
public class LhDayPlanAdjustRequire extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @TableField("FACTORY_CODE")
    @ApiModelProperty("工厂编号")
    private String factoryCode;

    /** 年份 */
    @TableField("YEAR")
    @ApiModelProperty("年份")
    private Integer year;

    /** 月份 */
    @TableField("MONTH")
    @ApiModelProperty("月份")
    private Integer month;

    /** 年月，格式YYYYMM */
    @TableField("`YEAR_MONTH`")
    @ApiModelProperty("年月，格式YYYYMM")
    private Integer yearMonth;

    /** 产品状态 */
    @TableField("PRODUCT_STATUS")
    @ApiModelProperty("产品状态")
    private String productStatus;

    /** NC物料编码 */
    @TableField("MATERIAL_CODE")
    @ApiModelProperty("NC物料编码")
    private String materialCode;

    /** 物料描述 */
    @TableField("MATERIAL_DESC")
    @ApiModelProperty("物料描述")
    private String materialDesc;

    /** MES物料编码 */
    @TableField("MES_MATERIAL_CODE")
    @ApiModelProperty("MES物料编码")
    private String mesMaterialCode;

    /** 调整序号，仅允许1、2、3 */
    @TableField("ADJUST_COUNT")
    @ApiModelProperty("调整序号")
    private Integer adjustCount;

    /** 调整量，可正可负 */
    @TableField("PLAN_QTY")
    @ApiModelProperty("调整量")
    private BigDecimal planQty;

    /** 调整原因 */
    @TableField("REASON")
    @ApiModelProperty("调整原因")
    private String reason;

    /** 调整人 */
    @TableField("ADJUSTER")
    @ApiModelProperty("调整人")
    private String adjuster;

    /** 当前有效排产版本 */
    @TableField(exist = false)
    @ApiModelProperty("当前有效排产版本")
    private String productionVersion;

    /** 月计划量 */
    @TableField(exist = false)
    @ApiModelProperty("月计划量")
    private BigDecimal monthPlanQty;

    /** 调整1记录ID */
    @TableField(exist = false)
    private Long adjustId1;

    /** 调整2记录ID */
    @TableField(exist = false)
    private Long adjustId2;

    /** 调整3记录ID */
    @TableField(exist = false)
    private Long adjustId3;

    /** 调整1数量 */
    @TableField(exist = false)
    private BigDecimal adjustQty1;

    /** 调整2数量 */
    @TableField(exist = false)
    private BigDecimal adjustQty2;

    /** 调整3数量 */
    @TableField(exist = false)
    private BigDecimal adjustQty3;

    /** 调整1原因 */
    @TableField(exist = false)
    private String adjustReason1;

    /** 调整2原因 */
    @TableField(exist = false)
    private String adjustReason2;

    /** 调整3原因 */
    @TableField(exist = false)
    private String adjustReason3;

    /** 调整1修改人 */
    @TableField(exist = false)
    private String adjuster1;

    /** 调整2修改人 */
    @TableField(exist = false)
    private String adjuster2;

    /** 调整3修改人 */
    @TableField(exist = false)
    private String adjuster3;

    /** 调整1修改时间 */
    @TableField(exist = false)
    private Date adjustTime1;

    /** 调整2修改时间 */
    @TableField(exist = false)
    private Date adjustTime2;

    /** 调整3修改时间 */
    @TableField(exist = false)
    private Date adjustTime3;

    /** 调整后合计 */
    @TableField(exist = false)
    @ApiModelProperty("调整后合计")
    private BigDecimal adjustedTotalQty;

    /** 胎面胶TD */
    @TableField(exist = false)
    @ApiModelProperty("胎面胶TD")
    private String treadGlueTd;

    /** 月计划显示顺序 */
    @TableField(exist = false)
    private Integer displaySeq;

    /** 页码 */
    @TableField(exist = false)
    private Integer pageNum;

    /** 每页条数 */
    @TableField(exist = false)
    private Integer pageSize;
}
