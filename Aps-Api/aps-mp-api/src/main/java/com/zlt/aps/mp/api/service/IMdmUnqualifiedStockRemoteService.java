package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmUnqualifiedStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmUnqualifiedStockRemoteService.java
 * 描    述：IMdmUnqualifiedStockRemoteService不合格品库存前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-22
 */
@FeignClient(contextId = "IMdmUnqualifiedStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmUnqualifiedStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmUnqualifiedStock/list")
    TableDataInfo list(@RequestBody MdmUnqualifiedStock QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmUnqualifiedStock/save")
    AjaxResult save(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmUnqualifiedStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmUnqualifiedStock/{id}")
    MdmUnqualifiedStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmUnqualifiedStock/checkUnique")
    String checkUnique(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStockVO);

    /**
     * 导出不合格品库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmUnqualifiedStock/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmUnqualifiedStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入不合格品库存数据
     */
    @ApiOperation("导入不合格品库存")
    @PostMapping("/mdmUnqualifiedStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
