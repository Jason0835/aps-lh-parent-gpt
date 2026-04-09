package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxKeyProduct;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxKeyProductRemoteService.java
 * 描    述：关键产品配置前端接口
 * @author APS Team
 * @date 2026-04-09
 * @version 1.0
 *
 * 修改记录：
 *     修改时间：...
 *     修 改 人：...
 *     修改内容：...
 */
@FeignClient(contextId = "ICxKeyProductRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxKeyProductRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxKeyProduct/list")
    TableDataInfo list(@RequestBody CxKeyProduct queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/cxKeyProduct/save")
    AjaxResult save(@RequestBody CxKeyProduct entity);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxKeyProduct/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxKeyProduct/{id}")
    CxKeyProduct getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxKeyProduct/checkUnique")
    String checkUnique(@RequestBody CxKeyProduct entity);

    /**
     * 导出关键产品配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxKeyProduct/exportData/{fileName}")
    byte[] exportData(@RequestBody CxKeyProduct queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入关键产品配置数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/cxKeyProduct/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
