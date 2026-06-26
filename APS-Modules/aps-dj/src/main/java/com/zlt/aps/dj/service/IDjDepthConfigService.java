package com.zlt.aps.dj.service;

import com.zlt.aps.dj.api.domain.entity.DjDepthConfig;
import com.zlt.bill.common.service.IDocService;

/**
 * 垫胶备库班数与供成型机数配置Service接口
 *
 * @author zlt
 */
public interface IDjDepthConfigService extends IDocService<DjDepthConfig> {

    /**
     * 校验配置规则的交叉情况
     * 校验新增/修改的规则是否与现有规则存在范围交叉
     *
     * @param entity 深度配置实体
     * @return UserConstants.UNIQUE 表示无交叉（校验通过），UserConstants.NOT_UNIQUE 表示存在交叉
     */
    String checkRangeCross(DjDepthConfig entity);
}
