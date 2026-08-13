package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;

/**
 * 斜裁MES接口服务。
 */
public interface ICd15MesItfService {

    /** 同步斜裁库存。 */
    AjaxResult syncStock(AuxReqSyncDataLogs syncDataLogs);

    /** 同步斜裁库排状态。 */
    AjaxResult syncStorageLaneLimit(AuxReqSyncDataLogs syncDataLogs);

    /** 同步斜裁自动滚动目标班次库存。 */
    AjaxResult syncShiftStock(MesShiftStockSyncRequest request);
}
