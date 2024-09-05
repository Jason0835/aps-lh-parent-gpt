package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcImportErrorLogManagementDto;
import com.zlt.aps.nc.api.domain.dto.NcImportLogManagementDto;
import com.zlt.aps.nc.service.NcImportErrorLogManagementService;
import com.zlt.aps.nc.service.NcImportLogManagementService;
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
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
@RestController
@RequestMapping("/nc/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class NcImportLogManagementController extends BaseController
{
    @Autowired
    private NcImportLogManagementService importLogManagementService;
    @Autowired
    private NcImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody NcImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        NcImportLogManagementDto dto = new NcImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
