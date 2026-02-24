package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IRawSpecialMaterialRecordRemoteService.java
 * 描    述：IRawSpecialMaterialRecordRemoteService特殊材料清单前端接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IRawSpecialMaterialRecordRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IRawSpecialMaterialRecordRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/rawSpecialMaterialRecord/list")
    TableDataInfo list(@RequestBody RawSpecialMaterialRecord QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/rawSpecialMaterialRecord/save")
    AjaxResult save(@RequestBody RawSpecialMaterialRecord rawSpecialMaterialRecord);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/rawSpecialMaterialRecord/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/rawSpecialMaterialRecord/{id}")
    RawSpecialMaterialRecord getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/rawSpecialMaterialRecord/checkUnique")
    String checkUnique(@RequestBody RawSpecialMaterialRecord rawSpecialMaterialRecordVO);

    /**
     * 导出特殊材料清单列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/rawSpecialMaterialRecord/exportData/{fileName}")
    byte[] exportData(@RequestBody RawSpecialMaterialRecord queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入特殊材料清单数据
     */
    @ApiOperation("导入特殊材料清单")
    @PostMapping("/rawSpecialMaterialRecord/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
