package com.zlt.aps.cd15.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportErrorLogManagementDto;
import com.zlt.aps.cd15.api.domain.dto.Cd15ImportLogManagementDto;
import com.zlt.aps.cd15.service.Cd15ImportErrorLogManagementService;
import com.zlt.aps.cd15.service.Cd15ImportLogManagementService;
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
@RequestMapping("/cd15/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class Cd15ImportLogManagementController extends BaseController
{
    @Autowired
    private Cd15ImportLogManagementService importLogManagementService;
    @Autowired
    private Cd15ImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15ImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd15ImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody Cd15ImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd15ImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15ImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        Cd15ImportLogManagementDto dto = new Cd15ImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
