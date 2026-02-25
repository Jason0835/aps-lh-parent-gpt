package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmInterestRate;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmInterestRateRemoteService.java
 * 描    述：IMdmInterestRateRemoteService利率优先等级配置前端接口
 *@author zlt
 *@date 2025-03-03
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmInterestRateRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmInterestRateRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmInterestRate/list")
    TableDataInfo list(@RequestBody MdmInterestRate QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmInterestRate/save")
    AjaxResult save(@RequestBody MdmInterestRate mdmInterestRate);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmInterestRate/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmInterestRate/{id}")
    MdmInterestRate getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmInterestRate/checkUnique")
    String checkUnique(@RequestBody MdmInterestRate mdmInterestRateVO);

    /**
     * 导出利率优先等级配置列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmInterestRate/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmInterestRate queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入利率优先等级配置数据
     */
    @ApiOperation("导入利率优先等级配置")
    @PostMapping("/mdmInterestRate/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
