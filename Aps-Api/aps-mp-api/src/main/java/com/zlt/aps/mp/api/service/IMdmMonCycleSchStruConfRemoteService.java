package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMonCycleSchStruConf;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMonCycleSchStruConfRemoteService.java
 * 描    述：IMdmMonCycleSchStruConfRemoteService月周期排产结构配置前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
@FeignClient(contextId = "IMdmMonCycleSchStruConfRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMonCycleSchStruConfRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMonCycleSchStruConf/list")
    TableDataInfo list(@RequestBody MdmMonCycleSchStruConf QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmMonCycleSchStruConf/save")
    AjaxResult save(@RequestBody MdmMonCycleSchStruConf mdmMonCycleSchStruConf);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMonCycleSchStruConf/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMonCycleSchStruConf/{id}")
    MdmMonCycleSchStruConf getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMonCycleSchStruConf/checkUnique")
    String checkUnique(@RequestBody MdmMonCycleSchStruConf mdmMonCycleSchStruConfVO);

    /**
     * 导出月周期排产结构配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMonCycleSchStruConf/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMonCycleSchStruConf queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入月周期排产结构配置数据
     */
    @ApiOperation("导入月周期排产结构配置")
    @PostMapping("/mdmMonCycleSchStruConf/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
