package com.zlt.aps.gdyy.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 钢带压延排程结果对象 t_gdyy_schedule_result
 *
 * @author chen
 * @date 2021-07-05
 */
@Data
@EqualsAndHashCode()
@ApiModel(value = "钢带压延排程结果对象", description = "钢带压延排程结果对象 ")
public class GdyyScheduleResultDto2 extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_GDYY_SCHEDULE
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 排程日期
     */
    @ImportValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 对应的15度裁断批次号
     */
    @ApiModelProperty(value = "对应的15度裁断批次号")
    private String cd15BatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号，自动生成")
    private String orderNo;

    /**
     * 钢带大卷编号
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.gdyy.scheduleResult.bigRollCode2")
    @ApiModelProperty(value = "钢压大卷代号")
    private String bigRollCode;

    /**
     * 日用参考（个）
     */
    @ApiModelProperty(value = "日用参考")
    private Double dayUsed;

    /**
     * 生产线
     */
    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线")
    private String machineCode;

    /**
     * 库存（个）
     */
    @ImportValidated(number = true, min = 0, max = 99999999)
    @Excel(name = "ui.data.column.gdyy.scheduleResult.stockQty")
    @ApiModelProperty(value = "库存")
    private Double stockQty;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQtyNum")
    @ApiModelProperty(value = "当日合计个数")
    private Double dailyTotalQtyNum;

    /**
     * 中班（一班16点-24点）计划量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.plan.meter")
    @ApiModelProperty(value = "中班计划量")
    private Double class1Plan;

    /**
     * 中班计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.classPlanNum")
    @ApiModelProperty(value = "中班计划量个数")
    private Double class1PlanNum;

    /**
     * 中班（一班16点-24点）无库存计划量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.planNoStock.meter")
    @ApiModelProperty(value = "中班无库存计划量")
    private Double class1PlanNoStock;

    /**
     * 中班无库存计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.classPlanNoStockNum")
    @ApiModelProperty(value = "中班无库存计划量个数")
    private Double class1PlanNoStockNum;

    /**
     * 中班（一班16点-24点）支领量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.collar.meter")
    @ApiModelProperty(value = "中班支领量")
    private Double class1Finish;

    /**
     * 中班（一班16点-24点）备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "中班备注")
    private String class1Remark;

    /**
     * 夜班（二班0点-8点）计划量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.planMeter4")
    @ApiModelProperty(value = "夜班计划量")
    private Double class2Plan;

    /**
     * 夜班计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.classPlanNum")
    @ApiModelProperty(value = "夜班计划量个数")
    private Double class2PlanNum;

    /**
     * 夜班（二班0点-8点）无库存计划量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.planNoStockMeter4")
    @ApiModelProperty(value = "夜班无库存计划量")
    private Double class2PlanNoStock;

    /**
     * 夜班无库存计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.classPlanNoStockNum")
    @ApiModelProperty(value = "夜班无库存计划量个数")
    private Double class2PlanNoStockNum;

    /**
     * 夜班（二班0点-8点）支领量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.collar.meter2")
    @ApiModelProperty(value = "夜班支领量")
    private Double class2Finish;

    /**
     * 夜班（二班0点-8点）备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark2")
    @ApiModelProperty(value = "夜班备注")
    private String class2Remark;

    /**
     * 白班（三班8点-16点）计划量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.planMeter5")
    @ApiModelProperty(value = "白班计划量")
    private Double class3Plan;

    /**
     * 白班无库存计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.classPlanNum")
    @ApiModelProperty(value = "白班计划量个数")
    private Double class3PlanNum;

    /**
     * 白班（三班8点-16点）无库存计划量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.planNoStockMeter5")
    @ApiModelProperty(value = "白班计划量")
    private Double class3PlanNoStock;

    /**
     * 白班无库存计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.classPlanNoStockNum")
    @ApiModelProperty(value = "白班计划量个数")
    private Double class3PlanNoStockNum;

    /**
     * 白班（三班8点-16点）支领量
     */
    @ImportValidated(number = true, min = 0, max = 99999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.collar.meter3")
    @ApiModelProperty(value = "白班支领量")
    private Double class3Finish;

    /**
     * 白班（三班8点-16点）备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark3")
    @ApiModelProperty(value = "白班备注")
    private String class3Remark;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    /**
     * 收尾提示标识(0:提示收尾；1:不需要提示)
     */
    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "收尾规格标记（对应成型排程），0：收尾1：非收尾")
    @TableField("CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    private String productionStatus;

//    @ImportValidated(maxLength = 300)
//    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    /**
     * 月计划需求量
     */
    @ApiModelProperty(value = "月计划需求量", position = 95)
    private String monthPlan;

    /**
     * 月计划需求量
     */
    @ApiModelProperty(value = "月计划剩余量", position = 100)
    private String monthPlanOs;

    @ApiModelProperty(value = "关联汇总表中年份", position = 600)
    private String year;

    @ApiModelProperty(value = "关联汇总表中月份", position = 600)
    private String month;

    @ApiModelProperty(value = "注意事项", position = 600)
    private String notes;

    @ApiModelProperty(value = "颜色类型")
    private String colorType;

    @ApiModelProperty(value = "颜色代码")
    private String colorCode;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

}
