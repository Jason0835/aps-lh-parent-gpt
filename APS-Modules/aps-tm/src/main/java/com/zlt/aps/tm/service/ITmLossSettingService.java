package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.entity.TmLossSetting;
import com.zlt.bill.common.service.IDocService;

public interface ITmLossSettingService extends IDocService<TmLossSetting> {

    /**
     * 校验损耗率基础数据的损耗率字段。
     *
     * @param entity 待保存或导入的损耗率配置
     */
    void validateLossRate(TmLossSetting entity);
}
