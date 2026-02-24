package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmCycleSchStruConfRemoteService.java
 * 描    述：IMdmCycleSchStruConfRemoteService周期排产结构配置前端接口
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
@FeignClient(contextId = "IMdmCycleSchStruConfRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmCycleSchStruConfRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmCycleSchStruConf/list")
    TableDataInfo list(@RequestBody MdmCycleSchStruConf QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmCycleSchStruConf/save")
    AjaxResult save(@RequestBody MdmCycleSchStruConf mdmCycleSchStruConf);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmCycleSchStruConf/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmCycleSchStruConf/{id}")
    MdmCycleSchStruConf getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmCycleSchStruConf/checkUnique")
    String checkUnique(@RequestBody MdmCycleSchStruConf mdmCycleSchStruConfVO);

    /**
     * 导出周期排产结构配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmCycleSchStruConf/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmCycleSchStruConf queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入周期排产结构配置数据
     */
    @ApiOperation("导入周期排产结构配置")
    @PostMapping("/mdmCycleSchStruConf/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 生成月周期排产结构配置
     *
     * @param mdmCycleSchStruConf 参数
     * @return 结果
     */
    @ApiOperation("生成月周期排产结构配置")
    @PostMapping("/mdmCycleSchStruConf/genMonthCycleSchStruConf")
    AjaxResult genMonthCycleSchStruConf(@RequestBody MdmCycleSchStruConf mdmCycleSchStruConf);

}
