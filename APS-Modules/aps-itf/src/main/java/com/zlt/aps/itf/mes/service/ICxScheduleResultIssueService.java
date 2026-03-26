package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.CxScheduleResultIssue;

import java.util.List;

/**
 * 成型排程结果下发服务接口
 *
 * @author APS Team
 * @since 2.0.0
 */
public interface ICxScheduleResultIssueService {

    /**
     * 下发成型排程结果到MES
     * 业务规则：
     * 1. 更新当天的2班（早中班，即二班和三班）
     * 2. 更新明天的3班（早中晚班，即一班、二班和三班）
     * 3. 下发后天的3班（早中晚班，即一班、二班和三班）
     *
     * @param cxScheduleResultIssueList 成型排程结果列表
     * @param factoryCode               厂别
     * @param companyCode               分公司编码
     * @return 下发结果
     */
    AjaxResult issueCxScheduleResult(List<CxScheduleResultIssue> cxScheduleResultIssueList, String factoryCode, String companyCode);
}
