package com.zlt.aps.gdyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 * 钢带压延排程结果表
 * </p>
 *
 * @author chen
 * @since 2021-07-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GDYY_SCHEDULE_RESULT")
@ApiModel(value = "GdyyScheduleResult对象", description = "钢带压延排程结果表")
//@KeySequence(value = "SEQ_GDYY_SCHEDULE",dbType = DbType.ORACLE)
public class GdyyScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的15度裁断批次号")
    @TableField("CD15_BATCH_NO")
    private String cd15BatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    @TableField("ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "钢带大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    @ApiModelProperty(value = "日用参考（个）")
    @TableField("DAY_USED")
    private Double dayUsed;

    @ApiModelProperty(value = "库存（个）")
    @TableField("STOCK_QTY")
    private Double stockQty;

    @ApiModelProperty(value = "中班（一班16点-24点）计划量")
    @TableField("CLASS1_PLAN")
    private Double class1Plan;

    @ApiModelProperty(value = "中班计划量个数")
    private Double class1PlanNum;
    
    @ApiModelProperty(value = "中班（一班16点-24点）无库存计划量")
    @TableField("CLASS1_PLAN_NO_STOCK")
    private Double class1PlanNoStock;

    @ApiModelProperty(value = "中班（一班16点-24点）支领量")
    @TableField("CLASS1_FINISH")
    private Double class1Finish;

    @ApiModelProperty(value = "中班（一班16点-24点）备注")
    @TableField("CLASS1_REMARK")
    private String class1Remark;

    @ApiModelProperty(value = "夜班（二班0点-8点）计划量")
    @TableField("CLASS2_PLAN")
    private Double class2Plan;

    @ApiModelProperty(value = "夜班计划量个数")
    private Double class2PlanNum;

    @ApiModelProperty(value = "夜班（二班0点-8点）无计划计划量")
    @TableField("CLASS2_PLAN_NO_STOCK")
    private Double class2PlanNoStock;

    @ApiModelProperty(value = "夜班（二班0点-8点）支领量")
    @TableField("CLASS2_FINISH")
    private Double class2Finish;

    @ApiModelProperty(value = "夜班（二班0点-8点）备注")
    @TableField("CLASS2_REMARK")
    private String class2Remark;

    @ApiModelProperty(value = "白班（三班8点-16点）计划量")
    @TableField("CLASS3_PLAN")
    private Double class3Plan;

    @ApiModelProperty(value = "白班计划量个数")
    private Double class3PlanNum;

    @ApiModelProperty(value = "白班（三班8点-16点）无计划计划量")
    @TableField("CLASS3_PLAN_NO_STOCK")
    private Double class3PlanNoStock;

    @ApiModelProperty(value = "白班（三班8点-16点）支领量")
    @TableField("CLASS3_FINISH")
    private Double class3Finish;

    @ApiModelProperty(value = "白班（三班8点-16点）备注")
    @TableField("CLASS3_REMARK")
    private String class3Remark;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "收尾规格标记（对应成型排程），0：收尾1：非收尾")
    @TableField("CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;


    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    /**
     * 月计划需求量
     */
    @Excel(name = "月计划需求量")
    @ApiModelProperty(value = "月计划需求量", position = 95)
    @TableField(exist = false)
    private String monthPlan;

    /**
     * 月计划需求量
     */
    @Excel(name = "月计划剩余量")
    @ApiModelProperty(value = "月计划剩余量", position = 100)
    @TableField(exist = false)
    private String monthPlanOs;

    @ApiModelProperty(value = "关联汇总表中年份", position = 600)
    @TableField(exist = false)
    private String year;

    @ApiModelProperty(value = "关联汇总表中月份", position = 600)
    @TableField(exist = false)
    private String month;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "排程记录id数组")
    @TableField(exist = false)
    private Long[] ids;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    /**
     * 机台ID
     */
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @TableField(exist = false)
    private String machineName;
}
