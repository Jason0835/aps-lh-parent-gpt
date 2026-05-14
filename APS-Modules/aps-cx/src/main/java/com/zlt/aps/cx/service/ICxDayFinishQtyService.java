package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 成型排程日完成量Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ICxDayFinishQtyService extends IDocService<CxDayFinishQty> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<CxDayFinishQty> list);

    /**
     * 逻辑删除分厂指定完成日期的旧数据并批量插入新数据（事务性操作）
     *
     * @param factoryCode 分厂编号
     * @param finishDate  完成日期
     * @param updateBy    更新者
     * @param insertList  待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date finishDate, String updateBy, List<CxDayFinishQty> insertList);
}
