package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.ProductStockMonth;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductStockMonthRemoteService.java
 * 描    述：IProductStockMonthRemoteService物料月库存信息前端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-03-12
 */
@FeignClient(contextId = "IProductStockMonthRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductStockMonthRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/monthStock/list")
    TableDataInfo list(@RequestBody ProductStockMonth QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/monthStock/save")
    AjaxResult save(@RequestBody ProductStockMonth productStockMonth);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/monthStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/monthStock/{id}")
    ProductStockMonth getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/monthStock/checkUnique")
    String checkUnique(@RequestBody ProductStockMonth productStockMonthVO);

    /**
     * 导出物料月库存信息列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/monthStock/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductStockMonth queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入物料月库存信息数据
     */
    @ApiOperation("导入物料月库存信息")
    @PostMapping("/monthStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
