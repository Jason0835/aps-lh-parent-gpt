package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;

import java.util.List;

/**
 * 排程参数（硫磺辅料排程设置）Service接口
 *
 * @author Liam
 * @date 2022-04-06
 */
public interface LhflScheduleParamsService extends IService<LhflScheduleParams> {
    /**
     * 查询排程参数（硫磺辅料排程设置）列表
     *
     * @param lhflScheduleParams 排程参数（硫磺辅料排程设置）
     * @return 排程参数（硫磺辅料排程设置）集合
     */
    List<LhflScheduleParams> selectLhflScheduleParamsList(LhflScheduleParams lhflScheduleParams);

    /**
     * 保存排程参数（硫磺辅料排程设置）信息（id为空则新增，id不为空则修改）
     *
     * @param lhflScheduleParams
     */
    AjaxResult saveLhflScheduleParams(LhflScheduleParams lhflScheduleParams);

    /**
     * 复制排程参数（硫磺辅料排程设置）信息
     *
     * @param lhflScheduleParams
     */
    AjaxResult copyLhflScheduleParams(LhflScheduleParams lhflScheduleParams);
}
