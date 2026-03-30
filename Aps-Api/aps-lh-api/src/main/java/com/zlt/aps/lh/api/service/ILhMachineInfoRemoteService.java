package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMachineInfoRemoteService.java
 * 描    述：ILhMachineInfoRemoteService硫化机台信息前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@FeignClient(contextId = "ILhMachineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMachineInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhMachineInfo/list")
    TableDataInfo list(@RequestBody LhMachineInfo QueryVO);


    @ApiOperation("查询列表")
    @PostMapping("/lhMachineInfo/getList")
    List<LhMachineInfo> getList(@RequestBody LhMachineInfo query);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhMachineInfo/save")
    AjaxResult save(@RequestBody LhMachineInfo lhMachineInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhMachineInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhMachineInfo/{id}")
    LhMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhMachineInfo/checkUnique")
    String checkUnique(@RequestBody LhMachineInfo lhMachineInfoVO);

    /**
     * 导出硫化机台信息列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhMachineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMachineInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化机台信息数据
     */
    @ApiOperation("导入硫化机台信息")
    @PostMapping("/lhMachineInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
