package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITmParamsRemoteService.java
 * 描    述：ITmParamsRemoteService胎面排程参数配置前端接口
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
@FeignClient(contextId = "ITmParamsRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmParamsRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/tmParams/list")
    TableDataInfo list(@RequestBody TmParams queryVO);

    @ApiOperation("保存")
    @PostMapping("/tmParams/save")
    AjaxResult save(TmParams tmParams);

    @ApiOperation("删除")
    @DeleteMapping("/tmParams/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tmParams/{id}")
    TmParams getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/tmParams/checkUnique")
    String checkUnique(@RequestBody TmParams tmParamsVO);

    @ApiOperation("导出列表")
    @PostMapping("/tmParams/exportData/{fileName}")
    byte[] exportData(@RequestBody TmParams queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/tmParams/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
