package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 成型在机信息Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ICxMachineOnlineInfoService extends IDocService<CxMachineOnlineInfo> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<CxMachineOnlineInfo> list);

    /**
     * 逻辑删除分厂旧数据并批量插入新数据（事务性操作）
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param insertList  待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, String updateBy, List<CxMachineOnlineInfo> insertList);
}
