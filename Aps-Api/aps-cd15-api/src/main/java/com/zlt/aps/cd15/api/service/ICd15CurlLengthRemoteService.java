package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICd15CurlLengthRemoteService.java
 * 描    述：ICd15CurlLengthRemoteService钢丝斜裁卷曲长度前端接口
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICd15CurlLengthRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15CurlLengthRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cd15CurlLength/list")
    TableDataInfo list(@RequestBody Cd15CurlLength QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cd15CurlLength/save")
    AjaxResult save(@RequestBody Cd15CurlLength cd15CurlLength);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cd15CurlLength/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cd15CurlLength/{id}")
    Cd15CurlLength getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cd15CurlLength/checkUnique")
    String checkUnique(@RequestBody Cd15CurlLength cd15CurlLengthVO);

    /**
     * 导出钢丝斜裁卷曲长度列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cd15CurlLength/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15CurlLength queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝斜裁卷曲长度数据
     */
    @ApiOperation("导入钢丝斜裁卷曲长度")
    @PostMapping("/cd15CurlLength/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlLength 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/cd15CurlLength/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody Cd15CurlLength curlLength);

}
