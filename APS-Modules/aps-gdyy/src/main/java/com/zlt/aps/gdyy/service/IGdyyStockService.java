package com.zlt.aps.gdyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢带压延库存 服务接口。
 */
public interface IGdyyStockService extends IDocService<GdyyStock> {

    /**
     * 校验工厂+库存日期+大卷编号唯一性。
     */
    String checkUnique(GdyyStock entity);

    /**
     * 导入库存数据。
     */
    AjaxResult importData(List<GdyyStock> list, boolean updateSupport, Long importLogId);
}
