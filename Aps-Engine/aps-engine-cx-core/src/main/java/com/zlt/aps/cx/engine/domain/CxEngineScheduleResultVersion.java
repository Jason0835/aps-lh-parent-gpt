package com.zlt.aps.cx.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
  * 成型排程任务版本对象
  * @ClassName CxScheduleTask
  * @Description
  * @Author Joran.Zhang
  * @Date 2021/6/22 17:20
  * @Version 1.0
**/
@Data
@ApiModel(value = "成型排程任务留存版本对象", description = "成型排程任务留存版本对象")
public class CxEngineScheduleResultVersion extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 留存所对应的增补批次号
     */
    private String suppleBatchNo;

    @ApiModelProperty(value = "留存版本成型排程批次号")
    private String cxBatchNo;

    /** 成型排程工单号，自动生成，批次号+4位定长自增序号 */
    @ApiModelProperty(value = "成型排程工单号")
    private String orderNo;


    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;


    /** 成型机台编号 */
    @ApiModelProperty(value = "成型机台编号")
    private String cxMachineCode;

    /**
     * 成型机台类型：一次法；二次法
     */
    private String cxMachineType;


    /** 硫化机台编号 */
    @ApiModelProperty(value = "硫化机台编号")
    private String lhMachineCode;


    /** 库存地点 */
    @ApiModelProperty(value = "库存地点")
    private String storageLocation;

    /** SAP品号 */
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /** 规格型号 */
    @ApiModelProperty(value = "规格型号")
    private String specDesc;

    @ApiModelProperty(value = "外胎规格尺寸信息")
    private Double specDimension;

    /** 胎胚代码 */
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;


    /** 总库存数量 */
    @ApiModelProperty(value = "总库存数量")
    private Integer totalStock;

    /** 三班可硫化班次 */
    @ApiModelProperty(value = "三班可硫化班次")
    private Double class3AvailableLhShift;

    /** 三班计划数 */
    @ApiModelProperty(value = "三班计划数")
    private Integer class3PlanQty;

    /** 三班原因分析 */
    @ApiModelProperty(value = "三班原因分析")
    private String class3Analysis;


    /** 次日一班可硫化班次 */
    @ApiModelProperty(value = "次日一班可硫化班次")
    private Double class4AvailableLhShift;

    /** 次日一班计划数 */
    @ApiModelProperty(value = "次日一班计划数")
    private Integer class4PlanQty;

    /** 次日一班原因分析 */
    @ApiModelProperty(value = "次日一班原因分析")
    private String class4Analysis;



    /** 次日二班可硫化班次 */
    @ApiModelProperty(value = "次日二班可硫化班次")
    private Double class5AvailableLhShift;

    /** 次日二班计划数 */
    @ApiModelProperty(value = "次日二班计划数")
    private Integer class5PlanQty;

    /** 次日二班原因分析 */
    @ApiModelProperty(value = "次日二班原因分析")
    private String class5Analysis;


    /**
     * 冗余月度剩余量，后续计划安排需要用到
     */
    private Integer monthRemainQty;

    /**
     * 成型月度完成量
     */
    private Integer monthFinishQty;

    /**
     * 发布状态
     */
    public String isRelease;

    /**
     * 2021-07-23 Joran.zhang
     * 按胎胚所在排程硫化机模数占比计算库存，后续可硫化班次按这个库存来计算
     */
    public Integer calcTotalStock;

    /**
     * 计算出来的模数
     */
    public Integer calcMoldNum;

    /**
     * 2021-08-04 单胎硫化时长（分钟，从施工信息表中获取）
     */
    private Double lhSingleTireTime;

    /**
     * 成型排程日期字符串
     */
    private String cxScheduleDate;

    /**
     * 新增字段，确定硫化机 添加多台硫化机及对应的更换类型进行拼接
     * 如 硫化机1:拆模换;硫化机2:点数换;
     */
    @ApiModelProperty(value = "确定硫化机以及更换类型")
    private String lhMachineChangeMoldDesc;

    /**
     * 生产排程对应的生产排程版本号，属性只是用于关联查询存储
     */
    private  String apsMonthVersion;

    /**
     * 施工版本
     */
    private String bomDataVersion;

    /**
     * 特殊要求
     */
    private String specialRequirements;

    /**
     * 标记是否投产 0：是；1：否，处理同胎胚只投产一个
     */
    private String toProduct;


    /**
     * Joran 2021-12-22 标记月度汇总表中已经收尾 挑选到这种规格时不允许在进行挑选，且投产表状态要变更为已投产状态
     */
    private Boolean isProducted;


    @Override
    public String toString() {
        return "排程结果版本留存表{" +
                "id=" + id +
                ", 成型批次号='" + cxBatchNo + '\'' +
                ", 排程日期=" + DateUtils.parseDateToStr("yyyyMMdd",scheduleDate) +
                ", 机台编号='" + cxMachineCode + '\'' +
                ", 库存地点='" + storageLocation + '\'' +
                ", SAP品号='" + sapCode + '\'' +
                ", 寸口=" + specDimension +
                ", 胎胚代码='" + embryoCode + '\'' +

                ", 3班可硫化班次=" + class3AvailableLhShift +
                ", 3班计划量=" + class3PlanQty +
                ", 3班原因分析='" + class3Analysis + '\'' +
                ", 4班可硫化班次=" + class4AvailableLhShift +
                ", 4班计划量=" + class4PlanQty +
                ", 4班原因分析='" + class4Analysis + '\'' +
                ", 5班可硫化班次=" + class5AvailableLhShift +
                ", 5班计划量=" + class5PlanQty +
                ", 5班原因分析='" + class5Analysis + '\'' +
                '}';
    }



    /**
     * 根据增补批次号+工单号判断是否相同
     * @param obj
     * @return
     */
    public boolean equals (Object obj) {
        CxEngineScheduleResultVersion result = (CxEngineScheduleResultVersion) obj ;
        String key= GenerageMapKeyUtils.createMapKey(this.orderNo,this.suppleBatchNo);
        String thatKey= GenerageMapKeyUtils.createMapKey(result.orderNo,result.suppleBatchNo);
        return key.equals(thatKey);
    }


    /**
     * 获取扣除当天所有班次计划量后剩余的月度计划量
     * @return
     */
    public Integer calcMonthRemainQty(){
        monthRemainQty=(monthRemainQty==null?0:monthRemainQty);
        if(monthRemainQty>0){
            return this.monthRemainQty;
        }else{
            return monthRemainQty;
        }

    }

    /**
     * 初始化各个班次计划量
     */
    public void initPlanQty(){

        if(class3PlanQty==null){
            class3PlanQty=0;
        }
        if(class4PlanQty==null){
            class4PlanQty=0;
        }
        if(class5PlanQty==null){
            class5PlanQty=0;
        }
    }

    /**
     * 获取三班后的计划总量，汇总进行安排比较快
     * @return
     */
    public Integer getAfterClass3PlanQty(){
        initPlanQty();
        return  class3PlanQty+class4PlanQty+class5PlanQty;
    }

}
