package com.zlt.aps.tc.service;

import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

public interface ITcStockService extends IDocService<TcStock> {

    /**
     * 逻辑删除并批量保存胎侧库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param stockDate 库存日期
     * @param updateBy  更新者
     * @param list      待插入的胎侧库存列表
     */
    void logicDeleteAndSaveBatch(Date stockDate, String updateBy, List<TcStock> list);
}