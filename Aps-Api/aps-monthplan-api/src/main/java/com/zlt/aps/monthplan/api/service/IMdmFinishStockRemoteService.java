package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmFinishStock;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmFinishStockRemoteService.java
 * 描    述：IMdmFinishStockRemoteService成品库存前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@FeignClient(contextId = "IMdmFinishStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmFinishStockRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmFinishStock/list")
    TableDataInfo list(@RequestBody MdmFinishStock QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmFinishStock/save")
    AjaxResult save(@RequestBody MdmFinishStock mdmFinishStock);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmFinishStock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmFinishStock/{id}")
    MdmFinishStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmFinishStock/checkUnique")
    String checkUnique(@RequestBody MdmFinishStock mdmFinishStockVO);

    /**
     * 导出成品库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmFinishStock/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmFinishStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入成品库存数据
     */
    @ApiOperation("导入成品库存")
    @PostMapping("/mdmFinishStock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 查询MES实时成品库存列表
     *
     * @param mdmFinishStock 查询参数
     * @return 结果
     */
    @ApiOperation("查询MES实时成品库存列表")
    @PostMapping("/mdmFinishStock/list4Mes")
    TableDataInfo list4Mes(@RequestBody MdmFinishStock mdmFinishStock);

    /**
     * 导出成品库存列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmFinishStock/export4Mes/{fileName}")
    byte[] export4Mes(@RequestBody MdmFinishStock queryVO, @PathVariable("fileName") String fileName);
}
