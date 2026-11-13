package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 人工操作执行结果。
 *
 * <p>用于在保持既有影响行数兼容返回的同时，向异步任务传递自动排程风格的问题明细。</p>
 */
@Data
public class ManualOperationResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最终影响行数。
     */
    private int affectedCount;

    /**
     * 可继续执行的问题明细。
     */
    private List<AutoScheduleIssueVo> issues = new ArrayList<>();

    /**
     * 设置问题明细，空值统一转换为空列表。
     *
     * @param issues 问题明细
     */
    public void setIssues(List<AutoScheduleIssueVo> issues) {
        this.issues = issues == null ? new ArrayList<>() : issues;
    }
}
