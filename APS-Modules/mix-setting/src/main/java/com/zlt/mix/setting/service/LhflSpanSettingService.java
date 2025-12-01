package com.zlt.mix.setting.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.LhflSpanSetting;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 硫磺辅料跨区设置Service接口
 * 
 * @author chen
 * @date 2022-08-12
 */
public interface LhflSpanSettingService  extends IService<LhflSpanSetting>
{
    /**
     * 查询硫磺辅料跨区设置列表
     * 
     * @param lhflSpanSetting 硫磺辅料跨区设置
     * @return 硫磺辅料跨区设置集合
     */
    List<LhflSpanSetting> selectLhflSpanSettingList(LhflSpanSetting lhflSpanSetting);

    /**
     * 保存硫磺辅料跨区设置信息（id为空则新增，id不为空则修改）
     *
     * @param lhflSpanSetting
     */
    void saveLhflSpanSetting(LhflSpanSetting lhflSpanSetting);

    /**
     * 批量删除硫磺辅料跨区设置
     * 
     * @param ids 需要删除的硫磺辅料跨区设置ID
     * @return 结果
     */
    int deleteLhflSpanSettingByIds(Long[] ids);

    /**
     * 校验硫磺辅料跨区设置唯一性
     */
    String checkLhflSpanSettingUnique(LhflSpanSetting lhflSpanSetting);

    /**
     * 导入硫磺辅料跨区设置数据
     */
    AjaxResult importData(List<LhflSpanSetting> list, boolean updateSupport, Long importLogId);
}
