package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmModelInfoRemoteService.java
 * 描    述：IMdmModelInfoRemoteService模具信息前端接口
 *@author zlt
 *@date 2025-02-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmModelInfoRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmModelInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmModelInfo/list")
    TableDataInfo list(@RequestBody MdmModelInfo QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmModelInfo/save")
    AjaxResult save(@RequestBody MdmModelInfo mdmModelInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmModelInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmModelInfo/{id}")
    MdmModelInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmModelInfo/checkUnique")
    String checkUnique(@RequestBody MdmModelInfo mdmModelInfoVO);

    /**
     * 导出模具信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmModelInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmModelInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 模具汇总导出
    */
    @ApiOperation("模具汇总导出")
    @PostMapping("/mdmModelInfo/exportModelGroup/{fileName}")
    byte[] exportModelGroup(@RequestBody MdmModelInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具信息数据
     */
    @ApiOperation("导入模具信息")
    @PostMapping("/mdmModelInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mdmModelInfo/mesCapture")
    AjaxResult mesCapture();
}
