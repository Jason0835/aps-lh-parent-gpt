package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanNoProductionRecordRemoteService.java
 * 描    述：IMonthPlanNoProductionRecordRemoteService不排产记录前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@FeignClient(contextId = "IMonthPlanNoProductionRecordRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMonthPlanNoProductionRecordRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/monthPlanNoProductionRecord/list")
    TableDataInfo list(@RequestBody MonthPlanNoProductionRecord QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/monthPlanNoProductionRecord/save")
    AjaxResult save(@RequestBody MonthPlanNoProductionRecord monthPlanNoProductionRecord);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/monthPlanNoProductionRecord/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/monthPlanNoProductionRecord/{id}")
    MonthPlanNoProductionRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/monthPlanNoProductionRecord/checkUnique")
    String checkUnique(@RequestBody MonthPlanNoProductionRecord monthPlanNoProductionRecordVO);

    /**
     * 导出不排产记录列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/monthPlanNoProductionRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody MonthPlanNoProductionRecord queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入不排产记录数据
     */
    @ApiOperation("导入不排产记录")
    @PostMapping("/monthPlanNoProductionRecord/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
