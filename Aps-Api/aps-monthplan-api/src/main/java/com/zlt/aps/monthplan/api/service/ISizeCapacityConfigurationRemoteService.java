package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISizeCapacityConfigurationRemoteService.java
 * 描    述：ISizeCapacityConfigurationRemoteService寸口产能配置前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */
@FeignClient(contextId = "ISizeCapacityConfigurationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ISizeCapacityConfigurationRemoteService {

    /**
     * 查询列表
     *
     * @param QueryVO
     * @return
     */
    @PostMapping("/sizeCapacity/list")
    @ApiOperation("根据查询条件，查询列表数据")
    TableDataInfo list(@RequestBody SizeCapacityConfiguration QueryVO);

    /**
     * 根据分厂、年月、需求版本获取寸口的需求信息
     *
     * @param condition
     * @return
     */
    @PostMapping("/sizeCapacity/getDemandInfo")
    @ApiOperation("根据分厂、年月、需求版本获取寸口的需求信息")
    SizeCapacityConfigurationVo getDemandInfo(@RequestBody SizeCapacityConfiguration condition);

    /**
     * 根据ID，获取配置信息
     *
     * @param id
     * @return
     */
    @PostMapping("/sizeCapacity/getSizeCapacityConfiguration")
    @ApiOperation("根据ID，获取配置信息，包含对应的参考需求信息-总需求量、净需求、备货需求")
    SizeCapacityConfigurationVo getSizeCapacityConfiguration(@RequestBody Long id);

    /**
     * 根据分厂、年、月、需求版本，生产寸口产能配置
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/sizeCapacity/buildSizeCapacityConfiguration")
    @ApiOperation("根据分厂、年、月、需求版本，生产寸口产能配置")
    AjaxResult autoBuildConfiguration(@RequestBody BuildSizeCapacityParamVo factoryProductionParam);

    /**
     * 根据分厂、年、月、查看产能配置详情
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/sizeCapacity/getDaySizeCapacityInfo")
    @ApiOperation("根据分厂、年、月、查看产能配置详情")
    List<DaySizeCapacityConfigurationDetailVo> getDaySizeCapacityInfo(@RequestBody FactoryProductionParamVo factoryProductionParam);


    /**
     * 根据分厂、年、月、查看产能配置详情
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/sizeCapacity/getSizeDayCapacityInfo")
    @ApiOperation("根据分厂、年、月、查看产能配置详情")
    List<DaySizeCapacityConfigurationMouldMethodDetailVo> getSizeDayCapacityInfo(@RequestBody FactoryProductionParamVo factoryProductionParam);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/sizeCapacity/save")
    AjaxResult save(@RequestBody SizeCapacityConfiguration sizeCapacityConfiguration);

    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @ApiOperation("删除")
    @DeleteMapping("/sizeCapacity/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/sizeCapacity/{id}")
    SizeCapacityConfiguration getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     *
     * @param sizeCapacityConfigurationVO
     * @return
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/sizeCapacity/checkUnique")
    String checkUnique(@RequestBody SizeCapacityConfiguration sizeCapacityConfigurationVO);

    /**
     * 导出寸口产能配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/sizeCapacity/exportData/{fileName}")
    byte[] exportData(@RequestBody SizeCapacityConfiguration queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入寸口产能配置数据
     */
    @ApiOperation("导入寸口产能配置")
    @PostMapping("/sizeCapacity/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
