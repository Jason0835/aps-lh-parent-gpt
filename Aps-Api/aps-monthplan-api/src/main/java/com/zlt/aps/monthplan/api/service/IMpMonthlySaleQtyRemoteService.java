package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthlySaleQtyRemoteService.java
 * 描    述：IMpMonthlySaleQtyRemoteService月均销量前端接口
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IMpMonthlySaleQtyRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpMonthlySaleQtyRemoteService {

    /**
     * 查询月均销量列表
     */
    @ApiOperation("查询月均销量列表")
    @PostMapping("/monthlySaleQty/list")
    TableDataInfo list(@RequestBody MpMonthlySaleQty mpMonthlySaleQty);

    /**
    * 新增月均销量
    */
    @ApiOperation("新增月均销量")
    @PostMapping("/monthlySaleQty/add")
    AjaxResult add(@RequestBody MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 修改月均销量
     */
    @ApiOperation("修改月均销量")
    @PostMapping("/monthlySaleQty/edit")
    AjaxResult edit(@RequestBody MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 删除月均销量
     */
    @ApiOperation("删除月均销量")
    @DeleteMapping("/monthlySaleQty/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/monthlySaleQty/{id}")
    MpMonthlySaleQty getInfo(@PathVariable("id") Long id);

    /**
     * 校验月均销量唯一性
     */
    @ApiOperation("校验月均销量唯一性")
    @PostMapping("/monthlySaleQty/checkMpMonthlySaleQtyUnique")
    String checkMpMonthlySaleQtyUnique(@RequestBody MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 导出月均销量列表
    */
    @ApiOperation("导出月均销量列表")
    @PostMapping("/monthlySaleQty/exportData/{fileName}")
    byte[] exportData(@RequestBody MpMonthlySaleQty mpMonthlySaleQty,@PathVariable("fileName") String fileName);

    /**
     * 导入月均销量数据
     */
    @ApiOperation("导入月均销量")
    @PostMapping("/monthlySaleQty/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
