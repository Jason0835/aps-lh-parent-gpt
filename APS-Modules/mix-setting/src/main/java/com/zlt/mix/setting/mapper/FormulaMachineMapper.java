package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方与机台对应Mapper接口
 *
 * @author Gim
 * @date 2022-03-28
 */
public interface FormulaMachineMapper extends BaseMapper<FormulaMachine> {

    /**
     * 查询配方与机台对应列表
     *
     * @param formulaMachine 配方与机台对应
     * @return 配方与机台对应集合
     */
    List<FormulaMachine> selectFormulaMachineList(FormulaMachine formulaMachine);

    List<FormulaMachine> selectByIds(Long[] ids);

    /**
     * 批量新增
     *
     * @param list
     */
    void batchInsertFormulaMachineInfo(@Param("list") List<FormulaMachine> list);


    /**
     * 批量删除
     *
     * @param importList 导入的配方与机台对应列表
     */
    void batchDeleteFormulaMachineInfo(@Param("importList") List<FormulaMachine> importList);

    /**
     * 根据密炼区和胶料名称进行物理删除
     *
     * @param mixArea 密炼区
     * @param glue    胶料名称
     */
    void trueDeleteByMixAreaAndGlue(@Param("mixArea") String mixArea, @Param("glue") String glue);

    /**
     * 进行批量的逻辑删除
     *
     * @param importList 导入的配方与机台对应列表
     */
    void trueBatchDeleteFormulaMachineInfo(@Param("importList") List<FormulaMachine> importList);

    /**
     * 根据机台名称和胶料名称进行精确查询
     *
     * @param machine
     * @return
     */
    List<FormulaMachine> selectExactFormulaMachineList(FormulaMachine machine);

    /**
     * 根据密炼区和胶料名称查询配方与机台对应信息
     *
     * @param machine 参数
     * @return 查询到的集合
     */
    ArrayList<FormulaMachine> getFormulaMachineList(FormulaMachine machine);

    /**
     * 根据机台名称和胶料名称进行精确查询
     *
     * @param machine
     * @return
     */
    ArrayList<FormulaMachine> selectRecipeMachineList(FormulaMachine machine);
}
