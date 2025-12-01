package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.engine.vo.FormulaMachineVo;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 引擎机台相关mapper
 */
public interface MachineEngineMapper {

    /**
     * 获取胶料对应的机台列表
     * @param mixArea 密炼区
     * @return
     */
    List<GlueAreaMachineVo> listGlueMachine(@Param("mixArea") String mixArea);
    
    /**
     * 获取胶料对应的机台列表
     * @param mixArea 密炼区
     * @param glueList 胶料列表
     * @return
     */
    List<GlueAreaMachineVo> listGlueMachineByGLueList(@Param("mixArea") String mixArea, @Param("glueList") List<String> glueList);

    /**
     * 获取胶料对应的机台列表
     * @param areaMaterialList 硫磺辅料+密炼区列表
     * @return
     */
    List<MaterialAreaMachineVo> listMaterialMachine(@Param("areaMaterialList") List<MaterialAreaMachineVo> areaMaterialList);

    /**
     * 查询出硫磺辅料机台信息列表
     * @param mixArea 密炼区
     * @return
     */
    List<LhflMachine> listLhflMachineInfo(@Param("mixArea") String mixArea);
    

    /**
     * 查询出密炼机台信息列表
     * @param mixArea 密炼区
     * @return
     */
    List<MixMachine> listMixMachineInfo(@Param("mixArea") String mixArea);
    
    /**
     * 根据密炼区查询配方与机台对应信息
     *
     * @param machine 参数
     * @return 查询到的集合
     */
    List<FormulaMachineVo> selectFormulaMachineList(@Param("mixArea") String mixArea);
}
