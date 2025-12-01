package com.zlt.mix.setting.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.entity.SettingScheduleParams;

import java.util.List;

/**
 * 密炼参数信息Mapper接口
 *
 * @author Liam
 * @date 2022-03-14
 */
public interface SettingScheduleParamsMapper extends BaseMapper<SettingScheduleParams> {
    /**
     * 查询密炼参数信息列表
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 密炼参数信息列表
     */
    List<SettingScheduleParams> selectParamsList(SettingScheduleParams settingScheduleParams);
    
    /**
     * 加载指定密炼区的胶料排程参数设置
     *
     * @param settingScheduleParams 密炼参数信息
     * @return 密炼参数信息列表
     */
    List<SettingScheduleParams> selectParamsListMixArea(SettingScheduleParams settingScheduleParams);
}
