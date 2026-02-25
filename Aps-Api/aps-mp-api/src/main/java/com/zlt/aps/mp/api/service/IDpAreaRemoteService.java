package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.DpArea;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpAreaRemoteService.java
 * 描    述：IDpAreaRemoteService区域前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@FeignClient(contextId = "IDpAreaRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IDpAreaRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/dpArea/list")
    TableDataInfo list(@RequestBody DpArea QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/dpArea/save")
    AjaxResult save(@RequestBody DpArea dpArea);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/dpArea/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/dpArea/{id}")
    DpArea getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/dpArea/checkUnique")
    String checkUnique(@RequestBody DpArea dpAreaVO);

    /**
     * 导出区域列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/dpArea/exportData/{fileName}")
    byte[] exportData(@RequestBody DpArea queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入区域数据
     */
    @ApiOperation("导入区域")
    @PostMapping("/dpArea/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
