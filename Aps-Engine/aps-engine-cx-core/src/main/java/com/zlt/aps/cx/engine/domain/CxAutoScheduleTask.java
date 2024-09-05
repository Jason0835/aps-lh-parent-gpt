package com.zlt.aps.cx.engine.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
  * 成型自动排程任务对象
  * @ClassName CxAutoScheduleTask
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 17:11
  * @Version 1.0
**/
@Data
@ApiModel(value="成型自动排程任务对象", description="成型自动排程任务表")
public class CxAutoScheduleTask extends ApsBaseEntity {

    private  static final  long serialVersionUID=1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "成型工单号")
    @TableField("CX_ORDER_NO")
    private String cxOrderNo;

    @ApiModelProperty(value = "成型排程日期")
    @TableField("SCHEDULE_DATE")
    private String scheduleDate;

    @ApiModelProperty(value = "成型班次第一班为1，第二班：2，第三班：3，次一班：4，次二班：5，次三班：6")
    @TableField("CLASS_SHIFT")
    private Integer classShift;

    @ApiModelProperty(value = "成型机台编号")
    @TableField("CX_MACHINE_CODE")
    private String cxMachineCode;

    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    private String embryoCode;

    @ApiModelProperty(value = "任务计划总量")
    @TableField("TASK_TOTAL_QTY")
    private Integer taskTotalQty;

    @ApiModelProperty(value = "剩余计划量")
    @TableField("REMAIN_TASK_QTY")
    private Integer remainTaskQty;

    @ApiModelProperty(value = "本班计划量")
    @TableField("current_shift_plan_qty")
    private Integer currentShiftPlanQty;

    @ApiModelProperty(value = "任务状态;0：结束，1：进行中")
    @TableField("STATUS")
    private String status;

    /**
     *  当前班次剩余时间
     */
    private Double remainTime;
    /**
     * 班次时长
     */
    private Double classShiftHour= CxEngineConstants.CLASS_SHIFT_HOUR;

    /**
     * 施工版本信息
     */
    private String bomDataVersion;

    /**
     * 一批次自动排程的最大班次计划标注为更换工装，下一个规格根据这个来控制是否标注更换工装
     */
    private Boolean isChangeMoldAnalysis=false;

    /**
     * 机台任务顺序
     */
    private Integer machineAutoScheduleSort;

    @Override
    public String toString() {
        return "自动排程任务结果：{" +
                ", 机台编号='" + cxMachineCode + '\'' +
                ", 排程日期='" + scheduleDate + '\'' +
                ", 班次=" + classShift + '\'' +
                ", 成型工单号='" + cxOrderNo + '\'' +
                ", 胎胚代码='" + embryoCode + '\'' +
                ", 总任务量=" + taskTotalQty +
                ", 剩余任务量=" + remainTaskQty +
                ", 当前班次任务量=" + currentShiftPlanQty +
                ", 剩余时间=" + remainTime +
                ", 班次时长=" + classShiftHour +
                '}';
    }
}
