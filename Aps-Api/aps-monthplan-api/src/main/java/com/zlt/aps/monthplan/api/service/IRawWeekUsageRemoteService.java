package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.RawWeekUsage;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawWeekUsageRemoteService.java
 * 描    述：IRawWeekUsageRemoteService周维度原材料用量记录前端接口
 *@author zlt
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawWeekUsageRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawWeekUsageRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawWeekUsage/list")
    TableDataInfo list(@RequestBody RawWeekUsage QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawWeekUsage/save")
    AjaxResult save(@RequestBody RawWeekUsage rawWeekUsage);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawWeekUsage/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawWeekUsage/{id}")
    RawWeekUsage getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawWeekUsage/checkUnique")
    String checkUnique(@RequestBody RawWeekUsage rawWeekUsageVO);

    /**
     * 导出周维度原材料用量记录列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawWeekUsage/exportData/{fileName}")
    byte[] exportData(@RequestBody RawWeekUsage queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入周维度原材料用量记录数据
     */
    @ApiOperation("导入周维度原材料用量记录")
    @PostMapping("/rawWeekUsage/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);


    @PostMapping("/rawWeekUsage/generate-by-month")
    @ApiOperation("按照月份生成周维度原材料用量记录")
    public AjaxResult generateByMonth(@RequestParam String factoryCode,
                                      @RequestParam Integer year,
                                      @RequestParam Integer month);

    @PostMapping("/rawWeekUsage/generate-by-week")
    @ApiOperation("按照周维度份生成周维度原材料用量记录")
    public AjaxResult generateByWeek(@RequestParam String factoryCode,
                                     @RequestParam Integer year,
                                     @RequestParam Integer month,
                                     @RequestParam Integer week);

    @GetMapping("/rawWeekUsage/statistics")
    @ApiOperation("获取周用量统计数据")
    public AjaxResult getStatistics(@RequestParam String factoryCode,
                                    @RequestParam Integer year,
                                    @RequestParam(required = false) Integer month,
                                    @RequestParam(required = false) Integer week);
}
