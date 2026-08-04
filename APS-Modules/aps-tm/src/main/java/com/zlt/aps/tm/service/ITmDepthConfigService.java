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
     * 校验配置区间的字段合法性、连续性和完整性
     *
     * @param entity 备库班数配置实体
     * @return UserConstants.UNIQUE 表示校验通过，UserConstants.NOT_UNIQUE 表示校验失败
     */
    String checkRangeCross(TmDepthConfig entity);
}
