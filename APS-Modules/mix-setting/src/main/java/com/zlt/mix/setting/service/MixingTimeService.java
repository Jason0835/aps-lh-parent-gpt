package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.dto.MixingTimeDto;
import com.zlt.mix.setting.api.domain.entity.MixingTime;

import java.util.List;

/**
 * 炼胶时间信息Service接口
 *
 * @author Liam
 * @date 2022-03-31
 */
public interface MixingTimeService extends IService<MixingTime> {
    /**
     * 查询炼胶时间信息列表
     *
     * @param mixingTime 炼胶时间信息
     * @return 炼胶时间信息集合
     */
    List<MixingTimeDto> selectMixingTimeList(MixingTime mixingTime);

    /**
     * 保存炼胶时间信息信息（id为空则新增，id不为空则修改）
     *
     * @param mixingTime
     */
    void saveMixingTime(MixingTime mixingTime);

    /**
     * 批量删除炼胶时间信息
     *
     * @param ids 需要删除的炼胶时间信息ID
     * @return 结果
     */
    int deleteMixingTimeByIds(Long[] ids);

    /**
     * 校验炼胶时间信息唯一性
     */
    String checkMixingTimeUnique(MixingTime mixingTime);

    /**
     * 导入炼胶时间信息数据
     */
    AjaxResult importData(List<MixingTimeDto> list, boolean updateSupport, Long importLogId);
}
