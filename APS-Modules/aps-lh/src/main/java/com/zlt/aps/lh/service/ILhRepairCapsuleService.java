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

    /**
     * 查询导出数据
     *
     * @param obj 查询条件
     * @return 结果
     */
    String[] getQueryFormulas();

    /**
     * 逻辑删除分厂旧数据并批量插入新数据（事务性操作）
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param insertList  待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, String updateBy, List<LhRepairCapsule> insertList);
}
