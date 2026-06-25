package com.zlt.aps.xwyy.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 纤维压延排程结果对象 t_xwyy_schedule_result
 *
 * @author chen
 * @date 2021-07-06
 */
@Data
@ApiModel(value = "纤维压延排程结果对象", description = "纤维压延排程结果对象 ")
public class XwyyScheduleResultDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_XWYY_SCHEDULE
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
     * 对应的90度裁断批次号
     */
    @ApiModelProperty(value = "对应90度裁断批次号")
    private String cd90BatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /**
     * 帘布大卷编号
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.xwyy.scheduleResult.bigRollCode")
    @ApiModelProperty(value = "帘布大卷代号")
    private String bigRollCode;

    /**
     * 原线代码
     */
    @ApiModelProperty(value = "原线代码")
    private String originalLineCode;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线")
    private String machineId;

    /**
     * 库存供应成型时长，单位：小时
     */
    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /**
     * 当日合计个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQtyNum")
    @ApiModelProperty(value = "当日合计个数")
    private Double dailyTotalQtyNum;

    /**
     * 中班(12点-24点)计划量
     */
    @ImportValidated(number = true, min = 0, max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.meter")
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double dayPlanQty;

    /**
     * 中班生产顺序
     */
    @ApiModelProperty(value = "早班生产顺序")
    private Integer dayProduceOrder;

    /**
     * 中班计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.PlanQtyNum")
    @ApiModelProperty(value = "中班计划量个数")
    private Double dayPlanQtyNum;

    /**
     * 中班(12点-24点)完成量
     */
    @ImportValidated(number = true, min = 0, max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.dayFinishQty", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "中班(12点-24点)完成量")
    private Double dayFinishQty;

    /**
     * 中班(12点-24点)系统原因分析
     */
    @ApiModelProperty(value = "中班(12点-24点)系统原因分析")
    private String daySysAnalysis;

    /**
     * 中班(12点-24点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析")
    private String dayHandAnalysis;

    /**
     * 中班排程过程值
     */
    @ApiModelProperty(value = "中班排程过程值")
	private String dayProcessValue;

    /**
     * 夜班(0点-12点)计划量
     */
    @ImportValidated(number = true, min = 0, max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty.meter")
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double nightPlanQty;

    /**
     * 夜班生产顺序
     */
    @ApiModelProperty(value = "夜班生产顺序")
    private Integer nightProduceOrder;

    /**
     * 夜班计划量个数
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.PlanQtyNum")
    @ApiModelProperty(value = "夜班计划量个数")
    private Double nightPlanQtyNum;

    /**
     * 夜班(0点-12点)完成量
     */
    @ImportValidated(number = true, min = 0, max = 9999999)
    @Excel(name = "ui.data.column.scheduleResult.nightFinishQty", type = Excel.Type.EXPORT)
    @ApiModelProperty(value = "夜班(0点-12点)完成量")
    private Double nightFinishQty;

    /**
     * 夜班(0点-12点)系统原因分析
     */
    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析")
    private String nightSysAnalysis;

    /**
     * 夜班(0点-12点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析")
    private String nightHandAnalysis;

    /**
     * 夜班排程过程值
     */
    @ApiModelProperty(value = "夜班排程过程值")
	private String nightProcessValue;

    /**
     * 前日库存
     */
    @ApiModelProperty(value = "前日库存")
    private Double yesStock;

    /**
     * 当日库存
     */
    @ApiModelProperty(value = "当日库存")
    private Double todayStock;

    /**
     * 日用参考（个）
     */
    @ApiModelProperty(value = "日用参考")
    private Double dayUsed;

    /**
     * 白班外厂应支
     */
    @ImportValidated(number = true, min = 0, max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayOut.meter")
    @ApiModelProperty(value = "白班外厂应支")
    private Double dayOut;

    /**
     * 2厂早班计划量
     */
    @ApiModelProperty(value = "2厂早班计划量")
    private Double fac2Class1Plan;

    /**
     * 2厂中班计划量
     */
    @ApiModelProperty(value = "2厂中班计划量")
    private Double fac2Class2Plan;

    /**
     * 2厂晚班计划量
     */
    @ApiModelProperty(value = "2厂晚班计划量")
    private Double fac2Class3Plan;

    /**
     * 2厂合计计划量
     */
    @ApiModelProperty(value = "2厂合计计划量")
    private Double fac2TotalPlan;

    /**
     * 5厂早班计划量
     */
    @ApiModelProperty(value = "5厂早班计划量")
    private Double fac5Class1Plan;

    /**
     * 5厂中班计划量
     */
    @ApiModelProperty(value = "5厂中班计划量")
    private Double fac5Class2Plan;

    /**
     * 5厂晚班计划量
     */
    @ApiModelProperty(value = "5厂晚班计划量")
    private Double fac5Class3Plan;

    /**
     * 5厂合计计划量
     */
    @ApiModelProperty(value = "5厂合计计划量")
    private Double fac5TotalPlan;

    /**
     * 总合计计划量
     */
    @ApiModelProperty(value = "总合计计划量")
    private Double totalPlan;

    /**
     * 对应成型一班的计划量
     */
    @ApiModelProperty(value = "对应成型一班的计划量")
    private Double cxClass1Plan;

    /**
     * 对应成型二班的计划量
     */
    @ApiModelProperty(value = "对应成型二班的计划量")
    private Double cxClass2Plan;

    /**
     * 对应成型三班的计划量
     */
    @ApiModelProperty(value = "对应成型三班的计划量")
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的计划量
     */
    @ApiModelProperty(value = "对应成型次一班的计划量")
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的计划量
     */
    @ApiModelProperty(value = "对应成型次二班的计划量")
    private Double cxClass5Plan;

	/**
	 * 额外计划量标识：0无，1有额外计划量
	 */
	private String extraPlanFlag;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    /**
     * 删除标识：0--正常，1-删除
     */
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String delFlag;

    /**
     * 收尾提示标识(0:提示收尾；1:不需要提示)
     */
    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "收尾规格标记（对应成型排程），0：收尾1：非收尾")
    @TableField("CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;

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

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    private String productionStatus;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    @ApiModelProperty(value = "颜色类型")
    private String colorType;

    @ApiModelProperty(value = "颜色代码")
    private String colorCode;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "对应的成型批次号")
    private String cxBatchNo;

    @ApiModelProperty(value = "标准米长")
    private BigDecimal actClothLength;

    @ApiModelProperty(value = "排程记录id数组")
    private Long[] ids;

    @ApiModelProperty(value = "调度员是否修改了生产线，0：否，1：是")
    private Integer changeMachine;

    @ApiModelProperty(value = "调度员是否修改了中班计划量，0：否，1：是")
    private Integer changeDayPlan;

    @ApiModelProperty(value = "调度员是否修改了夜班计划量，0：否，1：是")
    private Integer changeNightPlan;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    private transient List<Long> ids2;

    /**
     * 原线提醒，0：不提醒，1：提醒
     */
    @ApiModelProperty(value = "原线提醒，0：不提醒，1：提醒")
    private String originalRemindFlag;

    /**
     * 原线规格名称
     */
    @ApiModelProperty(value = "原线规格名称")
    private String originalLineName = "";

    /**
     * 原线卷数
     */
    @ApiModelProperty(value = "原线卷数")
    private String originalLineQtyNum;

    /**
     * 原线长度
     */
    @ApiModelProperty(value = "原线长度")
    private String originalLineLength;

    /**
     * 多规格共用原线情况，需要更新的帘布大卷代号
     */
    private String maxBigRollCode;

    /**
     * 多规格共用原线且原线品牌相同的情况，需要更新的帘布大卷代号
     */
    private String maxBigRollCodeBrand;

    /**
     * 页面排序
     */
    private String orderStr;

    /**
     * 胶料号
     */
    @Excel(name = "ui.data.column.xwyy.scheduleResult.rubberCode")
    @ApiModelProperty(value = "胶料号")
    private String rubberCode;

    /**
     * 胶料车数
     */
    @Excel(name = "ui.data.column.xwyy.scheduleResult.rubberCarNumber")
    @ApiModelProperty(value = "胶料车数")
    private BigDecimal rubberCarNumber;

    /**
     * 原线品牌
     */
    @Excel(name = "ui.data.column.bigRollOriginalBrand.brand")
    @ApiModelProperty(value = "原线品牌")
    private String originalBrand;

    /**
     * 原线品牌个数
     */
    @Excel(name = "ui.data.column.bigRollOriginalBrand.brandNum")
    @ApiModelProperty(value = "原线品牌个数")
    private BigDecimal originalBrandNum;

    /**
     * 库存+计划量超过日用参考量（4倍）标记，1：已超过，其他未超过
     */
    private String moreThanDayUsedFlag;

    /**
     * 总大卷个数
     */
    private BigDecimal totalPlanNum;

    @ApiModelProperty(value = "机台名称")
    @TableField(exist = false)
    private String machineName;
}
