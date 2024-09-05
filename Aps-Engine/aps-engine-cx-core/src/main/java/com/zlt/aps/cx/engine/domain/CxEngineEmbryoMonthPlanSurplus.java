package com.zlt.aps.cx.engine.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成型工序胎胚计划量汇总表
 * @TableName T_CX_EMBRYO_MONTH_PLAN_SURPLUS
 */
@Data
public class CxEngineEmbryoMonthPlanSurplus extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 生产排程记录主计划版本号,年+月+日+01，02
     */
    private String monthPlanApsVersion;

    /**
     * 主计划版本号
     */
    private String monthPlanVersion;

    /**
     * 主计划所属年份
     */
    private String year;

    /**
     * 主计划所属月份
     */
    private String month;

    /**
     * 物料编码(成型胎胚代码)
     */
    private String embryoCode;

    /**
     * 月度计划量
     */
    private Integer monthPlanQty;

    /**
     * 月度计划调整量
     */
    private Integer monthPlanModifyQty;

    /**
     * 月度完成量
     */
    private Integer monthFinishQty;

    /**
     * 月剩余量
     */
    private Integer monthRemainQty;


    /**
     * 是否收尾标识
     */
    private Boolean isCloseOut;

    /**
     * 是否标识收尾提示
     */
    private Boolean markCloseOutTip;

    /**
     * 数据来源：0主计划；1插单
     */
    private String dataSource;

    /**
     * 月结库存
     */
    private Integer lastMonthStock;

    /**
     * 胎胚不良数
     */
    private Integer embryoBadQty;

    /**
     * Joran 2021-09-19 用来更新插单数据属性
     */
    private Integer updateInsertQty;

    private  String startTime;

    private String  endTime;

    private static final long serialVersionUID = 1L;

    /**
     * 获取月度计划总量
     * @return
     */
    public Integer getTotalPlanQty(){
        initQty();
        return monthPlanQty+monthPlanModifyQty+embryoBadQty-lastMonthStock;
    }

    /**
     * 获取实际超欠产
     * @return
     */
    public Integer getActualOverProduction(){
        //实际完成量-计划总量
        return monthFinishQty-getTotalPlanQty();
    }

    public  void initQty(){
        if(monthPlanQty==null){
            monthPlanQty=0;
        }
        if(monthPlanModifyQty==null){
            monthPlanModifyQty=0;
        }
        if(embryoBadQty==null){
            embryoBadQty=0;
        }
        if(lastMonthStock==null){
            lastMonthStock=0;
        }
        if(monthFinishQty==null){
            monthFinishQty=0;
        }
        if(monthRemainQty==null){
            monthRemainQty=0;
        }
    }
}