package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 硫化在机信息Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ILhMachineOnlineInfoService extends IDocService<LhMachineOnlineInfo> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<LhMachineOnlineInfo> list);
}
