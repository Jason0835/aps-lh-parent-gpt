package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.api.domain.entity.PlmConstructionInfo;

import java.util.List;

/**
 * PLM成型参数信息Service接口
 *
 */
public interface PlmParamsService extends IService<PlmConstructionInfo> {

    /**
     * 查询PLM成型参数信息列表
     *
     * @param params PLM成型参数信息
     * @return PLM成型参数信息集合
     */
    List<PlmConstructionInfo> selectParamsList(PlmConstructionInfo params);
}
