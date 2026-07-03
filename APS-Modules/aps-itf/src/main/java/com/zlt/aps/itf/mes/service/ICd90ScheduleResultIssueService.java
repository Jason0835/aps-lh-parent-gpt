package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue;

import java.util.List;

/**
 * 直裁排程结果下发 MES 服务接口。
 *
 * <p>接收 APS 侧已按班次展开的下发列表，按排班日期分组写入 MES 中间表：
 * day1/day2 走 upsert（存在则更新，不存在则插入），day3 走 delete + insert（先删后插）。
 * 完成后发送 MQ 通知 MES 拉取。</p>
 *
 * @author APS Team
 * @since 2.0.0
 */
public interface ICd90ScheduleResultIssueService {

    /**
     * 下发直裁排程结果到 MES。
     *
     * @param cd90ScheduleResultIssueList 直裁排程结果下发列表（已按班次展开）
     * @param factoryCode 厂别
     * @param companyCode 分公司编码
     * @return 下发结果
     */
    AjaxResult issueCd90ScheduleResult(List<Cd90ScheduleResultIssue> cd90ScheduleResultIssueList,
                                       String factoryCode, String companyCode);
}
