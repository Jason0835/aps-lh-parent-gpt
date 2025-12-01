package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.engine.vo.DecomposeRecipeVo;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import com.zlt.mix.setting.api.domain.entity.MachineGlueDecompose;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 根据终炼胶分解出对应的母炼胶
 */
public interface MotherGlueDecomposeMapper {

    /**
     * 获取终炼母炼分解表列表数据
     * @param glueAreaMachineList 终炼胶、密炼区、机台列表
     * @return
     */
    List<GlueDecompose> listGlueDecompose(@Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList);

    /**
     * 获取密炼机指定胶料分解表列表数据
     * @param glueAreaMachineList 终炼胶、密炼区、机台列表
     * @return
     */
    List<MachineGlueDecompose> listMachineGlueDecompose(@Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList);

    /**
     * 根据其中一段的母炼胶 查询出 对应的终炼胶分解表的列表数据
     * @param motherGlue 母炼胶名称
     * @return
     */
    List<GlueDecompose> listMotherGlueDecompose(@Param("motherGlue") String motherGlue);

    /**
     * 从配方表中查询出全部终炼、母炼分解信息（按配方类型倒序排）
     * @return
     */
    List<DecomposeRecipeVo> listDecomposeRecipe();
}
