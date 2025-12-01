package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.schedule.engine.mapper.MachineEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.vo.FormulaMachineVo;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引擎部分库存相关ServiceImpl
 */
@Service
public class MachineEngineServiceImpl implements MachineEngineService {

    @Resource
    private MachineEngineMapper machineEngineMapper;

    /**
     * 获取胶料对应的机台map
     * @Param mixArea 密炼区
     * @return map，key--密炼区+胶料号， value--机台code（多个机台时逗号分割）
     */
    public Map<String, String> mapGlueMachine(String mixArea) {
        Map<String, String> map = new HashMap<>();
        List<GlueAreaMachineVo> glueMachineList = machineEngineMapper.listGlueMachine(mixArea);
        if(!glueMachineList.isEmpty()) {
            for(GlueAreaMachineVo glueAreaMachine : glueMachineList) {
                map.put(glueAreaMachine.getMixArea() + glueAreaMachine.getGlue(), glueAreaMachine.getMachineCode());
            }
        }
        return map;
    }

    /**
     * 获取胶料对应的机台列表
     *
     * @param mixArea  密炼区
     * @param glueList 胶料列表
     * @return map，key--密炼区+胶料号， value--机台code（多个机台时逗号分割）
     */
    public Map<String, String> mapGlueMachineByGLueList(String mixArea, List<String> glueList) {
        Map<String, String> map = new HashMap<>();
        List<GlueAreaMachineVo> glueMachineList = machineEngineMapper.listGlueMachineByGLueList(mixArea, glueList);
        if(!glueMachineList.isEmpty()) {
            for(GlueAreaMachineVo glueAreaMachine : glueMachineList) {
                map.put(glueAreaMachine.getMixArea() + glueAreaMachine.getGlue(), glueAreaMachine.getMachineCode());
            }
        }
        return map;
    }

    /**
     * 获取硫磺辅料对应的机台列表map
     * @param areaMaterialList 要查询的硫磺辅料列表
     * @return map，key--密炼区+胶料号， value--机台code（多个机台时逗号分割）
     */
    public Map<String, String> mapMaterialMachine(List<MaterialAreaMachineVo> areaMaterialList) {
        Map<String, String> map = new HashMap<>();
        List<MaterialAreaMachineVo> materialMachineList = machineEngineMapper.listMaterialMachine(areaMaterialList);
        if(!materialMachineList.isEmpty()) {
            for(MaterialAreaMachineVo materialAreaMachineVo : materialMachineList) {
                boolean isMidEnable = ZltConstant.STATUS_ENABLE.equals(materialAreaMachineVo.getMidStatus());
                boolean isNightEnable = ZltConstant.STATUS_ENABLE.equals(materialAreaMachineVo.getNightStatus());
                boolean isDayEnable = ZltConstant.STATUS_ENABLE.equals(materialAreaMachineVo.getDayStatus());
                
                // 按机台各班别区分机台
                String machineKey = materialAreaMachineVo.getMixArea() + materialAreaMachineVo.getMaterialName();
            	if (isMidEnable) {
                    map.put(machineKey + EngineConstants.CLASS_MID, materialAreaMachineVo.getMachineCode());
            	}
            	if (isNightEnable) {
                    map.put(machineKey + EngineConstants.CLASS_NIGHT, materialAreaMachineVo.getMachineCode());
            	}
            	if (isDayEnable) {
                    map.put(machineKey + EngineConstants.CLASS_DAY, materialAreaMachineVo.getMachineCode());
            	}
            }
        }
        return map;
    }

    /**
     * 获取硫磺辅料机台的map
     * @param mixArea 密炼区
     * @return map，key--机台编号， value--机台信息对象
     */
    public Map<String, LhflMachine> mapLhflMachineInfo(String mixArea) {
        List<LhflMachine> list = machineEngineMapper.listLhflMachineInfo(mixArea);
        return list.stream().collect(Collectors.toMap(r->r.getMachineCode(), r->r));
    }
    

    /**
     * 获取密炼机台的名称map
     * @param mixArea 密炼区
     * @return map，key--机台编号， value--机台名称
     */
	public Map<String, String> mapMixMachineName(String mixArea) {
		return machineEngineMapper.listMixMachineInfo(mixArea).stream()
				.collect(Collectors.toMap(MixMachine::getMachineCode, MixMachine::getMachineName, (m1, m2) -> m1));
	}
	

    /**
     * 查询出密炼机台信息列表
     * @param mixArea 密炼区
     * @return
     */
	public List<MixMachine> listMixMachineInfo(String mixArea) {
    	return machineEngineMapper.listMixMachineInfo(mixArea);
    }

    
    /**
     * 根据密炼区查询配方与机台对应信息
     *
     * @param machine 参数
     * @return 查询到的集合
     */
    public List<FormulaMachineVo> listFormulaMachine(String mixArea) {
    	return machineEngineMapper.selectFormulaMachineList(mixArea);
    }
}
