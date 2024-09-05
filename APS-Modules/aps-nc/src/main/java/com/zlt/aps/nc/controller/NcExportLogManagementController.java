package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcExportLogManagementDto;
import com.zlt.aps.nc.service.NcExportLogManagementService;
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
@RequestMapping("/nc/exportLogManagement")
@Api(tags = {"工序导出日志管理维护接口"})
public class NcExportLogManagementController extends BaseController {
    @Autowired
    private NcExportLogManagementService eportLogManagementService;

    /**
     * 查询工序导出日志管理列表
     *
     * @return
     */
    @ApiOperation("查询工序导出日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcExportLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcExportLogManagementDto> list = eportLogManagementService.selectExportLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导出日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcExportLogManagementDto getExportLogManagement(@PathVariable("id") Long id) {
        NcExportLogManagementDto dto = new NcExportLogManagementDto();
        BeanUtils.copyProperties(eportLogManagementService.getById(id), dto);
        return dto;
    }


}
