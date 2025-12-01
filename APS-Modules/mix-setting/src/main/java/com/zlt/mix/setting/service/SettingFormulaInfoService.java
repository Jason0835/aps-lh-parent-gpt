package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.SettingFormulaInfo;

import java.util.List;

/**
 * 配方信息 Service接口
 *
 * @author Liam
 * @date 2022-03-22
 */
public interface SettingFormulaInfoService extends IService<SettingFormulaInfo> {
    /**
     * 查询配方信息列表
     *
     * @param entity 配方信息
     * @return 配方信息列表
     */
    List<SettingFormulaInfo> selectSettingFormulaInfoList(SettingFormulaInfo entity);

    /**
     * 保存配方信息（id为空则新增，id不为空则修改）
     *
     * @param entity 配方信息
     */
    void saveSettingFormulaInfo(SettingFormulaInfo entity);

    /**
     * 批量删除配方信息
     *
     * @param ids 配方信息的ID数组
     * @return 成功删除的条数
     */
    int deleteByIds(Long[] ids);

    /**
     * 导入配方信息
     *
     * @param list          配方信息列表
     * @param updateSupport 是否更新
     * @param importLogId   导入日志的id
     * @return 操作消息
     */
    AjaxResult importData(List<SettingFormulaInfo> list, boolean updateSupport, Long importLogId);

    /**
     * 判断胶料名称是否已经存在
     *
     * @param entity 配方信息
     * @return 是否存在
     */
    String checkGlueUnique(SettingFormulaInfo entity);
}
