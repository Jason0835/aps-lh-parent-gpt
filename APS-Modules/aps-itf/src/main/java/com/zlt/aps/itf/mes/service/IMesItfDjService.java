package com.zlt.aps.itf.mes.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;

/**
 * 垫胶MES接口
 *
 * @author APS
 */
public interface IMesItfDjService {

    /**
     * 同步垫胶库存。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncStock(AuxReqSyncDataLogs request);

    /**
     * 同步垫胶班次完成量并回写结果。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncShiftFinishQty(AuxReqSyncDataLogs request);

    /**
     * 同步垫胶日完成量。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    AjaxResult syncDayFinishQty(AuxReqSyncDataLogs request);

    /**
     * 下发垫胶排程结果到MES
     *
     * @param djScheduleResultIssueList 垫胶排程结果下发列表（已按3天拆分）
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    AjaxResult issueDjScheduleResult(List<DjScheduleResult> djScheduleResultIssueList, String factoryCode, String companyCode);
}
