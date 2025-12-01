package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.HalfYcImportBak;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IHalfYcImportBakRemoteService.java
 * 描    述：IHalfYcImportBakRemoteService线下计划导入前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-26
 */
@FeignClient(contextId = "IHalfYcImportBakRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface IHalfYcImportBakRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/halfYcImportBak/list")
    TableDataInfo list(@RequestBody HalfYcImportBak QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/halfYcImportBak/save")
    AjaxResult save(@RequestBody HalfYcImportBak halfYcImportBak);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/halfYcImportBak/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/halfYcImportBak/{id}")
    HalfYcImportBak getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/halfYcImportBak/checkUnique")
    String checkUnique(@RequestBody HalfYcImportBak halfYcImportBakVO);

    /**
     * 导出线下计划导入列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/halfYcImportBak/exportData/{fileName}")
    byte[] exportData(@RequestBody HalfYcImportBak queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入线下计划导入数据
     */
    @ApiOperation("导入线下计划导入")
    @PostMapping("/halfYcImportBak/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导出线下计划导入列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/halfYcImportBak/importExcelToListAndExport")
    byte[] importExcelToListAndExport(@RequestBody ImportContext importContext);

    /**
     * 导入线下模板调整
     *
     * @param importContext 导入上下文
     * @return 结果
     */
    @ApiOperation("导入线下模板调整")
    @PostMapping("/halfYcImportBak/import4OfflineTemplate")
    public AjaxResult import4OfflineTemplate(@RequestBody ImportContext importContext);
}
