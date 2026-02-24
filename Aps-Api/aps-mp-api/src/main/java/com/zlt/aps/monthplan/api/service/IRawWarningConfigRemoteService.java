package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningConfig;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawWarningConfigRemoteService.java
 * 描    述：IRawWarningConfigRemoteService原材料预警配置前端接口
 *@author zlt
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawWarningConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawWarningConfigRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawWarningConfig/list")
    TableDataInfo list(@RequestBody RawWarningConfig QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawWarningConfig/save")
    AjaxResult save(@RequestBody RawWarningConfig rawWarningConfig);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawWarningConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawWarningConfig/{id}")
    RawWarningConfig getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawWarningConfig/checkUnique")
    String checkUnique(@RequestBody RawWarningConfig rawWarningConfigVO);

    /**
     * 导出原材料预警配置列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawWarningConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody RawWarningConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入原材料预警配置数据
     */
    @ApiOperation("导入原材料预警配置")
    @PostMapping("/rawWarningConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
