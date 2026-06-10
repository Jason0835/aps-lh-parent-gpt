package com.zlt.aps.dj.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjImportErrorLogManagementDto;
import com.zlt.aps.dj.api.domain.dto.DjImportLogManagementDto;
import com.zlt.aps.dj.service.DjImportErrorLogManagementService;
import com.zlt.aps.dj.service.DjImportLogManagementService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 工序导入日志管理Controller
 *
 * @author duanjuntao
 * @date 2021-06-07
 */
@RestController
@RequestMapping("/dj/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class DjImportLogManagementController extends BaseController
{
    @Autowired
    private DjImportLogManagementService importLogManagementService;
    @Autowired
    private DjImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody DjImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        DjImportLogManagementDto dto = new DjImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
