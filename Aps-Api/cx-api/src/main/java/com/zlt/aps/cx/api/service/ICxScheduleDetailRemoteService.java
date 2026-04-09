package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxScheduleDetail;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxScheduleDetailRemoteService.java
 * 描    述：成型排程明细前端接口
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxScheduleDetailRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxScheduleDetailRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxScheduleDetail/list")
    TableDataInfo list(@RequestBody CxScheduleDetail queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/cxScheduleDetail/save")
    AjaxResult save(@RequestBody CxScheduleDetail entity);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxScheduleDetail/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxScheduleDetail/{id}")
    CxScheduleDetail getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxScheduleDetail/checkUnique")
    String checkUnique(@RequestBody CxScheduleDetail entity);

    /**
     * 导出成型排程明细列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxScheduleDetail/exportData/{fileName}")
    byte[] exportData(@RequestBody CxScheduleDetail queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成型排程明细数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/cxScheduleDetail/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
