package com.zlt.aps.cx.engine.domain;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
  * 成型工序计划量汇总对象
  * @ClassName CxEngineMonthPlanSurplus
  * @Author Joran.Zhang
  * @Date 2021/6/23 14:51
  * @Version 1.0
**/
@Data
@ApiModel(value = "成型工序外胎计划量汇总对象", description = "成型工序外胎计划量汇总对象 ")
public class CxEngineMonthPlanSurplus extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 生产排程记录主计划版本号,年+月+日+01，02 */
    @Excel(name = "生产排程记录主计划版本号,年+月+日+01，02")
    @ApiModelProperty(value = "生产排程记录主计划版本号,年+月+日+01，02")
    private String monthPlanApsVersion;

    /** 月度计划版本 */
    @Excel(name = "月度计划版本")
    @ApiModelProperty(value = "月度计划版本")
    private String monthPlanVersion;

    /** 月度计划所属年份 */
    @Excel(name = "月度计划所属年份")
    @ApiModelProperty(value = "月度计划所属年份")
    private String year;

    /** 月度计划所属月份 */
    @Excel(name = "月度计划所属月份")
    @ApiModelProperty(value = "月度计划所属月份")
    private String month;

    /** SAP品号 */
    @Excel(name = "SAP品号")
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /** 月度计划量 */
    @Excel(name = "月度计划量")
    @ApiModelProperty(value = "月度计划量")
    private Integer monthPlanQty;

    /** 成型计划调整量 */
    @Excel(name = "成型计划调整量")
    @ApiModelProperty(value = "成型计划调整量")
    private Integer planModifyQty;

    /** 月度胎胚完成量 */
    @Excel(name = "月度胎胚完成量")
    @ApiModelProperty(value = "月度胎胚完成量")
    private Integer monthFinishQty;

    /** 成型月剩余量 */
    @Excel(name = "成型月剩余量")
    @ApiModelProperty(value = "成型月剩余量")
    private Integer monthRemainQty;

    @ApiModelProperty(value = "数据来源：0：主计划；1:APS插单；插单数据主计划版本更新不进行删除")
    private String dataSource;

    /**
     * 成型批次号，检索对应排程计划版本号条件
     */
    private String cxBatchNo;

    /**
     * 是否收尾标识
     */
    private Boolean isCloseOut;

    /**
     * 是否标识收尾提示
     */
    private Boolean markCloseOutTip;

    /**
     * 月结库存
     */
    private Integer lastMonthStock;

    /**
     * 胎胚不良数
     */
    private Integer sapBadQty;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * Joran 2021-09-19 用来更新插单数据属性
     */
    private Integer updateInsertQty;

}
