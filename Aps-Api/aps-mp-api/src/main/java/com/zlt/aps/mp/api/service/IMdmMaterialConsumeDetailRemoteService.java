package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMaterialConsumeDetailRemoteService.java
 * 描    述：IMdmMaterialConsumeDetailRemoteService原材料消耗明细前端接口
 *@author zlt
 *@date 2026-03-03
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmMaterialConsumeDetailRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMaterialConsumeDetailRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmMaterialConsumeDetail/list")
    TableDataInfo list(@RequestBody MdmMaterialConsumeDetail QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmMaterialConsumeDetail/save")
    AjaxResult save(@RequestBody MdmMaterialConsumeDetail mdmMaterialConsumeDetail);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmMaterialConsumeDetail/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMaterialConsumeDetail/{id}")
    MdmMaterialConsumeDetail getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmMaterialConsumeDetail/checkUnique")
    String checkUnique(@RequestBody MdmMaterialConsumeDetail mdmMaterialConsumeDetailVO);

    /**
     * 导出原材料消耗明细列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmMaterialConsumeDetail/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMaterialConsumeDetail queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入原材料消耗明细数据
     */
    @ApiOperation("导入原材料消耗明细")
    @PostMapping("/mdmMaterialConsumeDetail/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
