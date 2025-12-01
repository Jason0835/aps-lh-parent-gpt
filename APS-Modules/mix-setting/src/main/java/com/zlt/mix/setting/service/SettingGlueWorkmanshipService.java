package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.SettingGlueWorkmanship;

import java.util.List;

/**
 * 分厂胶料工艺信息 Service接口
 *
 * @author Liam
 * @date 2022-03-18
 */
public interface SettingGlueWorkmanshipService extends IService<SettingGlueWorkmanship> {

    /**
     * 获取分厂胶料工艺信息列表
     *
     * @param entity 分厂胶料工艺信息
     * @return 分厂胶料工艺信息列表
     */
    List<SettingGlueWorkmanship> selectSettingGlueWorkmanshipList(SettingGlueWorkmanship entity);

    /**
     * 保存分厂胶料工艺信息（id为空则新增，id不为空则修改）
     *
     * @param entity 分厂胶料工艺信息
     */
    void saveGlueWorkmanship(SettingGlueWorkmanship entity);

    /**
     * 批量逻辑删除分厂胶料工艺信息
     *
     * @param ids 需要进行逻辑删除的id数组
     * @return 逻辑删除的数目
     */
    int deleteByIds(Long[] ids);

    /**
     * 导入分厂胶料工艺信息
     *
     * @param list          分厂胶料工艺信息列表
     * @param updateSupport 是否更新
     * @param importLogId   导入日志的ID
     * @return 操作消息
     */
    AjaxResult importData(List<SettingGlueWorkmanship> list, boolean updateSupport, Long importLogId);
}
