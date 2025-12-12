package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpHistorySaleRecordRemoteService.java
 * 描    述：IMpHistorySaleRecordRemoteService历史销售记录前端接口
 *@author zlt
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMpHistorySaleRecordRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpHistorySaleRecordRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/MpHistorySaleRecord/list")
    TableDataInfo list(@RequestBody MpHistorySaleRecord QueryVO);

    /**
     * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/MpHistorySaleRecord/save")
    AjaxResult save(@RequestBody MpHistorySaleRecord mpHistorySaleRecord);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/MpHistorySaleRecord/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/MpHistorySaleRecord/{id}")
    MpHistorySaleRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/MpHistorySaleRecord/checkUnique")
    String checkUnique(@RequestBody MpHistorySaleRecord mpHistorySaleRecordVO);

    /**
     * 导出历史销售记录列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/MpHistorySaleRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody MpHistorySaleRecord queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入历史销售记录数据
     */
    @ApiOperation("导入历史销售记录")
    @PostMapping("/MpHistorySaleRecord/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
