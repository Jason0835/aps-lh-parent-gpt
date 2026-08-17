package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;

/**
 * 内衬MES接口
 *
 * @author APS
 */
public interface IMesItfNcService {

    /**
     * 同步内衬库存。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncStock(AuxReqSyncDataLogs request);

    /**
     * 同步内衬班次完成量并回写结果。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncShiftFinishQty(AuxReqSyncDataLogs request);

    /**
     * 同步内衬日完成量。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncDayFinishQty(AuxReqSyncDataLogs request);

    /**
     * 下发内衬排程结果到MES
     *
     * @param ids         内衬排程结果下发ID列表
     * @param factoryCode 厂别
     * @param companyCode 分公司编码
     * @return 下发结果
     */
    AjaxResult issueNcScheduleResult(Long[] ids, String factoryCode, String companyCode);
}
