package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpOrderOffsetDetailRemoteService.java
 * 描    述：IDpOrderOffsetDetailRemoteServiceS1-0604订单冲减分配前端接口
 *@author zlt
 *@date 2026-01-23
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IDpOrderOffsetDetailRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IDpOrderOffsetDetailRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/dpOrderOffsetDetail/list")
    TableDataInfo list(@RequestBody DpOrderOffsetDetail QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/dpOrderOffsetDetail/save")
    AjaxResult save(@RequestBody DpOrderOffsetDetail dpOrderOffsetDetail);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/dpOrderOffsetDetail/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dpOrderOffsetDetail/{id}")
    DpOrderOffsetDetail getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/dpOrderOffsetDetail/checkUnique")
    String checkUnique(@RequestBody DpOrderOffsetDetail dpOrderOffsetDetailVO);

    /**
     * 导出S1-0604订单冲减分配列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/dpOrderOffsetDetail/exportData/{fileName}")
    byte[] exportData(@RequestBody DpOrderOffsetDetail queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入S1-0604订单冲减分配数据
     */
    @ApiOperation("导入S1-0604订单冲减分配")
    @PostMapping("/dpOrderOffsetDetail/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
