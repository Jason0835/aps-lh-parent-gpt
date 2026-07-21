package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;

import java.util.List;

/** 斜裁排程结果下发 MES 服务。 */
public interface ICd15ScheduleResultIssueService {

    /**
     * 下发斜裁排程结果。
     *
     * @param issueList 按班次展开的斜裁结果
     * @param factoryCode 工厂编码
     * @param companyCode 公司编码
     * @return 下发结果
     */
    AjaxResult issueCd15ScheduleResult(
            List<Cd15ScheduleResultIssue> issueList,
            String factoryCode,
            String companyCode);
}
