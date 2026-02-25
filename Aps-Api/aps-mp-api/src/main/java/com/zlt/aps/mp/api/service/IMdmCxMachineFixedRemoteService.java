package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineFixed;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmCxMachineFixedRemoteService.java
 * 描    述：IMdmCxMachineFixedRemoteService成型固定机台前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@FeignClient(contextId = "IMdmCxMachineFixedRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmCxMachineFixedRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmCxMachineFixed/list")
    TableDataInfo list(@RequestBody MdmCxMachineFixed QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmCxMachineFixed/save")
    AjaxResult save(@RequestBody MdmCxMachineFixed mdmCxMachineFixed);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmCxMachineFixed/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmCxMachineFixed/{id}")
    MdmCxMachineFixed getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmCxMachineFixed/checkUnique")
    String checkUnique(@RequestBody MdmCxMachineFixed mdmCxMachineFixedVO);

    /**
     * 导出成型固定机台列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmCxMachineFixed/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmCxMachineFixed queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型固定机台数据
     */
    @ApiOperation("导入成型固定机台")
    @PostMapping("/mdmCxMachineFixed/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
