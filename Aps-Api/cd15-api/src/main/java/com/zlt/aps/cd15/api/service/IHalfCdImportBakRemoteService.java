package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.HalfCdImportBak;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IHalfCdImportBakRemoteService.java
 * 描    述：IHalfCdImportBakRemoteService裁断线下计划导入导出前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-29
 */
@FeignClient(contextId = "IHalfCdImportBakRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE
        , path = "${api.path.cd15:/cd15}"
//        , url = "http://localhost:9005"
)
public interface IHalfCdImportBakRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/halfCdImportBak/list")
    TableDataInfo list(@RequestBody HalfCdImportBak QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/halfCdImportBak/save")
    AjaxResult save(@RequestBody HalfCdImportBak halfCdImportBak);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/halfCdImportBak/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/halfCdImportBak/{id}")
    HalfCdImportBak getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/halfCdImportBak/checkUnique")
    String checkUnique(@RequestBody HalfCdImportBak halfCdImportBakVO);

    /**
     * 导入线下计划导入数据
     */
    @ApiOperation("导入线下计划导入")
    @PostMapping("/halfCdImportBak/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导出线下计划导入列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/halfCdImportBak/importExcelToListAndExport")
    byte[] importExcelToListAndExport(@RequestBody ImportContext importContext);

    /**
     * 导入线下模板调整
     *
     * @param importContext 导入上下文
     * @return 结果
     */
    @ApiOperation("导入线下模板调整")
    @PostMapping("/halfCdImportBak/import4OfflineTemplate")
    public AjaxResult import4OfflineTemplate(@RequestBody ImportContext importContext);
}
