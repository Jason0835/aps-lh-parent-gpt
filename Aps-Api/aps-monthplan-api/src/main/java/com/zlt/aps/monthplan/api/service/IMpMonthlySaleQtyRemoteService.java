package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;


import java.util.List;

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
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@FeignClient(contextId = "IMpMonthlySaleQtyRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpMonthlySaleQtyRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/mpMonthlySaleQty/list")
    TableDataInfo list(@RequestBody MpMonthlySaleQty QueryVO);

    @ApiOperation("保存")
    @PostMapping("/mpMonthlySaleQty/save")
    AjaxResult save(@RequestBody MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 修改月均销量
     * 删除
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
    @ApiOperation("删除")
    @DeleteMapping("/mpMonthlySaleQty/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/monthlySaleQty/{id}")
    MpMonthlySaleQty getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/mpMonthlySaleQty/checkUnique")
    String checkUnique(@RequestBody MpMonthlySaleQty mpMonthlySaleQtyVO);

    @ApiOperation("导出列表")
    @PostMapping("/mpMonthlySaleQty/exportData/{fileName}")
    byte[] exportData(@RequestBody MpMonthlySaleQty queryVO, @PathVariable("fileName") String fileName);
    /**
     * 导入月均销量数据
     */
    @ApiOperation("导入月均销量")
    @PostMapping("/monthlySaleQty/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}

