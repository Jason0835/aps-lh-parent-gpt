package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.TireCapacityConfigurationVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ITireCapacityConfigurationRemoteService.java
 * 描    述：ITireCapacityConfigurationRemoteService轮胎类型产能配置(特殊情况下配置)前端接口
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
@FeignClient(contextId = "ITireCapacityConfigurationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ITireCapacityConfigurationRemoteService {

    /**
     * 查询列表
     *
     * @param QueryVO
     * @return
     */
    @ApiOperation("查询列表")
    @PostMapping("/tireCapacity/list")
    TableDataInfo list(@RequestBody TireCapacityConfiguration QueryVO);

    /**
     * 根据分厂、年月、需求版本获取轮胎类型-寸口的需求信息
     *
     * @param condition
     * @return
     */
    @PostMapping("/tireCapacity/getDemandInfo")
    @ApiOperation("根据分厂、年月、需求版本获取轮胎类型-寸口的需求信息")
    TireCapacityConfigurationVo getDemandInfo(@RequestBody TireCapacityConfiguration condition);

    /**
     * 根据ID，获取配置信息
     *
     * @param id
     * @return
     */
    @PostMapping("/tireCapacity/getTireCapacityConfiguration")
    @ApiOperation("根据ID，获取配置信息，包含对应的参考需求信息-总需求量、净需求、备货需求")
    TireCapacityConfigurationVo getTireCapacityConfiguration(@RequestBody Long id);

    /**
     * 保存
     *
     * @param tireCapacityConfiguration
     * @return
     */
    @ApiOperation("保存")
    @PostMapping("/tireCapacity/save")
    AjaxResult save(@RequestBody TireCapacityConfiguration tireCapacityConfiguration);


    /**
     * 删除
     *
     * @param ids
     * @return
     */
    @ApiOperation("删除")
    @DeleteMapping("/tireCapacity/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/tireCapacity/{id}")
    TireCapacityConfiguration getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     *
     * @param tireCapacityConfigurationVO
     * @return
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/tireCapacity/checkUnique")
    String checkUnique(@RequestBody TireCapacityConfiguration tireCapacityConfigurationVO);

    /**
     * 导出轮胎类型产能配置(特殊情况下配置)列表
     *
     * @param queryVO
     * @param fileName
     * @return
     */
    @ApiOperation("导出列表")
    @PostMapping("/tireCapacity/exportData/{fileName}")
    byte[] exportData(@RequestBody TireCapacityConfiguration queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入轮胎类型产能配置(特殊情况下配置)数据
     *
     * @param importContext
     * @param updateSupport
     * @return
     */
    @ApiOperation("导入轮胎类型产能配置(特殊情况下配置)")
    @PostMapping("/tireCapacity/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
