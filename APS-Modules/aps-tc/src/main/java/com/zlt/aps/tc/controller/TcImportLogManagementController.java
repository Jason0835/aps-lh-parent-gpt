package com.zlt.aps.tc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcImportErrorLogManagementDto;
import com.zlt.aps.tc.api.domain.dto.TcImportLogManagementDto;
import com.zlt.aps.tc.service.TcImportErrorLogManagementService;
import com.zlt.aps.tc.service.TcImportLogManagementService;
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
@RequestMapping("/tc/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class TcImportLogManagementController extends BaseController
{
    @Autowired
    private TcImportLogManagementService importLogManagementService;
    @Autowired
    private TcImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody TcImportErrorLogManagementDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TcImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        TcImportLogManagementDto dto = new TcImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
