package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICd90CurlLengthRemoteService.java
 * 描    述：ICd90CurlLengthRemoteService纤维直裁卷曲长度前端接口
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "ICd90CurlLengthRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90CurlLengthRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cd90CurlLength/list")
    TableDataInfo list(@RequestBody Cd90CurlLength QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/cd90CurlLength/save")
    AjaxResult save(@RequestBody Cd90CurlLength cd90CurlLength);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cd90CurlLength/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cd90CurlLength/{id}")
    Cd90CurlLength getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cd90CurlLength/checkUnique")
    String checkUnique(@RequestBody Cd90CurlLength cd90CurlLengthVO);

    /**
     * 导出纤维直裁卷曲长度列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/cd90CurlLength/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90CurlLength queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入纤维直裁卷曲长度数据
     */
    @ApiOperation("导入纤维直裁卷曲长度")
    @PostMapping("/cd90CurlLength/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlLength 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/cd90CurlLength/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody Cd90CurlLength curlLength);
}
