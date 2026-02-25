package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.VulcanizingMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IVulcanizingMachineRemoteService.java
 * 描    述：IVulcanizingMachineRemoteService基础数据-硫化机档案前端接口
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
@FeignClient(contextId = "IVulcanizingMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IVulcanizingMachineRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/vulcanizingMachine/list")
    TableDataInfo list(@RequestBody VulcanizingMachine QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/vulcanizingMachine/save")
    AjaxResult save(@RequestBody VulcanizingMachine vulcanizingMachine);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/vulcanizingMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/vulcanizingMachine/{id}")
    VulcanizingMachine getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/vulcanizingMachine/checkUnique")
    String checkUnique(@RequestBody VulcanizingMachine vulcanizingMachineVO);

    /**
     * 导出基础数据-硫化机档案列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/vulcanizingMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody VulcanizingMachine queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据-硫化机档案数据
     */
    @ApiOperation("导入基础数据-硫化机档案")
    @PostMapping("/vulcanizingMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
