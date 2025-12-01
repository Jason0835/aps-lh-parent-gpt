package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMouldingProductParamDto;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductionMouldConfigurationRemoteService.java
 * 描    述：IProductionMouldConfigurationRemoteService模具正在生产的品种前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@FeignClient(contextId = "IProductionMouldConfigurationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductionMouldConfigurationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productionMouldConfiguration/list")
    TableDataInfo list(@RequestBody ProductionMouldConfiguration QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/productionMouldConfiguration/save")
    AjaxResult save(@RequestBody ProductionMouldConfiguration productionMouldConfiguration);

    /**
     * 生成模具正在生产的品种物料
     *
     * @param param
     * @return
     */
    @ApiOperation("生成模具正在生产的品种物料")
    @PostMapping("/productionMouldConfiguration/buildMouldingProduct")
    AjaxResult buildMouldingProduct(@RequestBody FactoryMouldingProductParamDto param);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productionMouldConfiguration/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productionMouldConfiguration/{id}")
    ProductionMouldConfiguration getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productionMouldConfiguration/checkUnique")
    String checkUnique(@RequestBody ProductionMouldConfiguration productionMouldConfigurationVO);

    /**
     * 导出模具正在生产的品种列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/productionMouldConfiguration/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductionMouldConfiguration queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具正在生产的品种数据
     */
    @ApiOperation("导入模具正在生产的品种")
    @PostMapping("/productionMouldConfiguration/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
