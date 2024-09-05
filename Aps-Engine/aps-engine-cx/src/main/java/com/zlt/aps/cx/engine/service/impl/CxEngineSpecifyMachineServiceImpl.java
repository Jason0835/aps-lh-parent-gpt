package com.zlt.aps.cx.engine.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.domain.CxEngineSpecifyMachine;
import com.zlt.aps.cx.engine.mapper.CxEngineSpecifyMachineMapper;
import com.zlt.aps.cx.engine.service.CxEngineSpecifyMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * 成型工序获取定点信息业务逻辑实现
  * @ClassName CxEngineSpecifyMachineServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/24 16:53
  * @Version 1.0
**/
@Service("cxEngineSpecifyMachineService")
@Slf4j
public class CxEngineSpecifyMachineServiceImpl implements CxEngineSpecifyMachineService {

    @Autowired
    private CxEngineSpecifyMachineMapper cxEngineSpecifyMachineMapper;

    /**
     * 根据条件获取定点机台列表数据
     * @param cxEngineSpecifyMachine
     * @return
     */
    @Override
    public List<CxEngineSpecifyMachine> selectCxEngineSpecifyMachineList(CxEngineSpecifyMachine cxEngineSpecifyMachine) {
        return cxEngineSpecifyMachineMapper.selectCxEngineSpecifyMachineList(cxEngineSpecifyMachine);
    }

    /**
     * 获取组装成型工序定点机台相关信息
     * @return
     */
    @Override
    public Map<String, List<CxEngineSpecifyMachine>> getAllCxSpecifyMachineInfo(String jobType) {
        if(StringUtils.isEmpty(jobType)){
            jobType= CxEngineConstants.SPECIFY_JOB_TYPE_YES;
        }
        Map<String,List<CxEngineSpecifyMachine>> specifyMachineMap=new HashMap<>();
        //加载成型工序所有定点机台列表
        CxEngineSpecifyMachine condition=new CxEngineSpecifyMachine();
        condition.setJobType(jobType);
        List<CxEngineSpecifyMachine> specifyMachineList=selectCxEngineSpecifyMachineList(condition);
        //key值为SAP品号+胎胚代码+作业类型
        if(StringUtils.isNotEmpty(specifyMachineList)){
            List<CxEngineSpecifyMachine> specifyMachines=null;
            for (CxEngineSpecifyMachine cxEngineSpecifyMachine: specifyMachineList ) {
                if(!jobType.equals(cxEngineSpecifyMachine.getJobType())) {
                    continue;
                }
                String machineCode=cxEngineSpecifyMachine.getCxMachineCode();
               if(specifyMachineMap.containsKey(machineCode)){
                       specifyMachines=specifyMachineMap.get(machineCode);
                       specifyMachines.add(cxEngineSpecifyMachine);
               }else{
                   specifyMachines=new ArrayList<>();
                   specifyMachines.add(cxEngineSpecifyMachine);
               }
                specifyMachineMap.put(machineCode,specifyMachines);
            }
        }
        return specifyMachineMap;
    }

    /**
     * 规格胎胚定点相关设置验证消息提醒
     * @param sapCode
     * @param embryoCode
     * @param machineCode
     * @param sb
     */
    @Override
    public void validateSpecifyMachine(String sapCode, String embryoCode, String machineCode, StringBuilder sb) {
        //验证规格定点机台start
        CxEngineSpecifyMachine spefifyCondition=new CxEngineSpecifyMachine();
        spefifyCondition.setSapCode(sapCode);
        spefifyCondition.setEmbryoCode(embryoCode);
        List<CxEngineSpecifyMachine> cxEngineSpecifyMachineList=this.selectCxEngineSpecifyMachineList(spefifyCondition);
        String spefifyMachineCodes="";
        boolean toAppend=true;
        //不可作业
        List<CxEngineSpecifyMachine> cxEngineSpecifyMachineNoList=new ArrayList<>();
        if(StringUtils.isNotEmpty(cxEngineSpecifyMachineList)){
            for(CxEngineSpecifyMachine cxEngineSpecifyMachine:cxEngineSpecifyMachineList){
                if(CxEngineConstants.SPECIFY_JOB_TYPE_YES.equals(cxEngineSpecifyMachine.getJobType())){
                    if(cxEngineSpecifyMachine.getCxMachineCode().equals(machineCode)){
                        spefifyMachineCodes="";
                        toAppend=false;
                    }
                    if(toAppend){
                        spefifyMachineCodes+=StringUtils.isBlank(spefifyMachineCodes)?cxEngineSpecifyMachine.getCxMachineName():","+cxEngineSpecifyMachine.getCxMachineName();
                    }
                }else{
                    cxEngineSpecifyMachineNoList.add(cxEngineSpecifyMachine);
                }
            }
        }
        if(StringUtils.isNotEmpty(spefifyMachineCodes)){
            String msg=StringUtils.format(I18nUtil.getMessage("cx.engine.change.spefifyMachine.onlyUse.tip"),spefifyMachineCodes);
            sb.append(msg);
        }
        //验证规格限制作业机台end

        //验证不可作业机台start
        if(StringUtils.isNotEmpty(cxEngineSpecifyMachineNoList)){
            for(CxEngineSpecifyMachine cxEngineSpecifyMachine:cxEngineSpecifyMachineNoList){
                if(cxEngineSpecifyMachine.getCxMachineCode().equals(machineCode)){
                    sb.append(I18nUtil.getMessage("cx.engine.change.spefifyMachine.canNotUse.tip"));
                    break;
                }
            }
        }
        //验证不可作业机台end

        //验证规格定点机台end

    }
}
