package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxEngineMesMoldAdjustPlan;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleResult;
import com.zlt.aps.cx.engine.domain.MesLhProductionSpec;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.service.CxLhEngineCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("cxLhEngineCommonService")
@Slf4j
public class CxLhEngineCommonServiceImpl implements CxLhEngineCommonService {

    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    /**
     *  昨日的排程列表进行自动机台匹配
     * @param cxEngineScheduleResultList
     */
    @Override
    public void cxScheduleAutoMachLhMachine(Date suppleDate, List<CxEngineScheduleResult> cxEngineScheduleResultList) {
         if(suppleDate==null){
             suppleDate= DateUtils.getNowDate();
         }

        /* if(StringUtils.isEmpty(cxEngineScheduleResultList)){
             log.error("自动匹配的排程计划数据为空，不再进行自动硫化机安排！");
             return;
         }*/

        //1.进行增补计划日期对应的模具变动单数据
        String planDateStr=DateUtils.parseDateToStr("yyyyMMdd",suppleDate);
        CxEngineMesMoldAdjustPlan condition=new CxEngineMesMoldAdjustPlan();
        condition.setPlanDateStr(planDateStr);
        List<CxEngineMesMoldAdjustPlan> suppleDateMoldPlanList=cxLhEngineCommonMapper.selectMesMoldAdjustPlanList(condition);
        StringBuilder sb=new StringBuilder();
        sb.append("获取Mes模具变更计划:").append("[").append("\n");
        for(CxEngineMesMoldAdjustPlan cxEngineMesMoldAdjustPlan:suppleDateMoldPlanList){
            String lhMachineCode=cxEngineMesMoldAdjustPlan.getLhMachineCode();
            Date finishDate=cxEngineMesMoldAdjustPlan.getFinishTime();
            String moldStatusStr="0".equals(cxEngineMesMoldAdjustPlan.getMoldStatus())?"未装配":"已装配";
            String leftSapCode =cxEngineMesMoldAdjustPlan.getLeftSapCode();
            String rightSapCode=cxEngineMesMoldAdjustPlan.getRightSapCode();
            if(finishDate==null){ //换模完成时间为空，还是替换为后规格，需要进行提醒，尚未执行换模计划
                //TODO 提醒 尚未执行换模计划
            }
            sb.append(StringUtils.format("硫化机：{},执行状态：{},右模后规格:{}，左模后规格:{}",lhMachineCode,moldStatusStr,rightSapCode,leftSapCode)).append("\n");
        }
        sb.append("]").append("\n");

        sb.append("获取硫化机在产规格数据:").append("[").append("\n");
        //2.获取当前硫化在产规格数据
        List<MesLhProductionSpec> mesLhProductionSpecList=cxLhEngineCommonMapper.selectLhInProductionSpecByDate(planDateStr);
        for(MesLhProductionSpec mesLhProductionSpec:mesLhProductionSpecList){
            sb.append(StringUtils.format("硫化机：{}，规格sap:{}",mesLhProductionSpec.getLhMachineCode(),mesLhProductionSpec.getSapCode())).append("\n");
        }
        sb.append("]");
        System.out.println(sb.toString());

    }
}
