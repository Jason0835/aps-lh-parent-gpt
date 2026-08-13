package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

public interface ITmStockService extends IDocService<TmStock> {

    /**
     * 逻辑删除并批量保存胎面库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param stockDate 库存日期
     * @param updateBy  更新者
     * @param list      待插入的胎面库存列表
     */
    void logicDeleteAndSaveBatch(Date stockDate, String updateBy, List<TmStock> list);

    /**
     * 替换指定工厂和库存日期的胎面库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate 库存日期
     * @param updateBy 更新人
     * @param stockList MES库存列表，空集合表示清空快照
     */
    void replaceStock(String factoryCode, Date stockDate, String updateBy, List<TmStock> stockList);
}
