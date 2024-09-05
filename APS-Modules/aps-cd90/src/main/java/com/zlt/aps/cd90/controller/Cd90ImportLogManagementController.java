package com.zlt.aps.cd90.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportErrorLogManagementDto;
import com.zlt.aps.cd90.api.domain.dto.Cd90ImportLogManagementDto;
import com.zlt.aps.cd90.service.Cd90ImportErrorLogManagementService;
import com.zlt.aps.cd90.service.Cd90ImportLogManagementService;
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
@RequestMapping("/cd90/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class Cd90ImportLogManagementController extends BaseController
{
    @Autowired
    private Cd90ImportLogManagementService importLogManagementService;
    @Autowired
    private Cd90ImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd90ImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd90ImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody Cd90ImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd90ImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd90ImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        Cd90ImportLogManagementDto dto = new Cd90ImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
