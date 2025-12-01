package com.zlt.mix.schedule.engine.service.basicdata;

import com.zlt.mix.schedule.engine.vo.FormulaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MixMachine;

import java.util.List;
import java.util.Map;

/**
 * 引擎部分库存相关Service
 */
public interface MachineEngineService {

    /**
     * 获取胶料对应的机台列表
     * @param mixArea 密炼区
     * @return map，key--密炼区+胶料号， value--机台code（多个机台时逗号分割）
     */
    Map<String, String> mapGlueMachine(String mixArea);

    /**
     * 获取胶料对应的机台列表
     *
     * @param mixArea  密炼区
     * @param glueList 胶料列表
     * @return map，key--密炼区+胶料号， value--机台code（多个机台时逗号分割）
     */
    Map<String, String> mapGlueMachineByGLueList(String mixArea, List<String> glueList);

    /**
     * 获取硫磺辅料对应的机台列表
     * @param areaMaterialList 要查询的硫磺辅料列表
     * @return map，key--密炼区+胶料号， value--机台code（多个机台时逗号分割）
     */
    Map<String, String> mapMaterialMachine(List<MaterialAreaMachineVo> areaMaterialList);

    /**
     * 获取硫磺辅料机台的map
     * @param mixArea 密炼区
     * @return map，key--机台编号， value--机台信息对象
     */
    Map<String, LhflMachine> mapLhflMachineInfo(String mixArea);

    /**
     * 获取密炼机台的名称map
     * @param mixArea 密炼区
     * @return map，key--机台编号， value--机台名称
     */
    Map<String, String> mapMixMachineName(String mixArea);

    /**
     * 查询出密炼机台信息列表
     * @param mixArea 密炼区
     * @return
     */
    List<MixMachine> listMixMachineInfo(String mixArea);
    
    /**
     * 根据密炼区查询配方与机台对应信息
     *
     * @param machine 参数
     * @return 查询到的集合
     */
    List<FormulaMachineVo> listFormulaMachine(String mixArea);
}
