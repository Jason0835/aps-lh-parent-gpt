package com.ruoyi.system.controller;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.api.gateway.system.domain.SysOperLog;
import com.ruoyi.system.service.ISysOperLogService;

/**
 * 操作日志记录
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/operlog")
public class SysOperlogController extends BaseController
{
    @Autowired
    private ISysOperLogService operLogService;

    @GetMapping("/list")
    public TableDataInfo list(SysOperLog operLog)
    {
        startPage();
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    @Log(title = "system.title.operlog", businessType = BusinessType.EXPORT)
    @GetMapping("/totalList")
    public List<SysOperLog> totalList(SysOperLog role)
    {
        List<SysOperLog> list = operLogService.selectOperLogList(role);
        return list;
    }

    @Log(title = "system.title.operlog", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysOperLog operLog) throws IOException
    {
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.operlog"));
    }

    @DeleteMapping("/{operIds}")
    @Log(title = "system.title.operlog", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long[] operIds)
    {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    @Log(title = "system.title.operlog", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        operLogService.cleanOperLog();
        return AjaxResult.success();
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysOperLog operLog)
    {
        return toAjax(operLogService.insertOperlog(operLog));
    }


    /**
     * 查询操作日志详细
     * @param operId
     * @return
     */
    @PostMapping("/selectOperLogById")
    public SysOperLog selectOperLogById(Long operId){
        return operLogService.selectOperLogById(operId);
    }

    /**
     * POST请求分页
     * @param operLog
     * @return
     */
    @PostMapping("/postList")
    public TableDataInfo postList(@RequestBody SysOperLog operLog)
    {
        return list(operLog);
    }
}
