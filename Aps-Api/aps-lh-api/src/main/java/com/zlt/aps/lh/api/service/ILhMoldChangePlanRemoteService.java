package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleImportFileDTO;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMoldChangePlanRemoteService.java
 * 描    述：ILhMoldChangePlanRemoteService模具变动单前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */
@FeignClient(contextId = "ILhMoldChangePlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMoldChangePlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhMoldChangePlan/list")
    TableDataInfo list(@RequestBody LhMoldChangePlan QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhMoldChangePlan/save")
    AjaxResult save(@RequestBody LhMoldChangePlan lhMoldChangePlan);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhMoldChangePlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhMoldChangePlan/{id}")
    LhMoldChangePlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhMoldChangePlan/checkUnique")
    String checkUnique(@RequestBody LhMoldChangePlan lhMoldChangePlanVO);

    /**
     * 导出模具变动单列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhMoldChangePlan/exportData/{fileName}")
    byte[] exportData(@RequestBody LhMoldChangePlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具变动单数据
     */
    @ApiOperation("导入模具变动单")
    @PostMapping("/lhMoldChangePlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("生成换模计划")
    @PostMapping("/lhMoldChangePlan/generateMoldReplacementPlan")
    AjaxResult generateMoldReplacementPlan(@RequestBody LhMoldChangePlan queryVO);

    /**
     * 导入换模计划
     */
    @PostMapping("/lhMoldChangePlan/importMoldChangePlan")
    @ApiOperation("导入硫化换模计划")
    AjaxResult importMoldChangePlan(@RequestBody LhScheduleImportFileDTO lhScheduleImportFileDTO);


}
