package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpHistorySaleRecordRemoteService.java
 * 描    述：IMpHistorySaleRecordRemoteService历史销售记录前端接口
 *@author yelq
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@FeignClient(contextId = "IMpHistorySaleRecordRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpHistorySaleRecordRemoteService {

    /**
     * 查询历史销售记录列表
     */
    @ApiOperation("查询历史销售记录列表")
    @PostMapping("/historySaleRecord/list")
    TableDataInfo list(@RequestBody MpHistorySaleRecord mpHistorySaleRecord);

    /**
    * 新增历史销售记录
    */
    @ApiOperation("新增历史销售记录")
    @PostMapping("/historySaleRecord/add")
    AjaxResult add(@RequestBody MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 修改历史销售记录
     */
    @ApiOperation("修改历史销售记录")
    @PostMapping("/historySaleRecord/edit")
    AjaxResult edit(@RequestBody MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 删除历史销售记录
     */
    @ApiOperation("删除历史销售记录")
    @DeleteMapping("/historySaleRecord/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/historySaleRecord/{id}")
    MpHistorySaleRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验历史销售记录唯一性
     */
    @ApiOperation("校验历史销售记录唯一性")
    @PostMapping("/historySaleRecord/checkMpHistorySaleRecordUnique")
    String checkMpHistorySaleRecordUnique(@RequestBody MpHistorySaleRecord mpHistorySaleRecord);

    /**
     * 导出历史销售记录列表
    */
    @ApiOperation("导出历史销售记录列表")
    @PostMapping("/historySaleRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody MpHistorySaleRecord mpHistorySaleRecord,@PathVariable("fileName") String fileName);

    /**
     * 导入历史销售记录数据
     */
    @ApiOperation("导入历史销售记录")
    @PostMapping("/historySaleRecord/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
