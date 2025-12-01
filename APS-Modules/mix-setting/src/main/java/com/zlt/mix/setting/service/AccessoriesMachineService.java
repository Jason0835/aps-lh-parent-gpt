package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * 硫磺辅料与机台对应Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
public interface AccessoriesMachineService extends IService<AccessoriesMachine> {
    /**
     * 查询硫磺辅料与机台对应列表
     *
     * @param accessoriesMachine 硫磺辅料与机台对应
     * @return 硫磺辅料与机台对应集合
     */
    List<AccessoriesMachine> selectAccessoriesMachineList(AccessoriesMachine accessoriesMachine);

    /**
     * 保存硫磺辅料与机台对应信息（id为空则新增，id不为空则修改）
     *
     * @param accessoriesMachine
     */
    void saveAccessoriesMachine(AccessoriesMachine accessoriesMachine);

    /**
     * 批量删除硫磺辅料与机台对应
     *
     * @param ids 需要删除的硫磺辅料与机台对应ID
     * @return 结果
     */
    int deleteAccessoriesMachineByIds(Long[] ids);

    /**
     * 校验硫磺辅料与机台对应唯一性
     */
    String checkAccessoriesMachineUnique(AccessoriesMachine accessoriesMachine);

    /**
     * 导入硫磺辅料与机台对应数据
     */
    AjaxResult importData(List<AccessoriesMachine> list, boolean updateSupport, Long importLogId);

    /**
     * 根据密炼区和胶料名称进行精确查询
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
    ArrayList<AccessoriesMachine> listRecipeMachine(AccessoriesMachine accessoriesMachine);
}
