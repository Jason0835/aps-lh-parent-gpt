package com.zlt.aps.lh.service;

import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import com.zlt.bill.common.service.IDocService;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
public interface LhTestScheduleResultService extends IDocService<LhTestScheduleResult> {
    /**
     * 硫化TEST自动排程
     * @param autoLhScheduleResultDTO
     * @return
     */
    void autoLhTestScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException;

}
