package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.entity.TmDepthConfig;
import com.zlt.bill.common.service.IDocService;

/**
 * 备库班数配置Service接口
 *
 * @author zlt
 */
public interface ITmDepthConfigService extends IDocService<TmDepthConfig> {

    /**
     * 校验配置规则的交叉情况
     * 校验新增/修改的规则是否与现有规则存在范围交叉
     *
     * @param entity 备库班数配置实体
     * @return UserConstants.UNIQUE 表示无交叉（校验通过），UserConstants.NOT_UNIQUE 表示存在交叉
     */
    String checkRangeCross(TmDepthConfig entity);
}
