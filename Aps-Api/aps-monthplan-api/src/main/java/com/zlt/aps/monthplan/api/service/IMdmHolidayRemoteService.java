package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmHoliday;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmHolidayRemoteService.java
 * 描    述：IMdmHolidayRemoteService0150基础数据_节假日配置前端接口
 *@author zlt
 *@date 2026-01-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmHolidayRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.maindata:/maindata}")
public interface IMdmHolidayRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmHoliday/list")
    TableDataInfo list(@RequestBody MdmHoliday QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmHoliday/save")
    AjaxResult save(@RequestBody MdmHoliday mdmHoliday);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmHoliday/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmHoliday/{id}")
    MdmHoliday getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmHoliday/checkUnique")
    String checkUnique(@RequestBody MdmHoliday mdmHolidayVO);

    /**
     * 导出0150基础数据_节假日配置列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmHoliday/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmHoliday queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入0150基础数据_节假日配置数据
     */
    @ApiOperation("导入0150基础数据_节假日配置")
    @PostMapping("/mdmHoliday/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
