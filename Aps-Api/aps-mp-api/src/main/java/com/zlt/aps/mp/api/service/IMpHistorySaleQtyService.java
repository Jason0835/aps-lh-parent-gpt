package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MpHistorySaleQty;
import com.zlt.aps.mp.api.domain.vo.MpHistorySaleQtyExcel4MonthVo;
import com.zlt.aps.mp.api.domain.vo.MpHistorySaleQtyExcelVo;
import com.zlt.aps.mp.api.domain.vo.QueryCalcStockingParamVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpHistorySaleQtyService.java
 * 描    述：IMpHistorySaleQtyService历史销售记录前端接口
 *@author hsc
 *@date 2025-02-13
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */
@FeignClient(contextId = "IMpHistorySaleQtyService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpHistorySaleQtyService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpHistorySaleQty/list")
    TableDataInfo list(@RequestBody MpHistorySaleQty QueryVO);

    /**
     * 查询计算备货数据
     */
    @ApiOperation("查询计算备货数据")
    @PostMapping("/mpHistorySaleQty/queryCalcStocking")
    TableDataInfo queryCalcStocking(@RequestBody QueryCalcStockingParamVo queryCalcStockingParamVo);

    /**
     * 导入历史销售记录数据
     */
    @ApiOperation("导入历史销售记录")
    @PostMapping("/mpHistorySaleQty/importData")
    public AjaxResult importData(@RequestBody List<MpHistorySaleQtyExcelVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 导入历史销售记录数据-月份
     */
    @ApiOperation("导入历史销售记录-月份")
    @PostMapping("/mpHistorySaleQty/importMonthData")
    public AjaxResult importMonthData(@RequestBody List<MpHistorySaleQtyExcel4MonthVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 导出列表
     * @param queryVO 查询参数
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出列表")
    @PostMapping("/mpHistorySaleQty/exportData/{fileName}")
    byte[] exportData(@RequestBody MpHistorySaleQty queryVO, @PathVariable("fileName") String fileName);
}
