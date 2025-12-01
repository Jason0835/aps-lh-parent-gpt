package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;

/**
 * 硫磺辅料与机台对应Mapper接口
 *
 * @author Liam
 * @date 2022-04-18
 */
public interface AccessoriesMachineMapper extends BaseMapper<AccessoriesMachine> {

    /**
     * 查询硫磺辅料与机台对应列表
     * 将机台编号转换为机台名称并使用逗号拼接
     *
     * @param accessoriesMachine 硫磺辅料与机台对应
     * @return 硫磺辅料与机台对应集合
     */
    List<AccessoriesMachine> selectAccessoriesMachineList(AccessoriesMachine accessoriesMachine);

    /**
     * 批量删除硫磺辅料与机台对应
     * 通过ID列表获取对应的（密炼区+胶料名称），在进行批量删除
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteAccessoriesMachineByIds(Long[] ids);


    /**
     * 批量新增
     *
     * @param list 导入的数据列表
     */
    void batchInsertAccessoriesMachineInfo(@Param("list") List<AccessoriesMachine> list);


    /**
     * 删除对应密炼区和胶料名称的旧记录（物理删除）
     *
     * @param mixArea      密炼区
     * @param materialName 物料名称
     */
    int trueDeleteByMixAreaAndGlue(@Param("mixArea") String mixArea, @Param("materialName") String materialName);

    /**
     * 根据密炼区进行批量删除（物理删除）
     *
     * @param importList 导入的数据列表
     */
    void trueBatchDeleteAccessoriesMachineInfo(@Param("importList") List<AccessoriesMachine> importList);

    /**
     * 根据密炼区和胶料名称进行精确查询
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    List<AccessoriesMachine> selectExactAccessoriesMachineList(AccessoriesMachine accessoriesMachine);

    /**
     * 根据密炼区和胶料名称查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    ArrayList<AccessoriesMachine> getAccessoriesMachineList(AccessoriesMachine accessoriesMachine);
    
    /**
     * 根据密炼区和胶料名称精确查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    ArrayList<AccessoriesMachine> selectRecipeMachineList(AccessoriesMachine accessoriesMachine);
}
