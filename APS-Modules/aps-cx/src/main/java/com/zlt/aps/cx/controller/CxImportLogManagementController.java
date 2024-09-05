package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxImportErrorLogManagementDto;
import com.zlt.aps.cx.api.domain.dto.CxImportLogManagementDto;
import com.zlt.aps.cx.service.CxImportErrorLogManagementService;
import com.zlt.aps.cx.service.CxImportLogManagementService;
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
@RequestMapping("/cx/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class CxImportLogManagementController extends BaseController
{
    @Autowired
    private CxImportLogManagementService importLogManagementService;
    @Autowired
    private CxImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<CxImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody CxImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<CxImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        CxImportLogManagementDto dto = new CxImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
