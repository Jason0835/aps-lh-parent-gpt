package com.zlt.mix.setting.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.GlueSpanSetting;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 终炼母炼胶料跨区设置Service接口
 * 
 * @author chen
 * @date 2022-08-12
 */
public interface GlueSpanSettingService  extends IService<GlueSpanSetting>
{
    /**
     * 查询终炼母炼胶料跨区设置列表
     * 
     * @param glueSpanSetting 终炼母炼胶料跨区设置
     * @return 终炼母炼胶料跨区设置集合
     */
    List<GlueSpanSetting> selectGlueSpanSettingList(GlueSpanSetting glueSpanSetting);

    /**
     * 保存终炼母炼胶料跨区设置信息（id为空则新增，id不为空则修改）
     */
    void saveGlueSpanSetting(GlueSpanSetting glueSpanSetting);

    /**
     * 批量删除终炼母炼胶料跨区设置
     * 
     * @param ids 需要删除的终炼母炼胶料跨区设置ID
     * @return 结果
     */
    int deleteGlueSpanSettingByIds(Long[] ids);

    /**
     * 校验终炼母炼胶料跨区设置唯一性
     */
    String checkGlueSpanSettingUnique(GlueSpanSetting glueSpanSetting);

    /**
     * 导入终炼母炼胶料跨区设置数据
     */
    AjaxResult importData(List<GlueSpanSetting> list, boolean updateSupport, Long importLogId);
}
