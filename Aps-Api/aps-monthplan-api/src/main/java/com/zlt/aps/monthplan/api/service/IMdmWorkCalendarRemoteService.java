package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkCalendar;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmWorkCalendarRemoteService.java
 * 描    述：IMdmWorkCalendarRemoteService工作日历前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
@FeignClient(contextId = "IMdmWorkCalendarRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmWorkCalendarRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmWorkCalendar/list")
    TableDataInfo list(@RequestBody MdmWorkCalendar QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmWorkCalendar/save")
    AjaxResult save(@RequestBody MdmWorkCalendar mdmWorkCalendar);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmWorkCalendar/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmWorkCalendar/{id}")
    MdmWorkCalendar getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmWorkCalendar/checkUnique")
    String checkUnique(@RequestBody MdmWorkCalendar mdmWorkCalendarVO);

    /**
     * 导出工作日历列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmWorkCalendar/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmWorkCalendar queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入工作日历数据
     */
    @ApiOperation("导入工作日历")
    @PostMapping("/mdmWorkCalendar/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
