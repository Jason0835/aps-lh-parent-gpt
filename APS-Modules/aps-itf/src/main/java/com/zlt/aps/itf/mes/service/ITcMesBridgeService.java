package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;

/**
 * 胎侧MES库存和完成量同步桥接服务。
 */
public interface ITcMesBridgeService {

    /**
     * 同步胎侧库存。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncStock(AuxReqSyncDataLogs request);

    /**
     * 同步胎侧自动滚动班次库存。
     *
     * @param request 工厂、物理库存日和班序
     * @return 同步结果
     */
    AjaxResult syncShiftStock(MesShiftStockSyncRequest request);

    /**
     * 同步胎侧班次完成量并回写结果。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncShiftFinishQty(AuxReqSyncDataLogs request);

    /**
     * 同步胎侧日完成量。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncDayFinishQty(AuxReqSyncDataLogs request);
}
