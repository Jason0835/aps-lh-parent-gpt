package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResultIssue;

import java.util.List;

/**
 * 钢丝圈排程结果下发服务接口
 *
 * @author APS
 */
public interface IGsqScheduleResultIssueService {

    /**
     * 下发钢丝圈排程结果到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（钢丝圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（钢丝圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（钢丝圈5/6班→MES夜/早班），中班尚未排产不下发
     * 钢丝圈6班覆盖胎圈1~6班消耗量，TQ_CLASS1~6_PLAN全量传递
     *
     * @param gsqScheduleResultIssueList 钢丝圈排程结果下发列表（已按3天拆分）
     * @param factoryCode                厂别
     * @param companyCode                分公司编码
     * @return 下发结果
     */
    AjaxResult issueGsqScheduleResult(List<GsqScheduleResultIssue> gsqScheduleResultIssueList, String factoryCode, String companyCode);
}
