package com.zlt.aps.common.engine.mapper;


import com.zlt.aps.common.engine.domain.CxEngineQuotaSetting;

import java.util.List;

/**
 * 成型定额设定mapper接口
 */
public interface CxEngineQuotaSettingMapper {

    /**
     * 加载成型机机台定额信息
     * @param cxEngineQuotaSetting
     * @return
     */
    public List<CxEngineQuotaSetting> selectCxEngineQuotaSettingList(CxEngineQuotaSetting cxEngineQuotaSetting);
}
