package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.ProductMoldingLimit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductMoldingLimitRemoteService.java
 * 描    述：IProductMoldingLimitRemoteService基础数据-品种限制成型机前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-20
 */
@FeignClient(contextId = "IProductMoldingLimitRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductMoldingLimitRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productMoldingLimit/list")
    TableDataInfo list(@RequestBody ProductMoldingLimit QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/productMoldingLimit/save")
    AjaxResult save(@RequestBody ProductMoldingLimit productMoldingLimit);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productMoldingLimit/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productMoldingLimit/{id}")
    ProductMoldingLimit getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productMoldingLimit/checkUnique")
    String checkUnique(@RequestBody ProductMoldingLimit productMoldingLimitVO);

    /**
     * 导出基础数据-品种限制成型机列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/productMoldingLimit/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductMoldingLimit queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据-品种限制成型机数据
     */
    @ApiOperation("导入基础数据-品种限制成型机")
    @PostMapping("/productMoldingLimit/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
