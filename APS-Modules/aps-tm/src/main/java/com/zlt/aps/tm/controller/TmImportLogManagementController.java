package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmImportLogManagementDto;
import com.zlt.aps.tm.api.domain.dto.TmImportErrorLogManagementDto;
import com.zlt.aps.tm.service.TmImportLogManagementService;
import com.zlt.aps.tm.service.TmImportErrorLogManagementService;
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
@RequestMapping("/tm/importLogManagement")
@Api(tags = {"工序导入日志管理维护接口"})
public class TmImportLogManagementController extends BaseController
{
    @Autowired
    private TmImportLogManagementService importLogManagementService;
    @Autowired
    private TmImportErrorLogManagementService importErrorLogManagementService;

    /**
     * 查询工序导入日志管理列表
     * @return
     */
    @ApiOperation("查询工序导入日志管理列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmImportLogManagementDto dto)
    {
        startPage();
        dto.setOrderStr(orderStr());
        List<TmImportLogManagementDto> list = importLogManagementService.selectImportLogManagementList(dto);
        return getDataTable(list);
    }

    /**
     * 错误详情日志列表
     * @return
     */
    @ApiOperation("错误详情日志列表")
    @PostMapping("/errorView")
    public TableDataInfo errorView(@RequestBody TmImportErrorLogManagementDto dto) {
        startPage();
        List<TmImportErrorLogManagementDto> list = importErrorLogManagementService.selectImportErrorLogManagementList(dto);
        return getDataTable(list);
    }

    @ApiOperation("查询工序导入日志管理详细信息")
    @GetMapping("/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmImportLogManagementDto getImportLogManagement(@PathVariable("id") Long id) {
        TmImportLogManagementDto dto = new TmImportLogManagementDto();
        BeanUtils.copyProperties(importLogManagementService.getById(id), dto);
        return dto;
    }

}
