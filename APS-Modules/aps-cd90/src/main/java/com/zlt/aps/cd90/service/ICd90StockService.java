package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

public interface ICd90StockService extends IDocService<Cd90Stock> {
    String checkUnique(Cd90Stock entity);
    AjaxResult importData(List<Cd90Stock> list, boolean updateSupport, Long importLogId);

    /**
     * 逻辑删除并批量保存直裁库存（MES 同步专用）。
     * <p>步骤 1：逻辑删除指定工厂、数据来源、库存日期、班次的未删除记录；</p>
     * <p>步骤 2：批量插入 MES 同步的新数据。</p>
     *
     * @param factoryCode 工厂编码
     * @param dataSource  数据来源（0-MES，1-人工）
     * @param stockDate   库存日期
     * @param shiftCode   班次
     * @param updateBy    更新人
     * @param insertList  待插入的库存列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, String dataSource, Date stockDate, String shiftCode,
                                 String updateBy, List<Cd90Stock> insertList);
}