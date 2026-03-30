package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhParamsRemoteService.java
 * 描    述：ILhParamsRemoteService硫化参数信息前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@FeignClient(contextId = "ILhParamsRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhParamsRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhParams/list")
    TableDataInfo list(@RequestBody LhParams QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhParams/save")
    AjaxResult save(@RequestBody LhParams lhParams);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhParams/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhParams/{id}")
    LhParams getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhParams/checkUnique")
    String checkUnique(@RequestBody LhParams lhParamsVO);

    /**
     * 导出硫化参数信息列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhParams/exportData/{fileName}")
    byte[] exportData(@RequestBody LhParams queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入硫化参数信息数据
     */
    @ApiOperation("导入硫化参数信息")
    @PostMapping("/lhParams/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
