package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqImportErrorLogManagementDto;
import com.zlt.aps.gsq.api.domain.dto.GsqImportLogManagementDto;
import com.zlt.aps.gsq.service.GsqImportErrorLogManagementService;
import com.zlt.aps.gsq.service.GsqImportLogManagementService;
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
@RequestMapping("/gsq/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class GsqImportLogManagementController extends BaseController
{
    @Autowired
    private GsqImportLogManagementService importLogManagementService;
    @Autowired
    private GsqImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody GsqImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        GsqImportLogManagementDto dto = new GsqImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
