package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;

/**
 * 直裁MES接口服务。
 */
public interface ICd90MesItfService {

    /**
     * 同步直裁库存。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    AjaxResult syncStock(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步直裁库排状态。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    AjaxResult syncStorageLaneLimit(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步直裁自动滚动目标班次库存。
     *
     * @param request 目标库存日期、班次和开始时间
     * @return 同步结果
     */
    AjaxResult syncShiftStock(MesShiftStockSyncRequest request);

    /**
     * 同步直裁每日三班完成量并回写排程结果。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    AjaxResult syncClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs);
}
