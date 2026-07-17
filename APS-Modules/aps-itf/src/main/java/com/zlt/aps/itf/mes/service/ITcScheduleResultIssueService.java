package com.zlt.aps.itf.mes.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;

import java.util.List;

/**
 * 胎侧排程结果MES下发服务。
 */
public interface ITcScheduleResultIssueService {

    /**
     * 下发胎侧排程结果。
     *
     * @param issueList 下发记录
     * @return 下发结果
     */
    AjaxResult issue(List<TcScheduleResultIssue> issueList);

    /**
     * 查询指定数据版本的MES处理结果。
     *
     * @param dataVersion 数据版本
     * @return 发布反馈
     */
    TcReleaseFeedbackVo queryStatus(String dataVersion);
}
