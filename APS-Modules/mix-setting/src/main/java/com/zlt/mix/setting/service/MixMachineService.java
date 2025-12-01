package com.zlt.mix.setting.service;

import java.util.ArrayList;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 密炼机台信息Service接口
 * 
 * @author Gim
 * @date 2022-03-22
 */
public interface MixMachineService  extends IService<MixMachine>
{
    /**
     * 查询密炼机台信息列表
     * 
     * @param mixMachine 密炼机台信息
     * @return 密炼机台信息集合
     */
    List<MixMachine> selectMixMachineList(MixMachine mixMachine);

    /**
     * 保存密炼机台信息信息（id为空则新增，id不为空则修改）
     *
     * @param mixMachine
     */
    void saveMixMachine(MixMachine mixMachine);

    /**
     * 批量删除密炼机台信息
     * 
     * @param ids 需要删除的密炼机台信息ID
     * @return 结果
     */
    int deleteMixMachineByIds(Long[] ids);

    /**
     * 校验密炼机台信息唯一性
     */
    String checkMixMachineUnique(MixMachine mixMachine);
    /**
     * 校验密炼机台信息唯一性
     */
    String checkMixMachineUnique2(MixMachine mixMachine);

    /**
     * 导入密炼机台信息数据
     */
    AjaxResult importData(List<MixMachine> list, boolean updateSupport, Long importLogId);

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * @return 查询到的机台信息
     */
    public ArrayList<MixMachine> getAllMachineInfo();
}
