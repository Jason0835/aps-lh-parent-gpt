package com.zlt.mix.setting.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.GlueLossSetting;

/**
 * 胶料损耗率设定Service接口
 * 
 * @author Joran.zhang
 * @date 2022-05-23
 */
public interface GlueLossSettingService  extends IService<GlueLossSetting>
{
    /**
     * 查询胶料损耗率设定列表
     * 
     * @param glueLossSetting 胶料损耗率设定
     * @return 胶料损耗率设定集合
     */
    List<GlueLossSetting> selectGlueLossSettingList(GlueLossSetting glueLossSetting);

    /**
     * 保存胶料损耗率设定信息（id为空则新增，id不为空则修改）
     *
     * @param glueLossSetting
     */
    void saveGlueLossSetting(GlueLossSetting glueLossSetting);

    /**
     * 批量删除胶料损耗率设定
     * 
     * @param ids 需要删除的胶料损耗率设定ID
     * @return 结果
     */
    int deleteGlueLossSettingByIds(Long[] ids);

    /**
     * 校验胶料损耗率设定唯一性
     */
    String checkGlueLossSettingUnique(GlueLossSetting glueLossSetting);

    /**
     * 导入胶料损耗率设定数据
     */
    AjaxResult importData(List<GlueLossSetting> list, boolean updateSupport, Long importLogId);
}
