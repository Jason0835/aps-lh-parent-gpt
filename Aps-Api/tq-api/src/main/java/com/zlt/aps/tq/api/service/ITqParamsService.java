package com.zlt.aps.tq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITqParamsService.java
 * 描    述：ITqParamsService胎圈排程参数配置前端接口（对齐胎面 ITmParamsRemoteService）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
@FeignClient(contextId = "ITqParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqParamsService {

    @ApiOperation("查询列表")
    @PostMapping("/tqParams/list")
    TableDataInfo list(@RequestBody TqParams queryVO);

    @ApiOperation("保存")
    @PostMapping("/tqParams/save")
    AjaxResult save(TqParams tqParams);

    @ApiOperation("删除")
    @DeleteMapping("/tqParams/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tqParams/{id}")
    TqParams getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tqParams/checkUnique")
    String checkUnique(@RequestBody TqParams tqParamsVO);

    @ApiOperation("导出列表")
    @PostMapping("/tqParams/exportData/{fileName}")
    byte[] exportData(@RequestBody TqParams queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tqParams/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}