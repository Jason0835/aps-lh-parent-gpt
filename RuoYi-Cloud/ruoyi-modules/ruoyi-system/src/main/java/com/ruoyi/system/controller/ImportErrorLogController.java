package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.system.service.ImportErrorLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导入错误日志记录Controller
 *
 * @author zlt
 * @date 2021-07-27
 */
@RestController
@RequestMapping("/importErrorLog")
public class ImportErrorLogController extends BaseController {
    @Autowired
    private ImportErrorLogService importErrorLogService;

    /**
     * 查询导入错误日志记录列表
     */
    @PreAuthorize(hasPermi = "common:importErrorLog:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ImportErrorLog importErrorLog) {
        startPage("create_time desc");
        List<ImportErrorLog> list = importErrorLogService.selectImportErrorLogList(importErrorLog);
        return getDataTable(list);
    }

    /**
     * 获取导入错误日志记录详细信息
     */
    @PreAuthorize(hasPermi = "common:importErrorLog:query")
    @GetMapping(value = "/{id}")
    public ImportErrorLog getInfo(@PathVariable("id") Long id) {
        return importErrorLogService.selectImportErrorLogById(id);
    }

    /**
     * 新增导入错误日志记录
     */
    @PreAuthorize(hasPermi = "common:importErrorLog:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody ImportErrorLog importErrorLog) {
        return toAjax(importErrorLogService.insertImportErrorLog(importErrorLog));
    }

    /**
     * 修改导入错误日志记录
     */
    @PreAuthorize(hasPermi = "common:importErrorLog:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody ImportErrorLog importErrorLog) {
        return toAjax(importErrorLogService.updateImportErrorLog(importErrorLog));
    }

    /**
     * 删除导入错误日志记录
     */
    @PreAuthorize(hasPermi = "common:importErrorLog:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(importErrorLogService.deleteImportErrorLogByIds(ids));
    }

    /**
     * 导出导入错误日志记录列表
     */
    @PreAuthorize(hasPermi = "common:importErrorLog:export")
    @PostMapping("/getList")
    public List<ImportErrorLog> getList(@RequestBody ImportErrorLog importErrorLog) {
        startPage("create_time desc");
        return importErrorLogService.selectImportErrorLogList(importErrorLog);
    }

    /**
     * 校验导入错误日志记录唯一性
     */
    @ApiOperation("校验导入错误日志记录唯一性")
    @PostMapping("/checkImportErrorLogUnique")
    public String checkImportErrorLogUnique(@RequestBody ImportErrorLog importErrorLog) {
        return importErrorLogService.checkImportErrorLogUnique(importErrorLog);
    }

    /**
     * 批量新增导入错误日志记录
     *
     * @param importErrorLogs 导入错误日志记录
     * @return 结果
     */
    @PostMapping("/insertImportErrorLogList")
    public int insertImportErrorLogList(@RequestBody List<ImportErrorLog> importErrorLogs) {
        return importErrorLogService.insertImportErrorLogList(importErrorLogs);
    }
}
