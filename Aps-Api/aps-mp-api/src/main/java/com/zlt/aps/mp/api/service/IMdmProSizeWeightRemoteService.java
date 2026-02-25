package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmProSizeWeight;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProSizeWeightRemoteService.java
 * 描    述：IMdmProSizeWeightRemoteService基础数据库位寸口重量前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-04-08
 */
@FeignClient(contextId = "IMdmProSizeWeightRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmProSizeWeightRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmProSizeWeight/list")
    TableDataInfo list(@RequestBody MdmProSizeWeight QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmProSizeWeight/save")
    AjaxResult save(@RequestBody MdmProSizeWeight mdmProSizeWeight);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmProSizeWeight/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmProSizeWeight/{id}")
    MdmProSizeWeight getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmProSizeWeight/checkUnique")
    String checkUnique(@RequestBody MdmProSizeWeight mdmProSizeWeightVO);

    /**
     * 导出基础数据库位寸口重量列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmProSizeWeight/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmProSizeWeight queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据库位寸口重量数据
     */
    @ApiOperation("导入基础数据库位寸口重量")
    @PostMapping("/mdmProSizeWeight/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
