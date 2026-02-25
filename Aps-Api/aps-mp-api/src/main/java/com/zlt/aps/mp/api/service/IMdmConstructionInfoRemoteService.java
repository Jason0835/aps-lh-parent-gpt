package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmConstructionInfo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmConstructionInfoRemoteService.java
 * 描    述：IMdmConstructionInfoRemoteService投产胎胚施工信息前端接口
 *@author zlt
 *@date 2025-12-10
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmConstructionInfoRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmConstructionInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmConstructionInfo/list")
    TableDataInfo list(@RequestBody MdmConstructionInfo QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmConstructionInfo/save")
    AjaxResult save(@RequestBody MdmConstructionInfo mdmConstructionInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmConstructionInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmConstructionInfo/{id}")
    MdmConstructionInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmConstructionInfo/checkUnique")
    String checkUnique(@RequestBody MdmConstructionInfo mdmConstructionInfoVO);

    /**
     * 导出投产胎胚施工信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmConstructionInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmConstructionInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入投产胎胚施工信息数据
     */
    @ApiOperation("导入投产胎胚施工信息")
    @PostMapping("/mdmConstructionInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mdmConstructionInfo/mesCapture")
    AjaxResult mesCapture();

}
