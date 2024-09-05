package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxEngineScheduleLimit;
import com.zlt.aps.cx.engine.mapper.CxEngineScheduleLimitMapper;
import com.zlt.aps.cx.engine.service.CxEngineScheduleLimitService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * 成型排产限制数据获取
  * @ClassName CxEngineScheduleLimitServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/25 16:19
  * @Version 1.0
**/
@Service("cxEngineScheduleLimitService")
@Slf4j
public class CxEngineScheduleLimitServiceImpl implements CxEngineScheduleLimitService {

    @Autowired
    private CxEngineScheduleLimitMapper cxEngineScheduleLimitMapper;
    @Override
    public List<CxEngineScheduleLimit> selectCxEngineScheduleLimitByMachineCodeList() {
        return cxEngineScheduleLimitMapper.selectCxEngineScheduleLimitByMachineCodeList();
    }

    /**
     * 获取组装后的成型排产限制数据
     * @return
     */
    @Override
    public Map<String, CxEngineScheduleLimit> getCxScheduleLimitMap() {
        Map<String, CxEngineScheduleLimit> limitMap=null;
        List<CxEngineScheduleLimit> scheduleLimitList=this.selectCxEngineScheduleLimitByMachineCodeList();
        if(StringUtils.isNotEmpty(scheduleLimitList)){
            limitMap=new HashMap<>();
            for(CxEngineScheduleLimit scheduleLimit:scheduleLimitList){
                String machineType=scheduleLimit.getMachineType();
                String specDimension=scheduleLimit.getSpecDimension()==null?"":scheduleLimit.getSpecDimension().toString();
                String key=CxScheduleUtils.getMapKeyByInputString(machineType,specDimension);
                limitMap.put(key,scheduleLimit);
            }
        }
        return limitMap;
    }

    /**
     * 加载含有成型机编号排产限制
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/7/1 19:06
     * @param 
     * @return 
     */
    @Override
    public Map<String, List<CxEngineScheduleLimit>> getCxScheduleLimitMachineCodeMap() {
        Map<String, List<CxEngineScheduleLimit>> limitMap=new HashMap<>();
        List<CxEngineScheduleLimit> scheduleLimitList=this.selectCxEngineScheduleLimitByMachineCodeList();
        if(StringUtils.isNotEmpty(scheduleLimitList)){
            limitMap=new HashMap<>();
            List<CxEngineScheduleLimit> cxEngineScheduleLimitList=null;
            for(CxEngineScheduleLimit scheduleLimit:scheduleLimitList){
                String machineCode=scheduleLimit.getMachineCode();
                if(limitMap.containsKey(machineCode)){
                    cxEngineScheduleLimitList=limitMap.get(machineCode);
                    cxEngineScheduleLimitList.add(scheduleLimit);
                }else{
                    cxEngineScheduleLimitList=new ArrayList<>();
                    cxEngineScheduleLimitList.add(scheduleLimit);
                    limitMap.put(machineCode,cxEngineScheduleLimitList);
                }

            }
        }
        return limitMap;
    }


}
