package com.zlt.aps.mp.demand.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanSaleOrder;
import com.zlt.aps.monthplan.api.domain.itf.InDataListVo;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanSaleOrderService.java
 * 描    述：IMonthPlanSaleOrderService月度销售计划订单后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
public interface IMonthPlanSaleOrderService {
    /**
     * 根据查询条件，获取列表数据
     *
     * @param queryWrapper
     * @return
     */
    List<MonthPlanSaleOrder> getList(Wrapper<MonthPlanSaleOrder> queryWrapper);

    /**
     * 新增月度销售计划订单
     *
     * @param monthPlanSaleOrder 月度销售计划订单
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int insertMonthPlanSaleOrder(MonthPlanSaleOrder monthPlanSaleOrder);

    /**
     * 修改月度销售计划订单
     *
     * @param monthPlanSaleOrder 月度销售计划订单
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateMonthPlanSaleOrder(MonthPlanSaleOrder monthPlanSaleOrder);

    /**
     * 校验月度销售计划订单唯一性
     */
    public String checkMonthPlanSaleOrderUnique(MonthPlanSaleOrder monthPlanSaleOrder);

    /**
     * 根据主键ID，批量删除数据
     *
     * @param ids
     * @return
     */
    int removeByIds(List<Long> ids);

    /**
     * 导入数据处理
     *
     * @param excelDataList excel解析后的数据
     * @param updateSupport 是否需要更新处理 true 更新 false不更新
     * @param importLogId   导入日志ID
     * @return
     */
    AjaxResult importData(List<MonthPlanSaleOrder> excelDataList, boolean updateSupport, Long importLogId);

    /**
     * 转成表对象存储数据
     *
     * @param inSaleOrderDto   查询参数
     * @param inDataListVoList 接口返回的数据
     */
    void handleInSaleOrderSyncResultData(InSaleOrderDto inSaleOrderDto, List<InDataListVo> inDataListVoList);

    /**
     * 外销销售订单同步
     *
     * @param inSaleOrderDto 外销销售订单同步参数
     * @return 结果
     */
    public AjaxResult syncOutSaleOrder(InSaleOrderDto inSaleOrderDto);
}
