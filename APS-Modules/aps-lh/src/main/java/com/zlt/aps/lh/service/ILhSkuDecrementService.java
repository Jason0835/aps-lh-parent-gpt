package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhSkuDecrement;
import com.zlt.bill.common.service.IDocService;

/**
 * SKU减量清单服务接口
 */
public interface ILhSkuDecrementService extends IDocService<LhSkuDecrement> {

    /**
     * 获取查询公式
     *
     * @return 查询公式
     */
    String[] getQueryFormulas();

    /**
     * 规范确认减量请求数据
     *
     * @param entity 请求数据
     */
    void normalizeConfirmData(LhSkuDecrement entity);}
