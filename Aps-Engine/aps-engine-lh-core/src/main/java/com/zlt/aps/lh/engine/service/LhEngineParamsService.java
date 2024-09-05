package com.zlt.aps.lh.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.lh.engine.domain.LhEngineParams;

import java.util.List;

/**
 * 硫化参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface LhEngineParamsService extends IService<LhEngineParams> {
    /**
     * 查询硫化参数信息
     *
     * @param id 硫化参数信息ID
     * @return 硫化参数信息
     */
    public LhEngineParams selectParamsById(Long id);

    /**
     * 查询硫化参数信息列表
     *
     * @param params 硫化参数信息     * @return 硫化参数信息集合
     */
    public List<LhEngineParams> selectParamsList(LhEngineParams params);

}
