package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Date;

/**
 * 斜裁库存管理 Service。
 */
public interface ICd15StockService extends IDocService<Cd15Stock> {

    /**
     * 校验同工厂、库存日期、物料编号库存唯一性。
     *
     * @param entity 斜裁库存
     * @return 唯一性结果
     */
    String checkUnique(Cd15Stock entity);

    /**
     * 导入斜裁库存。
     *
     * @param list          导入数据
     * @param updateSupport 是否更新已有数据
     * @param importLogId   导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15Stock> list, boolean updateSupport, Long importLogId);

    /** 替换指定工厂和库存日期的MES快照。 */
    void logicDeleteAndSaveBatch(String factoryCode, Date stockDate,
                                 String updateBy, List<Cd15Stock> stockList);
}
