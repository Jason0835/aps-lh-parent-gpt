package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.LocationChannelConfiguration;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILocationChannelConfigurationRemoteService.java
 * 描    述：ILocationChannelConfigurationRemoteService库位类别渠道品牌配置前端接口
 *@author ZLT
 *@date 2025-02-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：ZLT
 *     修改内容：...
 */
@FeignClient(contextId = "ILocationChannelConfigurationRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface ILocationChannelConfigurationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/LocationChannelConfiguration/list")
    TableDataInfo list(@RequestBody LocationChannelConfiguration QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/LocationChannelConfiguration/save")
    AjaxResult save(@RequestBody LocationChannelConfiguration locationChannelConfiguration);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/LocationChannelConfiguration/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/LocationChannelConfiguration/{id}")
    LocationChannelConfiguration getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/LocationChannelConfiguration/checkUnique")
    String checkUnique(@RequestBody LocationChannelConfiguration locationChannelConfigurationVO);

    /**
     * 导出库位类别渠道品牌配置列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/LocationChannelConfiguration/exportData/{fileName}")
    byte[] exportData(@RequestBody LocationChannelConfiguration queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入库位类别渠道品牌配置数据
     */
    @ApiOperation("导入库位类别渠道品牌配置")
    @PostMapping("/LocationChannelConfiguration/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
