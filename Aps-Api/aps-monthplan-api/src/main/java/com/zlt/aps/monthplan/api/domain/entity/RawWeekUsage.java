package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 周维度原材料用量记录
 */
@ApiModel(value = "周维度原材料用量记录", description = "周维度原材料用量记录")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_RAW_WEEK_USAGE")
public class RawWeekUsage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 工厂编码
     */
    @Excel(name = "工厂编码")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "年份")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 周次
     */
    @Excel(name = "周次")
    @ApiModelProperty(value = "周次", name = "week")
    @TableField(value = "WEEK")
    private Integer week;

    /**
     * 原材料编码
     */
    @Excel(name = "原材料编码")
    @ApiModelProperty(value = "原材料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 原材料名称
     */
    @Excel(name = "原材料名称")
    @ApiModelProperty(value = "原材料名称", name = "materialName")
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

    /**
     * 计划用量
     */
    @Excel(name = "计划用量")
    @ApiModelProperty(value = "计划用量", name = "planQty")
    @TableField(value = "PLAN_QTY")
    private BigDecimal planQty;

    /**
     * 实际用量（从MES获取）
     */
    @Excel(name = "实际用量")
    @ApiModelProperty(value = "实际用量", name = "actualQty")
    @TableField(value = "ACTUAL_QTY")
    private BigDecimal actualQty;

    /**
     * 偏差量
     */
    @Excel(name = "偏差量")
    @ApiModelProperty(value = "偏差量", name = "deviationQty")
    @TableField(value = "DEVIATION_QTY")
    private BigDecimal deviationQty;

    /**
     * 偏差率
     */
    @Excel(name = "偏差率")
    @ApiModelProperty(value = "偏差率", name = "deviationRate")
    @TableField(value = "DEVIATION_RATE")
    private BigDecimal deviationRate;

    /**
     * 是否预警：0-否 1-是
     */
    @Excel(name = "是否预警", dictType = "sys_yes_no")
    @ApiModelProperty(value = "是否预警", name = "hasWarning")
    @TableField(value = "HAS_WARNING")
    private Integer hasWarning;

    /**
     * 预警级别
     */
    @Excel(name = "预警级别", dictType = "warning_level")
    @ApiModelProperty(value = "预警级别", name = "warningLevel")
    @TableField(value = "WARNING_LEVEL")
    private String warningLevel;

    /**
     * 用量日期范围开始
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "开始日期", name = "startDate")
    @TableField(value = "START_DATE")
    private Date startDate;

    /**
     * 用量日期范围结束
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "结束日期", name = "endDate")
    @TableField(value = "END_DATE")
    private Date endDate;

    /**
     * 计算偏差量和偏差率
     */
    public void calculateDeviation() {
        if (planQty != null && actualQty != null && planQty.compareTo(BigDecimal.ZERO) != 0) {
            this.deviationQty = actualQty.subtract(planQty);
            this.deviationRate = deviationQty.divide(planQty, 4, BigDecimal.ROUND_HALF_UP);
        } else {
            this.deviationQty = BigDecimal.ZERO;
            this.deviationRate = BigDecimal.ZERO;
        }
    }

    /**
     * 检查是否需要预警
     * @param config 预警配置
     * @return 是否需要预警
     */
    public boolean checkWarning(RawWarningConfig config) {
        if (config == null || config.getEnabled() != 1) {
            return false;
        }

        if (deviationRate != null) {
            BigDecimal upper = config.getDeviationUpper();
            BigDecimal lower = config.getDeviationLower();

            if (upper != null && deviationRate.compareTo(upper) > 0) {
                return true;
            }
            if (lower != null && deviationRate.compareTo(lower) < 0) {
                return true;
            }
        }
        return false;
    }
}