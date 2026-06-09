package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITmMachineInfoRemoteService.java
 * 描    述：ITmMachineInfoRemoteService胎面机台基础表前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@FeignClient(contextId = "ITmMachineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmMachineInfoRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmMachineInfo/list")
    TableDataInfo list(@RequestBody TmMachineInfo queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmMachineInfo/save")
    AjaxResult save(TmMachineInfo tmMachineInfo);

    @ApiOperation("删除")
    @DeleteMapping("/tmMachineInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmMachineInfo/{id}")
    TmMachineInfo getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmMachineInfo/checkUnique")
    String checkUnique(@RequestBody TmMachineInfo tmMachineInfoVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmMachineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody TmMachineInfo queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmMachineInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
