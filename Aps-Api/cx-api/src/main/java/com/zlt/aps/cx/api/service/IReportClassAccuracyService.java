package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * 班次完成统计报表Service接口
 * @author chen
 * @date 2022-05-23
 */
@FeignClient(contextId = "IReportClassAccuracyService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IReportClassAccuracyService {

    /**
     * 查询班次完成统计报表列表
     */
    @ApiOperation("查询班次完成统计报表列表")
    @PostMapping("/reportClassAccuracy/list")
    TableDataInfo list(@RequestBody ReportClassAccuracyDto reportClassAccuracy);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/reportClassAccuracy/{id}")
    ReportClassAccuracyDto getInfo(@PathVariable("id") Long id);

    /**
     * 导出班次完成统计报表列表
     */
    @ApiOperation("导出班次完成统计报表列表")
    @PostMapping("/reportClassAccuracy/getList")
    List<ReportClassAccuracyDto> getList(@RequestBody ReportClassAccuracyDto reportClassAccuracy);

    /**
     * 导出班次完成统计报表列表
     */
    @ApiOperation("导出班次完成统计报表列表")
    @PostMapping("/reportClassAccuracy/export")
    byte[] export(@RequestBody ReportClassAccuracyDto reportClassAccuracy);
}
