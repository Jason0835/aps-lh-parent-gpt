package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmProductConstruction;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductConstructionRemoteService.java
 * 描    述：IMdmProductConstructionRemoteServiceSAP与施工对照前端接口
 *@author zlt
 *@date 2025-02-25
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmProductConstructionRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmProductConstructionRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmProductConstruction/list")
    TableDataInfo list(@RequestBody MdmProductConstruction QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmProductConstruction/save")
    AjaxResult save(@RequestBody MdmProductConstruction mdmProductConstruction);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmProductConstruction/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmProductConstruction/{id}")
    MdmProductConstruction getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmProductConstruction/checkUnique")
    String checkUnique(@RequestBody MdmProductConstruction mdmProductConstructionVO);

    /**
     * 导出SAP与施工对照列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmProductConstruction/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmProductConstruction queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入SAP与施工对照数据
     */
    @ApiOperation("导入SAP与施工对照")
    @PostMapping("/mdmProductConstruction/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入客户格式数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 是否更新已存在的数据
     * @return 结果
     */
    @ApiOperation("导入客户格式数据")
    @PostMapping("/mdmProductConstruction/importOfflineData")
    public AjaxResult importOfflineData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
