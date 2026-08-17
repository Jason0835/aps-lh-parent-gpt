package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 月计划与实际产量对比报表结果VO
 * <p>
 * 每个SKU对应4行VO（rowType区分）：
 * plan   - 月计划
 * actual - 实际产量
 * diff   - 差异（实际-计划）
 * rate   - 完成率（实际/计划，百分比）
 * </p>
 *
 * @author APS
 * @date 2026-08-13
 */
@Data
@ApiModel(value = "月计划与实际产量对比报表结果VO", description = "月计划与实际产量对比报表结果VO")
public class MonthPlanCompareVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;

    /**
     * 行类型：plan-月计划, actual-实际产量, diff-差异, rate-完成率
     */
    @ApiModelProperty(value = "行类型", name = "rowType")
    private String rowType;

    /**
     * 行类型标签（月计划/实际产量/差异/完成率），用于前端展示
     */
    @ApiModelProperty(value = "行类型标签", name = "rowTypeLabel")
    private String rowTypeLabel;

    /**
     * 合计值
     */
    @ApiModelProperty(value = "合计值", name = "totalQty")
    private BigDecimal totalQty;

    /**
     * 每日数据列表
     * <p>index=0 对应当月1号，index=n-1 对应当月n号，长度=当月天数</p>
     */
    @ApiModelProperty(value = "每日数据列表", name = "dayQtyList")
    private List<BigDecimal> dayQtyList;
}
