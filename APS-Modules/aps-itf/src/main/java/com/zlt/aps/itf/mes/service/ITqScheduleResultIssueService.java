package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResultIssue;

import java.util.List;

/**
 * 胎圈排程结果下发服务接口
 *
 * @author APS
 */
public interface ITqScheduleResultIssueService {

    /**
     * 下发胎圈排程结果到MES
     * 业务规则：
     * 1. D日（今天）：更新中班数据（胎圈1班→MES中班），夜班早班已过不下发
     * 2. D+1日（明天）：更新夜早中3班数据（胎圈2/3/4班→MES夜/早/中班）
     * 3. D+2日（后天）：先删后插夜早2班数据（胎圈5/6班→MES夜/早班），中班尚未排产不下发
     * 胎圈6班覆盖成型3~8班，CX_CLASS3~8_PLAN全量传递
     *
     * @param tqScheduleResultIssueList 胎圈排程结果下发列表（已按3天拆分）
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    AjaxResult issueTqScheduleResult(List<TqScheduleResultIssue> tqScheduleResultIssueList, String factoryCode, String companyCode);
}
