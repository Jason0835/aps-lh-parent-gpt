package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.PlmConstructionInfo;

import java.util.List;

/**
 * PLM参数信息Mapper接口
 *
 */
public interface PlmParamsMapper extends BaseMapper<PlmConstructionInfo> {
    /**
     * 查询参数集合
     *
     * @param params 查询条件
     * @return 查询到的结果
     */
    List<PlmConstructionInfo> listParams(PlmConstructionInfo params);
}
