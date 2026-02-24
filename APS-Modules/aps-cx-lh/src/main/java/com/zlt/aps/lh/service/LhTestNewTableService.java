package com.zlt.aps.lh.service;

import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.entity.LhTestNewTable;
import com.zlt.bill.common.service.IDocService;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
public interface LhTestNewTableService extends IDocService<LhTestNewTable> {
    /**
     * 更新生胎号
     * @param autoLhScheduleResultDTO
     * @return
     */
    void updateEmbryoCode(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException;

}
