package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.mapper.CxEngineChangeLhMachineMapper;
import com.zlt.aps.common.engine.service.CxEngineChangeLhMachineService;
import com.zlt.aps.cx.api.domain.entity.CxChangeLhMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
  * 成型排程工单号对应的硫化机关系信息逻辑实现类
  * @ClassName CxEngineChangeLhMachineServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2022/4/14 10:54
  * @Version 1.0
**/
@Service("cxEngineChangeLhMachineService")
public class CxEngineChangeLhMachineServiceImpl implements CxEngineChangeLhMachineService {

    @Autowired
    private CxEngineChangeLhMachineMapper cxEngineChangeLhMachineMapper;

    /**
     * 根据条件查询排程对应硫化机关系数据集合
     * @param cxChangeLhMachine
     * @return
     */
    @Override
    public List<CxChangeLhMachine> listChangeLhMachineList(CxChangeLhMachine cxChangeLhMachine) {
        return cxEngineChangeLhMachineMapper.listChangeLhMachineList(cxChangeLhMachine);
    }

    /**
     * 根据排程日期获取当天成型维护硫化机工单信息
     * @param scheduleDate 排程日期
     * @param dataSource 数据来源：0：成型排程；1：增补计划
     * @param cxOrderNo  成型工单号
     * @return
     */
    @Override
    public Map<String, String> splitCxOrderWithLhMachines(Date scheduleDate,String dataSource,String cxOrderNo) {
        Map<String,String> resultMap=new HashMap<>();
        String scheduleDateStr="";
        if(scheduleDate!=null){
            scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        }
        if (StringUtils.isEmpty(dataSource)){
            dataSource= EngineConstants.CHANGE_MACHINE_DATA_SOURCE_SCHEDULE;
        }
        CxChangeLhMachine condition=new CxChangeLhMachine();
        if(StringUtils.isNotEmpty(scheduleDateStr)){
            condition.setScheduleDateStr(scheduleDateStr);
        }
        if(StringUtils.isNotEmpty(cxOrderNo)){
            condition.setCxOrderNo(cxOrderNo);
        }
        condition.setDataSource(dataSource);
        List<CxChangeLhMachine> cxScheduleLhMachineList= cxEngineChangeLhMachineMapper.splitCxOrderMachineList(condition);
        if(StringUtils.isNotEmpty(cxScheduleLhMachineList)){
            resultMap=cxScheduleLhMachineList.stream().collect(Collectors.toMap(CxChangeLhMachine::getCxOrderNo, CxChangeLhMachine::getLhMachineNames));
        }
        return resultMap;
    }

    /**
     * 删除日期和来源对应的硫化机关系信息
     * @param scheduleDate 排程日期
     * @param dataSource 数据来源：0：成型排程；1：增补计划
     * @return
     */
    @Override
    public int deleteChangeLhMachineByScheduleDate(Date scheduleDate, String dataSource,String cxOrderNo) {
        String scheduleDateStr="";
        if(scheduleDate!=null){
             scheduleDateStr=DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        }
        return cxEngineChangeLhMachineMapper.deleteChangeLhMachineByScheduleDate(scheduleDateStr,dataSource,cxOrderNo);
    }

    /**
     * 批量新增排程和硫化机关系数据
     * @param cxChangeLhMachineList
     * @return
     */
    @Override
    public int batchInsertCxChangeLhMachine(List<CxChangeLhMachine> cxChangeLhMachineList) {
        return cxEngineChangeLhMachineMapper.batchInsertCxChangeLhMachine(cxChangeLhMachineList);
    }

    /**
     * 根据排程日期生成增补计划对应排程和硫化机关系
     * @param scheduleDate 增补计划日期
     */
    @Override
    @Transactional
    public void buildSuppleCxChangeLhMachine(Date scheduleDate,String dataSource) {
        //1.先进行增补计划硫化机关系数据删除
        deleteChangeLhMachineByScheduleDate(scheduleDate,dataSource,"");
        //2.根据日期查询现有成型的硫化机关系
        CxChangeLhMachine condition=new CxChangeLhMachine();
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        condition.setScheduleDateStr(scheduleDateStr);
        condition.setDataSource(EngineConstants.CHANGE_MACHINE_DATA_SOURCE_SCHEDULE);
        List<CxChangeLhMachine> scheduleChangeMachineList=listChangeLhMachineList(condition);
        if(StringUtils.isNotEmpty(scheduleChangeMachineList)){
            List<CxChangeLhMachine> insertSuppleChangeMachineList=new ArrayList<>(scheduleChangeMachineList.size());
            for(CxChangeLhMachine cxChangeLhMachine: scheduleChangeMachineList){
                cxChangeLhMachine.setBaseVale(null);
                cxChangeLhMachine.setId(null);
                cxChangeLhMachine.setUpdateBy(null);
                cxChangeLhMachine.setUpdateTime(null);
                cxChangeLhMachine.setDataSource(dataSource);
                insertSuppleChangeMachineList.add(cxChangeLhMachine);
            }
            //Joran 2022-04-14 创建增补计划对应排程工单的硫化机台信息
            if(StringUtils.isNotEmpty(insertSuppleChangeMachineList)){
                batchInsertCxChangeLhMachine(insertSuppleChangeMachineList);
            }
        }
    }
}
