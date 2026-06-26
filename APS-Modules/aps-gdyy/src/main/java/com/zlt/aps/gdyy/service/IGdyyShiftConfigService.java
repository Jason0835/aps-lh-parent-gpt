package com.zlt.aps.gdyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyShiftConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢带压延班次配置 服务接口。
 */
public interface IGdyyShiftConfigService extends IDocService<GdyyShiftConfig> {

    /**
     * 校验同工厂班次编码唯一。
     */
    String checkUnique(GdyyShiftConfig entity);

    /**
     * 导入班次配置。
     */
    AjaxResult importData(List<GdyyShiftConfig> list, boolean updateSupport, Long importLogId);
}
