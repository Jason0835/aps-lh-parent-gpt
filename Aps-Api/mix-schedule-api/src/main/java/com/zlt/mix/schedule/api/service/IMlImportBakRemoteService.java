package com.zlt.mix.schedule.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.vo.MlImportBak;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMlImportBakRemoteService.java
 * 描    述：IMlImportBakRemoteService密炼线下计划操作功能前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-05
 */
@FeignClient(contextId = "IMlImportBakRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE,
//        path = "${api.path.schedule:mixSchedule}"
        url = "http://localhost:9104"
)
public interface IMlImportBakRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mlImportBak/list")
    TableDataInfo list(@RequestBody MlImportBak QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mlImportBak/save")
    AjaxResult save(@RequestBody MlImportBak mlImportBak);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mlImportBak/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mlImportBak/{id}")
    MlImportBak getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mlImportBak/checkUnique")
    String checkUnique(@RequestBody MlImportBak mlImportBakVO);

    /**
     * 导出密炼线下计划操作功能列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mlImportBak/exportData/{fileName}")
    byte[] exportData(@RequestBody MlImportBak queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入密炼线下计划操作功能数据
     */
    @ApiOperation("导入密炼线下计划操作功能")
    @PostMapping("/mlImportBak/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入线下排程
     *
     * @return 结果
     */
    @ApiOperation("导入线下排程")
    @PostMapping("/mlImportBak/importOfflineData")
    public AjaxResult importOfflineData(@RequestBody List<MlImportBak> list, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("mixArea") String mixArea, @RequestParam("importLogId") Long importLogId);
}
