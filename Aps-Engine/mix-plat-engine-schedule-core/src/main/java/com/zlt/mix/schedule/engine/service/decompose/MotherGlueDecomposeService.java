package com.zlt.mix.schedule.engine.service.decompose;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;

import java.util.List;
import java.util.Map;

/**
 * 根据终炼胶分解出对应的母炼胶
 */
public interface MotherGlueDecomposeService {

    /**
     * 解析终炼母炼分解表
     * @param mixArea 密炼区
     * @param glueAreaMachineList 终炼胶、密炼区、机台列表
     * @param glueMachineMap 胶料对应的机台map
     * @return 最终返回格式：key--终炼胶code，value--此终炼胶下的母炼胶code集合
     */
    Map<String, List<GlueAreaMachineVo>> parseGlueDecompose(String mixArea, List<GlueAreaMachineVo> glueAreaMachineList, Map<String, String> glueMachineMap);

    /**
     * 解析母炼胶 对应的全部下级母炼胶
     * @param plan
     * @param glueMachineMap 胶料对应的机台map
     * @return
     */
    List<GlueAreaMachineVo> parseGlueDecomposeByMother(GlueDecomposePlan plan , Map<String, String> glueMachineMap);
}
