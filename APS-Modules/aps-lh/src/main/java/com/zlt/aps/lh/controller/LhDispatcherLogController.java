package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.service.ILhDispatcherLogService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：LhDispatcherLogController.java
* 描    述：硫化调度员排程操作日志 控制层类：....
*@author zlt
*@date 2025-03-21
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "硫化调度员排程操作日志")
@RestController
@RequestMapping("/lhDispatcherLog")
public class LhDispatcherLogController extends BusiController<LhDispatcherLog> {

    @Autowired
    private ILhDispatcherLogService lhDispatcherLogService;

    /**
     * 查询硫化调度员排程操作日志列表
     */
    @RequiresPermissions("lh:dispatcherLog:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhDispatcherLog queryVO) {
        startPage(getOrderBy());
        List<LhDispatcherLog> list = lhDispatcherLogService.selectList(queryVO);
        return getDataTable(list);
    }

    protected String getOrderBy() {
        return "create_time desc";
    }


    /**
     * 导出列表
     */
    @RequiresPermissions( "lh:dispatcherLog:export")
    @Log(title = "硫化调度员排程操作日志", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody LhDispatcherLog queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @Override
    protected List<LhDispatcherLog> listExportData(LhDispatcherLog queryVO) {
        startPage(getOrderBy());
        return lhDispatcherLogService.selectList(queryVO);
    }

}
