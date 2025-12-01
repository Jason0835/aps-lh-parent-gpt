package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxPersionTrainSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxPersionTrainSettingRemoteService.java
 * 描    述：ICxPersionTrainSettingRemoteService成型工序开机档数前端接口
 *@author zlt
 *@date 2025-03-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICxPersionTrainSettingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxPersionTrainSettingRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxPersionTrainSetting/list")
    TableDataInfo list(@RequestBody CxPersionTrainSetting QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cxPersionTrainSetting/save")
    AjaxResult save(@RequestBody CxPersionTrainSetting cxPersionTrainSetting);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxPersionTrainSetting/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxPersionTrainSetting/{id}")
    CxPersionTrainSetting getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxPersionTrainSetting/checkUnique")
    String checkUnique(@RequestBody CxPersionTrainSetting cxPersionTrainSettingVO);

    /**
     * 导出成型工序开机档数列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cxPersionTrainSetting/exportData/{fileName}")
    byte[] exportData(@RequestBody CxPersionTrainSetting queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型工序开机档数数据
     */
    @ApiOperation("导入成型工序开机档数")
    @PostMapping("/cxPersionTrainSetting/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 列表校验唯一并保存
     */
    @ApiOperation("列表校验唯一并保存")
    @PostMapping("/cxPersionTrainSetting/saveList")
    AjaxResult saveList(@RequestBody List<CxPersionTrainSetting> list);
}
