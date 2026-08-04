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
     * 校验配置区间的字段合法性、连续性和完整性
     *
     * @param entity 胎侧备库班数配置实体
     * @return UserConstants.UNIQUE 表示校验通过，UserConstants.NOT_UNIQUE 表示校验失败
     */
    String checkRangeCross(TcDepthConfig entity);
}
