package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.service.CxDispatcherLogService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxDispatcherLog;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 成型调度员排程操作日志Controller
 *
 * @author Gim
 * @date 2022-02-25
 */
@RestController
@RequestMapping("/cx/dispatcherLog")
public class CxDispatcherLogController extends BusiController<CxDispatcherLog> {
    @Autowired
    private CxDispatcherLogService dispatcherLogService;

    /**
     * 查询成型调度员排程操作日志列表
     */
    @ApiOperation("查询成型调度员排程操作日志列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxDispatcherLog dispatcherLog) {
        startPage(orderStr());
        List<CxDispatcherLog> list = dispatcherLogService.selectCxDispatcherLogList(dispatcherLog);
        return getDataTable(list);
    }
    
    /**
     * 导出列表
     */
    @Log(title = "成型调度员排程操作日志", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody CxDispatcherLog queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @Override
    protected List<CxDispatcherLog> listExportData(CxDispatcherLog query) {
        startPage(orderStr());
        return dispatcherLogService.selectCxDispatcherLogList(query);
    }
}
