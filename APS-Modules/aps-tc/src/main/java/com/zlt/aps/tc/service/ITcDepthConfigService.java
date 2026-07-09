package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcDepthConfig;
import com.zlt.bill.common.service.IDocService;

/**
 * 胎侧备库班数配置Service接口
 *
 * @author zlt
 */
public interface ITcDepthConfigService extends IDocService<TcDepthConfig> {

    /**
     * 校验配置规则的交叉情况
     * 校验新增/修改的规则是否与现有规则存在范围交叉
     *
     * @param entity 胎侧备库班数配置实体
     * @return UserConstants.UNIQUE 表示无交叉（校验通过），UserConstants.NOT_UNIQUE 表示存在交叉
     */
    String checkRangeCross(TcDepthConfig entity);
}