package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.ProductVulcanizingLimit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductVulcanizingLimitRemoteService.java
 * 描    述：IProductVulcanizingLimitRemoteService基础数据-品种限制硫化机前端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@FeignClient(contextId = "IProductVulcanizingLimitRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductVulcanizingLimitRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productVulcanizingLimit/list")
    TableDataInfo list(@RequestBody ProductVulcanizingLimit QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/productVulcanizingLimit/save")
    AjaxResult save(@RequestBody ProductVulcanizingLimit productVulcanizingLimit);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productVulcanizingLimit/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productVulcanizingLimit/{id}")
    ProductVulcanizingLimit getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productVulcanizingLimit/checkUnique")
    String checkUnique(@RequestBody ProductVulcanizingLimit productVulcanizingLimitVO);

    /**
     * 导出基础数据-品种限制硫化机列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/productVulcanizingLimit/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductVulcanizingLimit queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据-品种限制硫化机数据
     */
    @ApiOperation("导入基础数据-品种限制硫化机")
    @PostMapping("/productVulcanizingLimit/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
