package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmStructureLhRatio;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmStructureLhRatioRemoteService.java
 * 描    述：IMdmStructureLhRatioRemoteService成型结构硫化配比前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmStructureLhRatioRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmStructureLhRatioRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmStructureLhRatio/list")
    TableDataInfo list(@RequestBody MdmStructureLhRatio QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmStructureLhRatio/save")
    AjaxResult save(@RequestBody MdmStructureLhRatio mdmStructureLhRatio);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmStructureLhRatio/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmStructureLhRatio/{id}")
    MdmStructureLhRatio getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmStructureLhRatio/checkUnique")
    String checkUnique(@RequestBody MdmStructureLhRatio mdmStructureLhRatioVO);

    /**
     * 导出成型结构硫化配比列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmStructureLhRatio/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmStructureLhRatio queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型结构硫化配比数据
     */
    @ApiOperation("导入成型结构硫化配比")
    @PostMapping("/mdmStructureLhRatio/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
