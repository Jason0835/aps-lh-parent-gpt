package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.system.service.ExportLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导出记录Controller
 *
 * @author zlt
 * @date 2021-07-24
 */
@RestController
@RequestMapping("/exportLog")
public class ExportLogController extends BaseController {
    @Autowired
    private ExportLogService exportLogService;

    /**
     * 查询导出记录列表
     */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ExportLog exportLog) {
        startPage("create_time desc");
        List<ExportLog> list = exportLogService.selectExportLogList(exportLog);
        return getDataTable(list);
    }

    /**
     * 获取导出记录详细信息
     */
    @GetMapping(value = "/{id}")
    public ExportLog getInfo(@PathVariable("id") Long id) {
        return exportLogService.selectExportLogById(id);
    }

    /**
     * 新增导出记录
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody ExportLog exportLog) {
        return toAjax(exportLogService.insertExportLog(exportLog));
    }

    /**
     * 修改导出记录
     */
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody ExportLog exportLog) {
        return toAjax(exportLogService.updateExportLog(exportLog));
    }

    /**
     * 删除导出记录
     */
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(exportLogService.deleteExportLogByIds(ids));
    }

    /**
     * 导出导出记录列表
     */
    @PostMapping("/getList")
    public List<ExportLog> getList(@RequestBody ExportLog exportLog) {
        startPage("create_time desc");
        return exportLogService.selectExportLogList(exportLog);
    }

    /**
     * 校验导出记录唯一性
     */
    @ApiOperation("校验导出记录唯一性")
    @PostMapping("/checkExportLogUnique")
    public String checkExportLogUnique(@RequestBody ExportLog exportLog) {
        return exportLogService.checkExportLogUnique(exportLog);
    }

}
