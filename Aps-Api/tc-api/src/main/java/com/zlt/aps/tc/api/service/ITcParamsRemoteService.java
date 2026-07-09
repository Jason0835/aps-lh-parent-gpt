package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITcParamsRemoteService.java
 * 描    述：ITcParamsRemoteService胎侧排程参数配置前端接口
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
@FeignClient(contextId = "ITcParamsRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcParamsRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tcParams/list")
    TableDataInfo list(@RequestBody TcParams queryVO);

    @ApiOperation("保存")
    @PostMapping("/tcParams/save")
    AjaxResult save(TcParams tcParams);

    @ApiOperation("删除")
    @DeleteMapping("/tcParams/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tcParams/{id}")
    TcParams getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tcParams/checkUnique")
    String checkUnique(@RequestBody TcParams tcParamsVO);

    @ApiOperation("导出列表")
    @PostMapping("/tcParams/exportData/{fileName}")
    byte[] exportData(@RequestBody TcParams queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tcParams/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("根据参数编码查询")
    @GetMapping("/tcParams/selectOneByParamCode")
    TcParams selectOneByParamCode(@RequestParam("paramCode") String paramCode, @RequestParam("factoryCode") String factoryCode);

    @ApiOperation("查询参数Map")
    @GetMapping("/tcParams/listTcParams")
    Map<String, String> listTcParams(@RequestParam("factoryCode") String factoryCode);
}
