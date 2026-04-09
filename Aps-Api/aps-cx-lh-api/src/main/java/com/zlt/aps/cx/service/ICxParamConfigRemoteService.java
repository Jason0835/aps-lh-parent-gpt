package com.zlt.aps.cx.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxParamConfigRemoteService.java
 * 描    述：排程参数配置前端接口
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxParamConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxParamConfigRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxParamConfig/list")
    TableDataInfo list(@RequestBody CxParamConfig queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/cxParamConfig/save")
    AjaxResult save(@RequestBody CxParamConfig entity);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxParamConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxParamConfig/{id}")
    CxParamConfig getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxParamConfig/checkUnique")
    String checkUnique(@RequestBody CxParamConfig entity);

    /**
     * 导出排程参数配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxParamConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody CxParamConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入排程参数配置数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/cxParamConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
