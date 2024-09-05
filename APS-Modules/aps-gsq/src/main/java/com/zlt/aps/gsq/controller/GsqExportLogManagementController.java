package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqExportLogManagementDto;
import com.zlt.aps.gsq.service.GsqExportLogManagementService;
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
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
@RestController
@RequestMapping("/gsq/exportLogManagement")
@Api(tags = {"工序导出日志管理维护接口"})
public class GsqExportLogManagementController extends BaseController {
    @Autowired
    private GsqExportLogManagementService eportLogManagementService;

    /**
     * 查询工序导出日志管理列表
     *
     * @return
     */
    @ApiOperation("查询工序导出日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqExportLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqExportLogManagementDto> list = eportLogManagementService.selectExportLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导出日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id) {
        GsqExportLogManagementDto dto = new GsqExportLogManagementDto();
        BeanUtils.copyProperties(eportLogManagementService.getById(id), dto);
        return dto;
    }


}
