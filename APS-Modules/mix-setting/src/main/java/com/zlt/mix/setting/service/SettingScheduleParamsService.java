package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;

import java.util.List;


/**
 * 密炼参数信息Service接口
 *
 * @author Liam
 * @date 2022-03-14
 */
public interface SettingScheduleParamsService extends IService<SettingScheduleParams> {

    /**
     * 查询密炼参数信息列表
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 密炼参数信息列表
     */
    List<SettingScheduleParams> selectParamsList(SettingScheduleParams settingScheduleParams);

    /**
     * 获取密炼参数信息详细信息
     *
     * @param id 密炼参数信息ID
     * @return 密炼参数信息
     */
    SettingScheduleParams selectParamsById(Long id);


    /**
     * 修改密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    AjaxResult updateParams(SettingScheduleParams settingScheduleParams);

    /**
     * 复制密炼参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    AjaxResult copyScheduleParams(SettingScheduleParams settingScheduleParams);

    /**
     * 加载指定密炼区参数信息
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 操作消息
     */
    AjaxResult selectParamsListMixArea(SettingScheduleParams settingScheduleParams);
}
