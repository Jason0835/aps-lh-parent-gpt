package com.zlt.mix.schedule.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;

import javax.annotation.Resource;

import com.zlt.mix.schedule.api.domain.dto.ScheduleOperLogDto;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.mix.schedule.api.domain.entity.ScheduleOperLog;
import com.zlt.mix.schedule.service.ScheduleOperLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.common.core.utils.ExcelUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 排程操作日志Controller
 *
 * @author chen
 * @date 2022-07-13
 */
@RestController
@RequestMapping("/scheduleOperLog")
public class ScheduleOperLogController extends BaseController {
    @Resource
    private ScheduleOperLogService scheduleOperLogService;

    /**
     * 查询排程操作日志列表
     */
    @ApiOperation("查询排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo listScheduleOperLog(@RequestBody ScheduleOperLog scheduleOperLog) {
        startPage(false);
        scheduleOperLog.setOrderStr(orderStr());
        List<ScheduleOperLog> list = scheduleOperLogService.selectScheduleOperLogList(scheduleOperLog);
        return getDataTable(list);
    }

    @ApiOperation("获取排程操作日志详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public ScheduleOperLog getScheduleOperLogInfo(@PathVariable("id") Long id) {
        return scheduleOperLogService.getById(id);
    }

    @Log(title = "schedule.scheduleOperLog.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存排程操作日志信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveScheduleOperLog(@RequestBody ScheduleOperLog scheduleOperLog) {
        scheduleOperLogService.saveScheduleOperLog(scheduleOperLog);
        return AjaxResult.success();
    }

    @Log(title = "schedule.scheduleOperLog.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出排程操作日志列表")
    @PostMapping("/exportData")
    public byte[] exportData(@RequestBody ScheduleOperLogDto dto) {
        startPage(false);
        dto.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return scheduleOperLogService.exportData(dto);
    }
}
