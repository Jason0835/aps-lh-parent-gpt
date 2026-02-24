package com.zlt.aps.mp.itf.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;

import java.io.UnsupportedEncodingException;

/**
 * 内销销售订单接口
 *
 * @author Chen
 * @date 2025/4/8
 */
public interface IInSaleOrderSyncService {

    /**
     * 同步内销销售订单接口
     *
     * @param inSaleOrderDto 查询参数
     * @return 结果
     * @throws UnsupportedEncodingException 异常
     * @throws IllegalAccessException       异常
     */
    AjaxResult syncInSaleOrder(InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException;

    /**
     * 同步内销历史销售订单接口
     *
     * @param inSaleOrderDto 查询参数
     * @return 结果
     * @throws UnsupportedEncodingException 异常
     * @throws IllegalAccessException       异常
     */
    AjaxResult syncInHisSaleOrder(InSaleOrderDto inSaleOrderDto) throws UnsupportedEncodingException, IllegalAccessException;

    /**
     * 同步外销销售订单接口
     *
     * @param inSaleOrderDto 查询参数
     * @return 结果
     */
    AjaxResult syncOutSaleOrder(InSaleOrderDto inSaleOrderDto);

}
