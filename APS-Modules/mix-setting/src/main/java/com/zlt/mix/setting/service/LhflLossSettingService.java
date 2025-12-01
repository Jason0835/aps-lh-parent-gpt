package com.zlt.mix.setting.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.LhflLossSetting;

/**
 * 硫磺辅料耗损率设定Service接口
 * 
 * @author Joran.zhang
 * @date 2022-05-23
 */
public interface LhflLossSettingService extends IService<LhflLossSetting>
{
    /**
     * 查询硫磺辅料耗损率设定列表
     * 
     * @param lhflLossrateSetting 硫磺辅料耗损率设定
     * @return 硫磺辅料耗损率设定集合
     */
    List<LhflLossSetting> selectLhflLossSettingList(LhflLossSetting lhflLossrateSetting);

    /**
     * 保存硫磺辅料耗损率设定信息（id为空则新增，id不为空则修改）
     *
     * @param lhflLossrateSetting
     */
    void saveLhflLossSetting(LhflLossSetting lhflLossrateSetting);

    /**
     * 批量删除硫磺辅料耗损率设定
     * 
     * @param ids 需要删除的硫磺辅料耗损率设定ID
     * @return 结果
     */
    int deleteLhflLossSettingByIds(Long[] ids);

    /**
     * 校验硫磺辅料耗损率设定唯一性
     */
    String checkLhflLossSettingUnique(LhflLossSetting lhflLossrateSetting);

    /**
     * 导入硫磺辅料耗损率设定数据
     */
    AjaxResult importData(List<LhflLossSetting> list, boolean updateSupport, Long importLogId);
}
