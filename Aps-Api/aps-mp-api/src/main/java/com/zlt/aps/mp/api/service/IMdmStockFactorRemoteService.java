package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmStockFactor;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmStockFactorRemoteService.java
 * 描    述：IMdmStockFactorRemoteService备货系数配置前端接口
 *@author zlt
 *@date 2025-02-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmStockFactorRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmStockFactorRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmStockFactor/list")
    TableDataInfo list(@RequestBody MdmStockFactor QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmStockFactor/save")
    AjaxResult save(@RequestBody MdmStockFactor mdmStockFactor);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmStockFactor/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmStockFactor/{id}")
    MdmStockFactor getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmStockFactor/checkUnique")
    String checkUnique(@RequestBody MdmStockFactor mdmStockFactorVO);

    /**
     * 导出备货系数配置列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmStockFactor/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmStockFactor queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入备货系数配置数据
     */
    @ApiOperation("导入备货系数配置")
    @PostMapping("/mdmStockFactor/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
