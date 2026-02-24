package com.zlt.aps.monthplan.factory.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleQty;
import com.zlt.aps.monthplan.api.domain.itf.InDataListVo;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;
import com.zlt.aps.monthplan.api.domain.vo.CalcStockingResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcel4MonthVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcelVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpHistorySaleQtyService.java
 * 描    述：IMpHistorySaleQtyService历史销售记录后端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-13
 */
public interface IMpHistorySaleQtyService extends IService<MpHistorySaleQty> {

    /**
     * 查询历史销售记录列表
     *
     * @param mpHistorySaleQty 历史销售记录
     * @return 历史销售记录集合
     */
    List<MpHistorySaleQty> selectMpHistorySaleQtyList(MpHistorySaleQty mpHistorySaleQty);


    /**
     * 查询计算备货数据
     *
     * @param queryCalcStockingParamVo 计算备货查询参数
     * @return
     */
    List<CalcStockingResultVo> selectCalcStocking(QueryCalcStockingParamVo queryCalcStockingParamVo, Integer lastMonth);

    /**
     * 导入历史销售记录
     */
    @Transactional
    AjaxResult importData(List<MpHistorySaleQtyExcelVo> list, boolean updateSupport, Long importLogId);

    /**
     * 导入历史销售记录
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @param importLog
     * @param beginTime
     * @param attributes
     */
    @Transactional
    void importDataAsync(List<MpHistorySaleQtyExcelVo> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes);

    /**
     * 处理内销销售订单同步结果数据
     *
     * @param inSaleOrderDto   查询参数
     * @param inDataListVoList 内销历史销售订单同步结果数据
     */
    void handleInHisSaleOrderSyncResultData(InSaleOrderDto inSaleOrderDto, List<InDataListVo> inDataListVoList);

    /**
     * 导入历史销售计划-月
     * @param list 要导入的数据
     * @param updateSupport 是否更新
     * @param id   导入日志id
     * @param importLog 导入日志
     * @param beginTime 开始时间
     * @param attributes 请求属性
     */
    @Transactional
    void importMonthDataAsync(List<MpHistorySaleQtyExcel4MonthVo> list, boolean updateSupport, Long id, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes);

    /**
     * 导入历史销售计划-月
     * @param list 要导入的数据
     * @param updateSupport 是否更新
     * @param id   导入日志id
     * @return 结果
     */
    @Transactional
    AjaxResult importMonthData(List<MpHistorySaleQtyExcel4MonthVo> list, boolean updateSupport, Long id);

    /**
     * 查询导出列表-年
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpHistorySaleQtyExcelVo> selectMpHistorySaleQtyList4ExportData(MpHistorySaleQty queryVO);
}
