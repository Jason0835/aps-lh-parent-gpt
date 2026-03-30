package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonthSurplus;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMonthSurplusRemoteService.java
 * 描    述：IMdmMonthSurplusRemoteService0140基础数据_月底计划余量前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@FeignClient(contextId = "IMdmMonthSurplusRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMonthSurplusRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMonthSurplus/list")
    TableDataInfo list(@RequestBody MdmMonthSurplus QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmMonthSurplus/save")
    AjaxResult save(@RequestBody MdmMonthSurplus mdmMonthSurplus);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMonthSurplus/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMonthSurplus/{id}")
    MdmMonthSurplus getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMonthSurplus/checkUnique")
    String checkUnique(@RequestBody MdmMonthSurplus mdmMonthSurplusVO);

    /**
     * 导出0140基础数据_月底计划余量列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMonthSurplus/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMonthSurplus queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入0140基础数据_月底计划余量数据
     */
    @ApiOperation("导入0140基础数据_月底计划余量")
    @PostMapping("/mdmMonthSurplus/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 根据工厂、年、月查询需求计划版本列表（去重）
     *
     * @param monthSurplus 年月工厂
     * @return 需求计划版本列表
     */
    @ApiOperation("根据工厂、年、月查询需求计划版本列表（去重）")
    @PostMapping("/mdmMonthSurplus/listRequireVersions")
    public AjaxResult listRequireVersions(@RequestBody MdmMonthSurplus monthSurplus);
}
