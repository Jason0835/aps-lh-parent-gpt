package com.zlt.mix.setting.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.RemindSetting;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 提醒设备Service接口
 *
 * @author Gim
 * @date 2022-03-23
 */
public interface RemindSettingService extends IService<RemindSetting> {
    /**
     * 查询提醒设备列表
     *
     * @param remindSetting 提醒设备
     * @return 提醒设备集合
     */
    List<RemindSetting> selectRemindSettingList(RemindSetting remindSetting);

    /**
     * 保存提醒设备信息（id为空则新增，id不为空则修改）
     *
     * @param remindSetting
     */
    void saveRemindSetting(RemindSetting remindSetting);

    /**
     * 批量删除提醒设备
     *
     * @param ids 需要删除的提醒设备ID
     * @return 结果
     */
    int deleteRemindSettingByIds(Long[] ids);

    /**
     * 校验提醒设备唯一性
     */
    String checkRemindSettingUnique(RemindSetting remindSetting);

    /**
     * 导入提醒设备数据
     */
    AjaxResult importData(List<RemindSetting> list, boolean updateSupport, Long importLogId);
}
