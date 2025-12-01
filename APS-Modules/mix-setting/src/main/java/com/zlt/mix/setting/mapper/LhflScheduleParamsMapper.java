package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.entity.LhflScheduleParams;

import java.util.List;

/**
 * 排程参数（硫磺辅料排程设置）Mapper接口
 *
 * @author Liam
 * @date 2022-04-06
 */
public interface LhflScheduleParamsMapper extends BaseMapper<LhflScheduleParams> {

    /**
     * 查询排程参数（硫磺辅料排程设置）列表
     *
     * @param lhflScheduleParams 排程参数（硫磺辅料排程设置）
     * @return 排程参数（硫磺辅料排程设置）集合
     */
    List<LhflScheduleParams> selectLhflScheduleParamsList(LhflScheduleParams lhflScheduleParams);

}
