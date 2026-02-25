package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmBomInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmBomInfoRemoteService.java
 * 描    述：IMdmBomInfoRemoteServiceBOM示方书前端接口
 *@author zlt
 *@date 2025-12-05
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmBomInfoRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmBomInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmBomInfo/list")
    TableDataInfo list(@RequestBody MdmBomInfo QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmBomInfo/save")
    AjaxResult save(@RequestBody MdmBomInfo mdmBomInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmBomInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmBomInfo/{id}")
    MdmBomInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmBomInfo/checkUnique")
    String checkUnique(@RequestBody MdmBomInfo mdmBomInfoVO);

    /**
     * 导出BOM示方书列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmBomInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmBomInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入BOM示方书数据
     */
    @ApiOperation("导入BOM示方书")
    @PostMapping("/mdmBomInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);


    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mdmBomInfo/mesCapture")
    AjaxResult mesCapture();

}
