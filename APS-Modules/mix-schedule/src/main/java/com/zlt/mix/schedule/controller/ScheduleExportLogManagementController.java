package com.zlt.mix.schedule.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.entity.ScheduleExportLogManagement;
import com.zlt.mix.schedule.service.ScheduleExportLogManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工序导出日志管理Controller
 */
@RestController
@RequestMapping("/schedule/exportLogManagement")
@Api(tags = {"工序导出日志管理维护接口"})
public class ScheduleExportLogManagementController extends BaseController {
    @Autowired
    private ScheduleExportLogManagementService eportLogManagementService;

    /**
     * 查询工序导出日志管理列表
     *
     * @return
     */
    @ApiOperation("查询工序导出日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ScheduleExportLogManagement dto) {
        startPage(false);
        dto.setOrderStr(orderStr());
        List<ScheduleExportLogManagement> list = eportLogManagementService.selectExportLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导出日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public ScheduleExportLogManagement getExportLogManagement(@PathVariable("id") Long id) {
        ScheduleExportLogManagement dto = new ScheduleExportLogManagement();
        BeanUtils.copyProperties(eportLogManagementService.getById(id), dto);
        return dto;
    }


}
