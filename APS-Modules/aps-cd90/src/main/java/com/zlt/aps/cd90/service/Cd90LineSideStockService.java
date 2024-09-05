package com.zlt.aps.cd90.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;

/**
 * 90°裁断线边库存信息Service接口
 *
 * @author hak
 * @date 2023-03-03
 */
public interface Cd90LineSideStockService {

    /**
     * 查询90°裁断库存信息列表
     *
     * @param stock 90°裁断库存信息
     * @return 90°裁断库存信息集合
     */
    List<Cd90LineSideStock> selectStockList(Cd90LineSideStock stock);

    /**
     * 到MES同步库存数据
     *
     * @return 
     */
    AjaxResult syncStock();
}
