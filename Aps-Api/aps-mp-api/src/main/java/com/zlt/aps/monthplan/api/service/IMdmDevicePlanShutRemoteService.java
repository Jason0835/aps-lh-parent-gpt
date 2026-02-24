package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmDevicePlanShut;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmDevicePlanShutRemoteService.java
 * 描    述：IMdmDevicePlanShutRemoteService0106基础数据_设备计划停机前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-04
 */
@FeignClient(contextId = "IMdmDevicePlanShutRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmDevicePlanShutRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmDevicePlanShut/list")
    TableDataInfo list(@RequestBody MdmDevicePlanShut QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmDevicePlanShut/save")
    AjaxResult save(@RequestBody MdmDevicePlanShut mdmDevicePlanShut);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmDevicePlanShut/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmDevicePlanShut/{id}")
    MdmDevicePlanShut getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmDevicePlanShut/checkUnique")
    String checkUnique(@RequestBody MdmDevicePlanShut mdmDevicePlanShutVO);

    /**
     * 导出0106基础数据_设备计划停机列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmDevicePlanShut/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmDevicePlanShut queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入0106基础数据_设备计划停机数据
     */
    @ApiOperation("导入0106基础数据_设备计划停机")
    @PostMapping("/mdmDevicePlanShut/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
