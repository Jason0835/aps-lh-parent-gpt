package com.zlt.aps.cd15.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;

/**
 * 15°裁断线边库存信息Service接口
 *
 * @author hak
 * @date 2023-03-03
 */
public interface Cd15LineSideStockService {

    /**
     * 查询15°裁断库存信息列表
     *
     * @param stock 15°裁断库存信息
     * @return 15°裁断库存信息集合
     */
    List<Cd15LineSideStock> selectStockList(Cd15LineSideStock stock);
}
