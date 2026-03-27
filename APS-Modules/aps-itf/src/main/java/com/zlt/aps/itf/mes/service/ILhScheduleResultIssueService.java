package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;

import java.util.List;

/**
 * 硫化排程结果下发服务接口
 *
 * @author APS Team
 * @since 2.0.0
 */
public interface ILhScheduleResultIssueService {

    /**
     * 硫化排程结果下发到MES
     * 业务规则：
     * 1. 更新当天的2班（早中班）- 清空一班数据
     * 2. 更新明天的3班（早中晚班）
     * 3. 下发后天的3班（早中晚班）
     *
     * @param lhScheduleResultIssueList 硫化排程结果列表
     * @param factoryCode 工厂编码
     * @param companyCode 公司编码
     * @return 结果
     */
    AjaxResult issueLhScheduleResult(List<LhScheduleResultIssue> lhScheduleResultIssueList, String factoryCode, String companyCode);
}
