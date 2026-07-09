package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITcMachineInfoRemoteService.java
 * 描    述：ITcMachineInfoRemoteService胎侧机台基础表前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@FeignClient(contextId = "ITcMachineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcMachineInfoRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcMachineInfo/list")
    TableDataInfo list(@RequestBody TcMachineInfo queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcMachineInfo/save")
    AjaxResult save(TcMachineInfo tcMachineInfo);

    @ApiOperation("删除")
    @DeleteMapping("/tcMachineInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcMachineInfo/{id}")
    TcMachineInfo getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcMachineInfo/checkUnique")
    String checkUnique(@RequestBody TcMachineInfo tcMachineInfoVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcMachineInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody TcMachineInfo queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcMachineInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
