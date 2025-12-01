package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxParamsRemoteService.java
 * 描    述：ICxParamsRemoteService成型工序参数信息前端接口
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICxParamsRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxParamsRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxParams/list")
    TableDataInfo list(@RequestBody CxParams QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cxParams/save")
    AjaxResult save(@RequestBody CxParams cxParams);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxParams/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxParams/{id}")
    CxParams getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxParams/checkUnique")
    String checkUnique(@RequestBody CxParams cxParamsVO);

    /**
     * 导出成型工序参数信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cxParams/exportData/{fileName}")
    byte[] exportData(@RequestBody CxParams queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型工序参数信息数据
     */
    @ApiOperation("导入成型工序参数信息")
    @PostMapping("/cxParams/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
