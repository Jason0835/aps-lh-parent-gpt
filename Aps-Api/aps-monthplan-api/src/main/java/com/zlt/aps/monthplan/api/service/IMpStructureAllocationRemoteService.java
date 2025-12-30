package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpStructureAllocationRemoteService.java
 * 描    述：IMpStructureAllocationRemoteService排产过程_结构排产前端接口
 *@author zlt
 *@date 2025-12-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpStructureAllocationRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpStructureAllocationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpStructureAllocation/list")
    TableDataInfo list(@RequestBody MpStructureAllocation QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpStructureAllocation/save")
    AjaxResult save(@RequestBody MpStructureAllocation mpStructureAllocation);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpStructureAllocation/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpStructureAllocation/{id}")
    MpStructureAllocation getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpStructureAllocation/checkUnique")
    String checkUnique(@RequestBody MpStructureAllocation mpStructureAllocationVO);

    /**
     * 导出排产过程_结构排产列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpStructureAllocation/exportData/{fileName}")
    byte[] exportData(@RequestBody MpStructureAllocation queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入排产过程_结构排产数据
     */
    @ApiOperation("导入排产过程_结构排产")
    @PostMapping("/mpStructureAllocation/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
