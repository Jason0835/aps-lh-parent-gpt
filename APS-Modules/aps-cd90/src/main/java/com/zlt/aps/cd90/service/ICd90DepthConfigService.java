package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ICd90DepthConfigService extends IDocService<Cd90DepthConfig> {
    String checkUnique(Cd90DepthConfig entity);

    String checkRangeCross(Cd90DepthConfig entity);

    AjaxResult importData(List<Cd90DepthConfig> list, boolean updateSupport, Long importLogId);
}