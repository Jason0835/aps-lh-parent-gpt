package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 斜裁备库班数与供成型机数配置业务接口。
 */
public interface ICd15DepthConfigService extends IDocService<Cd15DepthConfig> {

    String checkUnique(Cd15DepthConfig entity);

    String checkRangeCross(Cd15DepthConfig entity);

    AjaxResult importData(List<Cd15DepthConfig> list, boolean updateSupport, Long importLogId);
}
