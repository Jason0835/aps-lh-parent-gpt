package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;

import java.util.List;

/**
 * 小料机台信息Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
public interface LhflMachineService extends IService<LhflMachine> {
    /**
     * 查询小料机台信息列表
     *
     * @param lhflMachine 小料机台信息
     * @return 小料机台信息集合
     */
    List<LhflMachine> selectLhflMachineList(LhflMachine lhflMachine);

    /**
     * 保存小料机台信息信息（id为空则新增，id不为空则修改）
     *
     * @param lhflMachine
     */
    void saveLhflMachine(LhflMachine lhflMachine);

    /**
     * 批量删除小料机台信息
     *
     * @param ids 需要删除的小料机台信息ID
     * @return 结果
     */
    int deleteLhflMachineByIds(Long[] ids);

    /**
     * 校验小料机台信息唯一性
     * 密炼区+机台编号
     */
    String checkLhflMachineUnique(LhflMachine lhflMachine);

    /**
     * 校验小料机台信息唯一性
     * 密炼区+机台名称
     */
    String checkLhflMachineUnique2(LhflMachine lhflMachine);

    /**
     * 导入小料机台信息数据
     */
    AjaxResult importData(List<LhflMachine> list, boolean updateSupport, Long importLogId);
}
