package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.bill.common.service.IDocService;

public interface ITcLossSettingService extends IDocService<TcLossSetting> {

    /**
     * 校验损耗率基础数据的损耗率字段。
     *
     * @param entity 待保存或导入的损耗率配置
     */
    void validateLossRate(TcLossSetting entity);
}
