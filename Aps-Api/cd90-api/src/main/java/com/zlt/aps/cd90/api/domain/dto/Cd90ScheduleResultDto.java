package com.zlt.aps.cd90.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 90度裁断排程结果表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-23
 */
@Data
@ApiModel(value = "Cd90ScheduleResult对象", description = "90度裁断排程结果表")
public class Cd90ScheduleResultDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate")
    private LocalDateTime scheduleDate;

//    @ApiModelProperty(value = "对应的成型批次号")
//    @Excel(name="ui.data.column.scheduleResult.moldingBatchNumber")
//    private String cxBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

//    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
//    @TableField("ORDER_NO")
//    private String orderNo;

    @ApiModelProperty(value = "帘布大卷编号")
    @Excel(name = "ui.bigRollColor.column.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "帘布代码")
    @Excel(name = "ui.xwyy.specifyMachine.column.bigRollCod")
    private String clothCode;

    @ApiModelProperty(value = "单耗")
    @Excel(name = "ui.data.column.scheduleResult.unitConsume")
    private BigDecimal unitConsume;

    @ApiModelProperty(value = "机台ID，多个逗号分割")
    private String machineId;

    @ApiModelProperty(value = "月计划需求量")
    @Excel(name = "ui.data.column.scheduleResult.monthPlan")
    private String monthPlanQty;

    @ApiModelProperty(value = "月计划剩余量")
    @Excel(name = "ui.data.column.scheduleResult.monthPlanOs")
    private String monthRemainQty;

    @ApiModelProperty(value = "库存数量")
    @Excel(name = "ui.data.column.scheduleResult.stockQty")
    private Integer stockQty;

    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    @Excel(name = "ui.data.column.scheduleResult.supplyTime")
    private BigDecimal supplyTime;

    @ApiModelProperty(value = "中班(12点-24点)计划量")
    @Excel(name = "ui.data.column.scheduleResult.plan")
    private Integer dayPlanQty;

    @ApiModelProperty(value = "中班(12点-24点)完成量")
    @Excel(name = "ui.data.column.scheduleResult.finish")
    private Integer dayFinishQty;

    @ApiModelProperty(value = "中班(12点-24点)生产顺序")
    @Excel(name = "ui.data.column.scheduleResult.produceOrder")
    private Integer dayProduceOrder;

    @ApiModelProperty(value = "中班(12点-24点)完成率")
    @Excel(name = "ui.data.column.scheduleResult.finishRate")
    private BigDecimal dayFinishRate;

    @ApiModelProperty(value = "中班(12点-24点)系统原因分析")
    private String daySysAnalysis;

    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析")
    private String dayHandAnalysis;

    @ApiModelProperty(value = "中班(12点-24点)原因分析")
    @Excel(name = "ui.data.column.scheduleResult.analysis")
    private String datAnalysis;

    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    @Excel(name = "ui.data.column.scheduleResult.plan")
    private Integer nightPlanQty;

    @ApiModelProperty(value = "夜班(0点-12点)完成量")
    @Excel(name = "ui.data.column.scheduleResult.finish")
    private Integer nightFinishQty;

    @ApiModelProperty(value = "夜班(0点-12点)生产顺序")
    @Excel(name = "ui.data.column.scheduleResult.produceOrder")
    private Integer nightProduceOrder;

    @ApiModelProperty(value = "夜班(0点-12点)完成率")
    @Excel(name = "ui.data.column.scheduleResult.finishRate")
    private BigDecimal nightFinishRate;

    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析")
    private String nightSysAnalysis;

    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "夜班(0点-12点)原因分析")
    @Excel(name = "ui.data.column.scheduleResult.analysis")
    private String nightAnalysis;

    @ApiModelProperty(value = "对应成型一班的计划量")
    @Excel(name = "ui.data.column.scheduleResult.cxClass1Plan")
    private Integer cxClass1Plan;

    @ApiModelProperty(value = "对应成型二班的计划量")
    @Excel(name = "ui.data.column.scheduleResult.cxClass2Plan")
    private Integer cxClass2Plan;

    @ApiModelProperty(value = "对应成型三班的计划量")
    @Excel(name = "ui.data.column.scheduleResult.cxClass3Plan")
    private Integer cxClass3Plan;

    @ApiModelProperty(value = "对应成型次一班的计划量")
    @Excel(name = "ui.data.column.scheduleResult.cxClass4Plan")
    private Integer cxClass4Plan;

    @ApiModelProperty(value = "对应成型次二班的计划量")
    @Excel(name = "ui.data.column.scheduleResult.cxClass5Plan")
    private Integer cxClass5Plan;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

}
