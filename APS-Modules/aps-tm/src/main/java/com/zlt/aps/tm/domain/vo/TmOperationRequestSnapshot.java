package com.zlt.aps.tm.domain.vo;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.vo.TmInsertTaskRequestVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 胎面人工异步任务请求快照。
 */
@Data
public class TmOperationRequestSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 插单请求。 */
    private TmInsertTaskRequestVo insertRequest;

    /** 单条调量或转机台请求。 */
    private TmScheduleResult scheduleResult;

    /** 批量转机台目标机台。 */
    private String targetMachineCode;

    /** 批量转机台请求。 */
    private List<TmScheduleResult> scheduleResultList;

    /** 删除或发布结果ID。 */
    private List<Long> resultIdList;
}
