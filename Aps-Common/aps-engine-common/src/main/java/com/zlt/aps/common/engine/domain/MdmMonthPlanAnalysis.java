package com.zlt.aps.common.engine.domain;

import java.math.BigDecimal;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 
 * @TableName T_MDM_MONTH_PLAN_ANALYSIS
 */
@Data
public class MdmMonthPlanAnalysis extends ApsBaseEntity {
    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", position = 500)
    private Long id;

    /**
     * 生产排程计划版本号
     */
    @ApiModelProperty(value = "生产排程计划版本号", position = 500)
    private String monthPlanApsVersion;

    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", position = 500)
    private String materialCode;

    /**
     * 胎胚代码
     */
    @ApiModelProperty(value = "胎胚代码", position = 500)
    private String embryoCode;

    /**
     * 质量等级
     */
    @ApiModelProperty(value = "质量等级", position = 500)
    private String qualityGrade;

    /**
     * 库存地点
     */
    @ApiModelProperty(value = "库存地点", position = 500)
    private String storageLocation;

    /**
     * 特殊要求
     */
    @ApiModelProperty(value = "特殊要求", position = 500)
    private String specialRequirements;

    /**
     * 实际安排
     */
    @ApiModelProperty(value = "实际安排", position = 500)
    private Integer actualArrangement = 0;

    /**
     * 成型胎胚月度计划量
     */
    @ApiModelProperty(value = "成型胎胚月度计划量", position = 500)
    private Integer cxMonthPlanQty = 0;

    /**
     * 胎面代码
     */
    @ApiModelProperty(value = "胎面代码", position = 500)
    private String tmCode;

    /**
     * 胎面月度计划量
     */
    @ApiModelProperty(value = "胎面月度计划量", position = 500)
    private Double tmMonthPlanQty = 0d;

    /**
     * 胎侧代码
     */
    @ApiModelProperty(value = "胎侧代码", position = 500)
    private String tcCode;

    /**
     * 胎侧月度计划量
     */
    @ApiModelProperty(value = "胎侧月度计划量", position = 500)
    private BigDecimal tcMonthPlanQty = BigDecimal.ZERO;

    /**
     * 内衬代码
     */
    @ApiModelProperty(value = "内衬代码", position = 500)
    private String ncCode;

    /**
     * 内衬
     */
    @ApiModelProperty(value = "内衬", position = 500)
    private BigDecimal ncMonthPlanQty = BigDecimal.ZERO;

    /**
     * 胎圈代码
     */
    @ApiModelProperty(value = "胎圈代码", position = 500)
    private String tqCode;

    /**
     * 胎圈
     */
    @ApiModelProperty(value = "胎圈", position = 500)
    private BigDecimal tqMonthPlanQty = BigDecimal.ZERO;

    /**
     * 钢丝圈代码
     */
    @ApiModelProperty(value = "钢丝圈代码", position = 500)
    private String gsqCode;

    /**
     * 钢丝圈
     */
    @ApiModelProperty(value = "钢丝圈", position = 500)
    private BigDecimal gsqMonthPlanQty = BigDecimal.ZERO;

//    /**
//     * 15度裁断代码
//     */
//    private String cd15Code;

    /**
     * 15度裁断
     */
    @ApiModelProperty(value = "15度裁断", position = 500)
    private BigDecimal gdcdMonthPlanQty = BigDecimal.ZERO;

    /**
     * 15度裁断1#代码
     */
    @ApiModelProperty(value = "15度裁断1#代码", position = 500)
    private String cd15OneCode;

    /**
     * 15度裁断1#量
     */
    @ApiModelProperty(value = "15度裁断1#量", position = 500)
    private BigDecimal cd15OneMonthPlanQty = BigDecimal.ZERO;

    /**
     * 15度裁断2#代码
     */
    @ApiModelProperty(value = "15度裁断2#代码", position = 500)
    private String cd15TwoCode;

    /**
     * 15度裁断2#量
     */
    @ApiModelProperty(value = "15度裁断2#量", position = 500)
    private BigDecimal cd15TwoMonthPlanQty = BigDecimal.ZERO;

//    /**
//     * 90度裁断代码
//     */
//    private String lbcdCode;

    /**
     * 90度裁断
     */
    @ApiModelProperty(value = "90度裁断", position = 500)
    private BigDecimal lbcdMonthPlanQty = BigDecimal.ZERO;

    /**
     * 90度裁断1#代码
     */
    @ApiModelProperty(value = "90度裁断1#代码", position = 500)
    private String cd90OneCode;

    /**
     * 90度裁断1#量
     */
    @ApiModelProperty(value = "90度裁断1#量", position = 500)
    private BigDecimal cd90OneMonthPlanQty = BigDecimal.ZERO;

    /**
     * 90度裁断2#代码
     */
    @ApiModelProperty(value = "90度裁断2#代码", position = 500)
    private String cd90TwoCode;

    /**
     * 90度裁断2#量
     */
    @ApiModelProperty(value = "90度裁断2#量", position = 500)
    private BigDecimal cd90TwoMonthPlanQty = BigDecimal.ZERO;

    /**
     * 90度裁断3#代码
     */
    @ApiModelProperty(value = "90度裁断3#代码", position = 500)
    private String cd90ThreeCode;

    /**
     * 90度裁断3#量
     */
    @ApiModelProperty(value = "90度裁断3#量", position = 500)
    private BigDecimal cd90ThreeMonthPlanQty = BigDecimal.ZERO;

    /**
     * 钢带压延代码
     */
    @ApiModelProperty(value = "钢带压延代码", position = 500)
    private String gdyyCode;

    /**
     * 钢带压延（米）
     */
    @ApiModelProperty(value = "钢带压延（米）", position = 500)
    private BigDecimal gdyyMonthPlanQty = BigDecimal.ZERO;

    /**
     * 钢带压延（个数）
     */
    @ApiModelProperty(value = "钢带压延（个数）", position = 500)
    private Integer gdyyMonthPlanNumQty = 0;

    /**
     * 纤维压延代码
     */
    @ApiModelProperty(value = "纤维压延代码", position = 500)
    private String xwyyCode;

    /**
     * 纤维压延（米）
     */
    @ApiModelProperty(value = "纤维压延（米）", position = 500)
    private BigDecimal xwyyMonthPlanQty = BigDecimal.ZERO;

    /**
     * 纤维压延（个数）
     */
    @ApiModelProperty(value = "纤维压延（个数）", position = 500)
    private Integer xwyyMonthPlanNumQty = 0;

    /**
     * 施工信息版本
     */
    @ApiModelProperty(value = "施工信息版本", position = 500)
    private String bomDataVersion;

    private static final long serialVersionUID = 1L;

}