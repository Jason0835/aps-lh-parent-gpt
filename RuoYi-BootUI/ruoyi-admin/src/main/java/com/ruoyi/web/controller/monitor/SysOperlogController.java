package com.ruoyi.web.controller.monitor;

import com.ruoyi.api.gateway.system.domain.SysOperLog;
import com.ruoyi.api.gateway.system.service.ISysOperLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common.text.Convert;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


/**
 * 操作日志记录
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/monitor/operlog")
public class SysOperlogController extends BaseController
{
    private String prefix = "monitor/operlog";

    @Autowired
    private ISysOperLogService operLogService;

    @RequiresPermissions("monitor:operlog:view")
    @GetMapping()
    public String operlog()
    {
        return prefix + "/operlog";
    }

    @RequiresPermissions("monitor:operlog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysOperLog operLog)
    {
        return operLogService.list(operLog);
    }


    @ApiOperation("导出操作日志")
    @RequiresPermissions("monitor:operlog:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, SysOperLog entity) throws IOException {
        List<SysOperLog> list = operLogService.totalList(entity);
        ExcelUtil<SysOperLog> util = new ExcelUtil(SysOperLog.class);
        util.exportExcel(response, list, I18nUtil.getMessage("ui.operlog.info.export.sheetName"), I18nUtil.getMessage("ui.operlog.info.export.sheetName"));
    }

    /*@Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @RequiresPermissions("monitor:operlog:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SysOperLog operLog)
    {
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
        return util.exportExcel(list, "操作日志");
    }*/

    @RequiresPermissions("monitor:operlog:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
        return operLogService.remove(arr);
    }

    @RequiresPermissions("monitor:operlog:detail")
    @GetMapping("/detail/{operId}")
    public String detail(@PathVariable("operId") Long operId, ModelMap mmap)
    {
        SysOperLog sysOperLog=operLogService.selectOperLogById(operId);
        sysOperLog.setOperLocation(StringUtils.isBlank(sysOperLog.getOperLocation())?"":sysOperLog.getOperLocation());
        mmap.put("operLog", sysOperLog);
        return prefix + "/detail";
    }
    
    @RequiresPermissions("monitor:operlog:remove")
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean()
    {
        return operLogService.clean();
    }
}
