package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportErrorLogManagementDto;
import com.zlt.aps.xwyy.api.domain.dto.XwyyImportLogManagementDto;
import com.zlt.aps.xwyy.service.XwyyImportErrorLogManagementService;
import com.zlt.aps.xwyy.service.XwyyImportLogManagementService;
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
@RequestMapping("/xwyy/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class XwyyImportLogManagementController extends BaseController
{
    @Autowired
    private XwyyImportLogManagementService importLogManagementService;
    @Autowired
    private XwyyImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<XwyyImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody XwyyImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<XwyyImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        XwyyImportLogManagementDto dto = new XwyyImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
