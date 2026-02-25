package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmCapsuleChuck;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmCapsuleChuckRemoteService.java
 * 描    述：IMdmCapsuleChuckRemoteService胶囊卡盘台账前端接口
 *@author zlt
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmCapsuleChuckRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmCapsuleChuckRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmCapsuleChuck/list")
    TableDataInfo list(@RequestBody MdmCapsuleChuck QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmCapsuleChuck/save")
    AjaxResult save(@RequestBody MdmCapsuleChuck mdmCapsuleChuck);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmCapsuleChuck/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmCapsuleChuck/{id}")
    MdmCapsuleChuck getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmCapsuleChuck/checkUnique")
    String checkUnique(@RequestBody MdmCapsuleChuck mdmCapsuleChuckVO);

    /**
     * 导出胶囊卡盘台账列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmCapsuleChuck/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmCapsuleChuck queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入胶囊卡盘台账数据
     */
    @ApiOperation("导入胶囊卡盘台账")
    @PostMapping("/mdmCapsuleChuck/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
