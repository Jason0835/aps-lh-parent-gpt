package com.zlt.aps.gdyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.entity.GdyyOriginalLineSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IGdyyOriginalLineSpecRemoteService.java
 * 描    述：IGdyyOriginalLineSpecRemoteService钢丝压延原线规格前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-07-08
 */
@FeignClient(contextId = "IGdyyOriginalLineSpecRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:/gdyy}")
public interface IGdyyOriginalLineSpecRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/gdyyOriginalLineSpec/list")
    TableDataInfo list(@RequestBody GdyyOriginalLineSpec QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/gdyyOriginalLineSpec/save")
    AjaxResult save(@RequestBody GdyyOriginalLineSpec gdyyOriginalLineSpec);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/gdyyOriginalLineSpec/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/gdyyOriginalLineSpec/{id}")
    GdyyOriginalLineSpec getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/gdyyOriginalLineSpec/checkUnique")
    String checkUnique(@RequestBody GdyyOriginalLineSpec gdyyOriginalLineSpecVO);

    /**
     * 导出钢丝压延原线规格列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/gdyyOriginalLineSpec/exportData/{fileName}")
    byte[] exportData(@RequestBody GdyyOriginalLineSpec queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入钢丝压延原线规格数据
     */
    @ApiOperation("导入钢丝压延原线规格")
    @PostMapping("/gdyyOriginalLineSpec/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
