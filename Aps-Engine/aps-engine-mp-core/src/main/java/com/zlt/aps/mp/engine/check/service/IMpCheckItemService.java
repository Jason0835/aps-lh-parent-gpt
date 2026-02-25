package com.zlt.aps.mp.engine.check.service;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.api.domain.vo.MpCheckItemVo;

import java.util.List;

/**
 * 检测项检测Service服务
 *
 * @author hsc
 * @since 2026/01/30
 */
public interface IMpCheckItemService {

    /**
     * 检测项检测
     *
     * @param factoryProductionParamVo
     * @return
     */
    public List<MpCheckItemVo> check(Context context);
}
