package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryNoProductionRemoteService.java
 * 描    述：IFactoryNoProductionRemoteService基础数据-分厂不排产前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
@FeignClient(contextId = "IFactoryNoProductionRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryNoProductionRemoteService {

    /**
     * 查询基础数据-分厂不排产列表
     */
    @ApiOperation("查询基础数据-分厂不排产列表")
    @PostMapping("/factoryNoProduction/list")
    TableDataInfo list(@RequestBody FactoryNoProduction factoryNoProduction);

    /**
     * 新增基础数据-分厂不排产
     */
    @ApiOperation("新增基础数据-分厂不排产")
    @PostMapping("/factoryNoProduction/add")
    AjaxResult add(@RequestBody FactoryNoProduction factoryNoProduction);

    /**
     * 修改基础数据-分厂不排产
     */
    @ApiOperation("修改基础数据-分厂不排产")
    @PostMapping("/factoryNoProduction/edit")
    AjaxResult edit(@RequestBody FactoryNoProduction factoryNoProduction);

    /**
     * 删除基础数据-分厂不排产
     */
    @ApiOperation("删除基础数据-分厂不排产")
    @DeleteMapping("/factoryNoProduction/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/factoryNoProduction/{id}")
    FactoryNoProduction getInfo(@PathVariable("id") Long id);

    /**
     * 校验基础数据-分厂不排产唯一性
     */
    @ApiOperation("校验基础数据-分厂不排产唯一性")
    @PostMapping("/factoryNoProduction/checkFactoryNoProductionUnique")
    String checkFactoryNoProductionUnique(@RequestBody FactoryNoProduction factoryNoProduction);

    /**
     * 导入分厂不排产设定数据
     *
     * @param context
     * @param updateSupport
     * @return
     */
    @ApiOperation("导入分厂不排产设定")
    @PostMapping("/factoryNoProduction/importData")
    AjaxResult importData(@RequestBody ImportContext context, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入分厂不排产设定数据 For AI
     *
     * @param factoryNoProductionList
     * @return
     */
    @ApiOperation("导入分厂不排产设定For AI")
    @PostMapping("/factoryNoProduction/importDataForAI")
    AjaxResult importDataForAI(@RequestBody List<FactoryNoProduction> factoryNoProductionList);
}
