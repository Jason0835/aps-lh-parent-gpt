package com.zlt.aps.lh.engine.domain;

import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import lombok.Data;

import java.util.Date;
import java.util.Objects;

/**
 * 硫化排程结果引擎端对象
 */
@Data
public class LhEngineScheduleResult extends LhScheduleResultDto {

    /**
     *  硫化排程日期条件
     */
    private String lhScheduleDate;

    /**
     * 硫化机定额产量
     */
    private Integer quota;

    /**
     * 一班是否停汽
     */
    private Boolean class1Occlusion=false;

    /**
     * 二班是否停汽
     */
    private Boolean class2Occlusion=false;

    /**
     * 三班是否停汽
     */
    private Boolean class3Occlusion=false;

    /**
     * 一班是否换模
     */
    private Boolean class1ChangeMold=false;

    /**
     * 二班是否换模
     */
    private Boolean class2ChangeMold=false;

    /**
     * 三班是否换模
     */
    private Boolean class3ChangeMold=false;

    /**
     * 一班是否开班
     */
    private Boolean class1OpenShift=false;

    /**
     * 班次上限
     */
    private Integer class1MaxPlanQty=0;

    /**
     * 二班是否开班
     */
    private Boolean class2OpenShift=false;

    /**
     * 二班计划上限
     */
    private Integer class2MaxPlanQty=0;

    /**
     * 三班是否开班
     */
    private Boolean class3OpenShift=false;

    /**
     * 三班计划上限
     */
    private Integer class3MaxPlanQty=0;

    /**
     * 一班开汽标记
     */
    private Boolean class1OpenStream=false;

    /**
     * 二班开汽标记
     */
    private Boolean class2OpenStream=false;

    /**
     * 三班开汽标记
     */
    private Boolean class3OpenStream=false;

    //硫化机台生产顺序
    private Integer machinePlanSort;

    /**
     *  一班原因分析编码，多个拼接
     */
    private String  class1AnalysisCode;

    /**
     *  二班原因分析编码，多个拼接
     */
    private String  class2AnalysisCode;

    /**
     *  三班原因分析编码，多个拼接
     */
    private String  class3AnalysisCode;

    //数据来源：0>自动排程；1>APS插单；2>导入；
    private String dataSource;

    //使用模数
    private Integer useMoldNumber;

    //昨日存在换膜计划标记
    private Boolean lastDayChangeMoldFlag=false;

    //今日换模计划标记
    private Boolean toDayChangeMoldFlag=false;

    //是否存在多个胎胚的数据
    private Boolean isMoreEmbryoCode=false;
    //是否换模计划
    private Boolean changeMoldFlag=false;

    //开班时间
    private Date classOpenShiftTime;

    //开汽时间
    private Date classOpenStreamTime;
    //换模时间
    private Date changeMoldTime;

    //可用开始时间
    private Date enableStartTime;

    //可用结束时间
    private Date enableEndTime;

    //生产顺序 开班>开汽>正常>换模
    private Integer planSort;

    private String orderByPlanQtyStr;

    /**
     * 一班待料标记
     */
    private Boolean class1WaitMaterialFlag=false;

    /**
     * 二班待料标记
     */
    private Boolean class2WaitMaterialFlag=false;

    /**
     * 三班待料标记
     */
    private Boolean class3WaitMaterialFlag=false;

    public void initLhPlanQty(){
        if(getClass1PlanQty()==null){
            setClass1PlanQty(0);
        }
        if(getClass2PlanQty()==null){
            setClass2PlanQty(0);
        }
        if(getClass3PlanQty()==null){
            setClass3PlanQty(0);
        }
    }

    /**
     * 重写equal
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LhEngineScheduleResult)) return false;
        if (!super.equals(o)) return false;
        LhEngineScheduleResult that = (LhEngineScheduleResult) o;
        return Objects.equals(getOrderNo(), that.getOrderNo());
    }

}
