package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 月度计划外胎汇总明细对象
 *
 * @author Liam
 * @since 2025/4/2
 */
@Data
@TableName(value = "T_LH_MONTH_PLAN_SURPLUS_DETAIL")
public class LhMonthPlanSurplusDetail extends BaseEntity {
    
    /**
     * 月度计划版本
     */
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 月度计划所属年份
     */
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月度计划所属月份
     */
    @TableField(value = "MONTH")
    private Integer Month;

    /**
     * 分厂编号
     */
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 物料编号
     */
    @TableField(value = "PRODUCT_CODE")
    private String productCode;

    /**
     * 规格代码
     */
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /**
     * 库位类别
     */
    @TableField(value = "LOCATION_TYPE")
    private String locationType;

    /**
     * 月度计划最小的起始日（年月、分厂、物料、规格、库位）
     */
    @TableField(value = "START_DATE")
    private Integer startDate;

    /**
     * 月度计划最大的结束日（年月、分厂、物料、规格、库位）
     */
    @TableField(value = "END_DATE")
    private Integer endDate;

    /**
     * 总生产量（月计划的年月、分厂、物料、规格、库位汇总）
     */
    @TableField(value = "PRODUCTION_QTY")
    private Long productionQty;

    /**
     * 总完成量（先OE，再外销，最后内销）
     */
    @TableField(value = "COMPLETE_QTY")
    private Long completeQty;

    /**
     * 排程时间
     */
    @TableField(value = "SCHEDULE_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleTime;

    /**
     * 生胎代码
     */
    @TableField(exist = false)
    private String embryoCode;

    /**
     * 品牌
     */
    @TableField(exist = false)
    private String brand;

    /**
     * 规格描述
     */
    @TableField(exist = false)
    private String specDesc;

    /**
     * 模具号
     */
    @TableField(exist = false)
    private String mouldNo;

}
