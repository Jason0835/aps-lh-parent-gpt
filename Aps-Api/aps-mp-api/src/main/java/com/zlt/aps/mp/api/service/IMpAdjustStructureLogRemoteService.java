package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureLog;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustStructureLogRemoteService.java
 * 描    述：IMpAdjustStructureLogRemoteService调整-操作日志前端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpAdjustStructureLogRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpAdjustStructureLogRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpAdjustStructureLog/list")
    TableDataInfo list(@RequestBody MpAdjustStructureLog QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mpAdjustStructureLog/save")
    AjaxResult save(@RequestBody MpAdjustStructureLog mpAdjustStructureLog);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpAdjustStructureLog/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpAdjustStructureLog/{id}")
    MpAdjustStructureLog getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpAdjustStructureLog/checkUnique")
    String checkUnique(@RequestBody MpAdjustStructureLog mpAdjustStructureLogVO);

    /**
     * 导出调整-操作日志列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mpAdjustStructureLog/exportData/{fileName}")
    byte[] exportData(@RequestBody MpAdjustStructureLog queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入调整-操作日志数据
     */
    @ApiOperation("导入调整-操作日志")
    @PostMapping("/mpAdjustStructureLog/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
