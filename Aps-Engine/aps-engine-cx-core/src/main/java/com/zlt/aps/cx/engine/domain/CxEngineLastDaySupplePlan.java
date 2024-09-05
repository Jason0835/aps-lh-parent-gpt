package com.zlt.aps.cx.engine.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 成型前日计划增补对象 t_cx_last_day_supple_plan
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
@ApiModel(value = "成型前日计划增补对象", description = "成型前日计划增补对象 ")
@Data
public class CxEngineLastDaySupplePlan extends CxEngineScheduleResult {

    private static final long serialVersionUID = 1L;
    /**
     * 增补日期搜索条件
     */
    private String suppleDateStr;

    /**
     * 用于排序时，如果是在产规格优先排
     */
    private  Boolean isProduct=false;

    /**
     * 获取三班计划量
     * @return
     */
    public Integer getTaskQty(){
        //空数据置0
        initData();
        //增补总计划量(计划的三班，次一班，次二班)
        return getClass3PlanQty()+getClass4PlanQty()+getClass5PlanQty();
    }

    /**
     * 初始化数据
     */
    public void initData(){
        if(getClass3PlannedQty()==null){
            setClass3PlannedQty(0);
        }
        if(getClass1PlanQty()==null){
            setClass1PlanQty(0);
        }
        if(getClass2PlanQty()==null){
            setClass2PlanQty(0);
        }
        if(getClass3PlanQty()==null){
            setClass3PlanQty(0);
        }
        if(getClass4PlanQty()==null){
            setClass4PlanQty(0);
        }
        if(getClass5PlanQty()==null){
            setClass5PlanQty(0);
        }

        if(getClass1Sort()==null){
            setClass1Sort(0);
        }
        if(getClass2Sort()==null){
            setClass2Sort(0);
        }
        if(getClass3Sort()==null){
            setClass3Sort(0);
        }
        if(getClass4Sort()==null){
            setClass4Sort(0);
        }
        if(getClass5Sort()==null){
            setClass5Sort(0);
        }

    }





}
