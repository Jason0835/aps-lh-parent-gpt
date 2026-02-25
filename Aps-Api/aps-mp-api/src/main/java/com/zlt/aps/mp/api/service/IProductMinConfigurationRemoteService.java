package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.ProductMinConfiguration;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductMinConfigurationRemoteService.java
 * 描    述：IProductMinConfigurationRemoteService最小批量前端接口
 *@author ZLT
 *@date 2025-02-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */
@FeignClient(contextId = "IProductMinConfigurationRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductMinConfigurationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productMinConfiguration/list")
    TableDataInfo list(@RequestBody ProductMinConfiguration QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/productMinConfiguration/save")
    AjaxResult save(@RequestBody ProductMinConfiguration productMinConfiguration);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productMinConfiguration/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productMinConfiguration/{id}")
    ProductMinConfiguration getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productMinConfiguration/checkUnique")
    String checkUnique(@RequestBody ProductMinConfiguration productMinConfigurationVO);

    /**
     * 导出最小批量列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/productMinConfiguration/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductMinConfiguration queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入最小批量数据
     */
    @ApiOperation("导入最小批量")
    @PostMapping("/productMinConfiguration/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
