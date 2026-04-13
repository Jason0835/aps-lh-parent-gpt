package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 胶囊已使用次数Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ILhRepairCapsuleService extends IDocService<LhRepairCapsule> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<LhRepairCapsule> list);
}
