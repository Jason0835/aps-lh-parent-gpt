package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportErrorLogManagementDto;
import com.zlt.aps.gdyy.api.domain.dto.GdyyImportLogManagementDto;
import com.zlt.aps.gdyy.service.GdyyImportErrorLogManagementService;
import com.zlt.aps.gdyy.service.GdyyImportLogManagementService;
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
@RequestMapping("/gdyy/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class GdyyImportLogManagementController extends BaseController
{
    @Autowired
    private GdyyImportLogManagementService importLogManagementService;
    @Autowired
    private GdyyImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<GdyyImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody GdyyImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GdyyImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyyImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        GdyyImportLogManagementDto dto = new GdyyImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
