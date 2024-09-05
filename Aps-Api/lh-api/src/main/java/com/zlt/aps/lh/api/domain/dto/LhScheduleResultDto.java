package com.zlt.aps.lh.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 硫化排程结果对象 t_lh_schedule_result
 *
 * @author chen
 * @date 2021-07-19
 */
@Data
@ApiModel(value = "硫化排程结果对象", description = "硫化排程结果对象 ")
public class LhScheduleResultDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    /**
     * 工单号，自动生成（工序+日期+三位顺序号001,002）
     */
    @ApiModelProperty(value = "工单号，自动生成")
    private String orderNo;

    /**
     * 硫化机台编号
     */
    @Excel(name = "ui.data.column.scheduleResult.lhMachineCode")
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;

    /**
     * 硫化机台名称
     */
    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.lhMachineName")
    @ApiModelProperty(value = "硫化机台名称")
    private String lhMachineName;

    /**
     * Joran 2022-03-14 存储当前左右模情况，如果非单模单规格的则可为空，单模单规格则存储对应的模信息，如：存储内容，L/R、L1/R1
     */
    @ApiModelProperty(value = "左右模")
    @Excel(name = "ui.data.column.scheduleResult.leftRightMold")
    @ImportValidated(isCode = true, maxLength = 10)
    private String leftRightMold;

    /**
     * SAP品号信息
     */
    @ImportValidated(required = true, isCode = true, maxLength = 60)
    @Excel(name = "ui.data.column.scheduleResult.sapCode")
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    @Excel(name = "ui.data.column.scheduleResult.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 规格描述信息
     */
    @ImportValidated(maxLength = 200)
    @Excel(name = "ui.data.column.scheduleResult.model")
    @ApiModelProperty(value = "规格描述信息")
    private String specDesc;

    /**
     * 库区信息
     */
    @Excel(name = "ui.data.column.scheduleResult.stockArea", dictType = "STORAGE_LOCATION")
    @ApiModelProperty(value = "库区信息")
    private String stockArea;

    /**
     * 硫化时长
     */
    @ApiModelProperty(value = "硫化时长")
    private Double lhTime;

    /**
     * 日计划数量
     */
    @ImportValidated(number = true, min = 0, max = 99999999)
    @Excel(name = "ui.data.column.scheduleResult.dailyPlanQty")
    @ApiModelProperty(value = "日计划数量")
    private Integer dailyPlanQty;

    /**
     * 排程日期
     */
    @ImportValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    /**
     * 一班计划量
     */
    @ImportValidated(number = true, digits = true, min = 0, max = 999999999)
    @Excel(name = "ui.data.column.scheduleResult.class1PlanQty")
    @ApiModelProperty(value = "一班计划量")
    private Integer class1PlanQty;

    @ApiModelProperty(value = "一班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date  class1StartTime;

    @ApiModelProperty(value = "一班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date  class1EndTime;

    /**
     * 一班原因分析
     */
    @ApiModelProperty(value = "一班原因分析")
    private String class1Analysis;

    /** 一班原因分析手工录入 */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class1Analysis")
    @ApiModelProperty(value = "一班原因分析手工录入")
    private String class1AnalysisInput;

    /**
     * 一班完成量
     */
    @Excel(name = "ui.data.column.scheduleResult.class1FinishQty")
    @ApiModelProperty(value = "一班完成量")
    private Integer class1FinishQty;

    /**
     * 二班计划量
     */
    @ImportValidated(number = true, digits = true, min = 0, max = 999999999)
    @Excel(name = "ui.data.column.scheduleResult.class2PlanQty")
    @ApiModelProperty(value = "二班计划量")
    private Integer class2PlanQty;

    @ApiModelProperty(value = "二班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date  class2StartTime;

    @ApiModelProperty(value = "二班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date  class2EndTime;

    /**
     * 二班原因分析
     */
    @ApiModelProperty(value = "二班原因分析")
    private String class2Analysis;

    /** 二班原因分析手工录入 */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class2Analysis")
    @ApiModelProperty(value = "二班原因分析手工录入")
    private String class2AnalysisInput;

    /**
     * 二班完成量
     */
    @Excel(name = "ui.data.column.scheduleResult.class2FinishQty")
    @ApiModelProperty(value = "二班完成量")
    private Integer class2FinishQty;

    /**
     * 三班计划量
     */
    @ImportValidated(number = true, digits = true, min = 0, max = 999999999)
    @Excel(name = "ui.data.column.scheduleResult.class3PlanQty")
    @ApiModelProperty(value = "三班计划量")
    private Integer class3PlanQty;

    @ApiModelProperty(value = "二班开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date  class3StartTime;

    @ApiModelProperty(value = "二班结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date  class3EndTime;

    /**
     * 三班原因分析
     */
    @ApiModelProperty(value = "三班原因分析")
    private String class3Analysis;

    /** 三班原因分析手工录入 */
    @ImportValidated(maxLength = 66)
    @Excel(name = "ui.data.column.scheduleResult.class3Analysis")
    @ApiModelProperty(value = "三班原因分析手工录入")
    private String class3AnalysisInput;

    /**
     * 三班完成量
     */
    @Excel(name = "ui.data.column.scheduleResult.class3FinishQty")
    @ApiModelProperty(value = "三班完成量")
    private Integer class3FinishQty;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    /**
     * 引擎部分校验通过后赋值为true否则false
     */
    private Boolean isSuccess;

    @ApiModelProperty(value = "排程记录id数组")
    private Long[] ids;

    /**
     * 引擎部分使用，成型批次号
     */
    private String cxBatchNo;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "调度员是否修改了硫化机，0：否，1：是")
    private Integer changeMachine;

    @ApiModelProperty(value = "调度员是否修改了一班计划量，0：否，1：是")
    private Integer changeClass1Plan;

    @ApiModelProperty(value = "调度员是否修改了二班计划量，0：否，1：是")
    private Integer changeClass2Plan;

    @ApiModelProperty(value = "调度员是否修改了三班计划量，0：否，1：是")
    private Integer changeClass3Plan;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    /**
     * 相同sap多个胎胚变色标识
     */
    @ApiModelProperty(value = "相同sap多个胎胚变色标识")
    private String multipleEmbryosOfSameSapFlag;
}
