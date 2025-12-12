package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkWearInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmWorkWearInfoRemoteService.java
 * 描    述：IMdmWorkWearInfoRemoteService成型鼓(工装)台账前端接口
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
@FeignClient(contextId = "IMdmWorkWearInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmWorkWearInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmWorkWearInfo/list")
    TableDataInfo list(@RequestBody MdmWorkWearInfo QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmWorkWearInfo/save")
    AjaxResult save(@RequestBody MdmWorkWearInfo mdmWorkWearInfo);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmWorkWearInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmWorkWearInfo/{id}")
    MdmWorkWearInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmWorkWearInfo/checkUnique")
    String checkUnique(@RequestBody MdmWorkWearInfo mdmWorkWearInfoVO);

    /**
     * 导出成型鼓(工装)台账列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmWorkWearInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmWorkWearInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型鼓(工装)台账数据
     */
    @ApiOperation("导入成型鼓(工装)台账")
    @PostMapping("/mdmWorkWearInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
