package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmProductionCalendar;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductionCalendarRemoteService.java
 * 描    述：IMdmProductionCalendarRemoteService生产日历前端接口
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmProductionCalendarRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmProductionCalendarRemoteService {

    /**
     * 查询生产日历列表
     */
    @ApiOperation("查询生产日历列表")
    @PostMapping("/productionCalendar/list")
    TableDataInfo list(@RequestBody MdmProductionCalendar mdmProductionCalendar);

    /**
    * 新增生产日历
    */
    @ApiOperation("新增生产日历")
    @PostMapping("/productionCalendar/add")
    AjaxResult add(@RequestBody MdmProductionCalendar mdmProductionCalendar);

    /**
     * 修改生产日历
     */
    @ApiOperation("修改生产日历")
    @PostMapping("/productionCalendar/edit")
    AjaxResult edit(@RequestBody MdmProductionCalendar mdmProductionCalendar);

    /**
     * 删除生产日历
     */
    @ApiOperation("删除生产日历")
    @DeleteMapping("/productionCalendar/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productionCalendar/{id}")
    MdmProductionCalendar getInfo(@PathVariable("id") Long id);

    /**
     * 校验生产日历唯一性
     */
    @ApiOperation("校验生产日历唯一性")
    @PostMapping("/productionCalendar/checkMdmProductionCalendarUnique")
    String checkMdmProductionCalendarUnique(@RequestBody MdmProductionCalendar mdmProductionCalendar);

    /**
     * 导出生产日历列表
    */
    @ApiOperation("导出生产日历列表")
    @PostMapping("/productionCalendar/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmProductionCalendar mdmProductionCalendar,@PathVariable("fileName") String fileName);

    /**
     * 导入生产日历数据
     */
    @ApiOperation("导入生产日历")
    @PostMapping("/productionCalendar/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
