package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.ScheduleImportErrorLogManagementDto;
import com.zlt.mix.schedule.api.domain.entity.ScheduleImportLogManagement;
import com.zlt.mix.schedule.service.ScheduleImportErrorLogManagementService;
import com.zlt.mix.schedule.service.ScheduleImportLogManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工序导入日志管理Controller
 */
@RestController
@RequestMapping("/schedule/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class ScheduleImportLogManagementController extends BaseController
{
    @Autowired
    private ScheduleImportLogManagementService importLogManagementService;
    @Autowired
    private ScheduleImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ScheduleImportLogManagement dto)
    {
        startPage(false);
        dto.setOrderStr(orderStr());
        List<ScheduleImportLogManagement> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody ScheduleImportErrorLogManagementDto dto) {
        startPage(false);
        dto.setOrderStr(orderStr());
        List<ScheduleImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public ScheduleImportLogManagement getImportLogManagement(@PathVariable("id") Long id) {
        ScheduleImportLogManagement dto = new ScheduleImportLogManagement();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
