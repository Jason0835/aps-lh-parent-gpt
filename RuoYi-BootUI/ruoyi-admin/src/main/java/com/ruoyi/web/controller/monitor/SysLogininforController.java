package com.ruoyi.web.controller.monitor;


import com.ruoyi.api.gateway.system.domain.SysLogininfor;
import com.ruoyi.api.gateway.system.service.ISysLogininforService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common.text.Convert;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 系统访问记录
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/monitor/logininfor")
public class SysLogininforController extends BaseController
{
    private String prefix = "monitor/logininfor";

    @Autowired
    private ISysLogininforService logininforService;


    @RequiresPermissions("monitor:logininfor:view")
    @GetMapping()
    public String logininfor()
    {
        return prefix + "/logininfor";
    }

    @RequiresPermissions("monitor:logininfor:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysLogininfor logininfor)
    {
        return logininforService.list(logininfor);
    }

    @ApiOperation("导出登录日志")
    @RequiresPermissions("monitor:logininfor:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, SysLogininfor entity) throws IOException {
        List<SysLogininfor> list = logininforService.totalList(entity);
        ExcelUtil<SysLogininfor> util = new ExcelUtil(SysLogininfor.class);
        util.exportExcel(response, list, I18nUtil.getMessage("ui.logininfor.info.export.sheetName"), I18nUtil.getMessage("ui.logininfor.info.export.sheetName"));
    }

   /* 导出功能后续会调整，调整后重新处理
   @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @RequiresPermissions("monitor:logininfor:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SysLogininfor logininfor)
    {
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<SysLogininfor> util = new ExcelUtil<SysLogininfor>(SysLogininfor.class);
        return util.exportExcel(list, "登录日志");
    }*/

    @RequiresPermissions("monitor:logininfor:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
        return logininforService.remove(arr);
    }
    
    @RequiresPermissions("monitor:logininfor:remove")
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean()
    {
        return logininforService.clean();
    }

    /* Cloud版本没有解锁功能，先注释
    @RequiresPermissions("monitor:logininfor:unlock")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @PostMapping("/unlock")
    @ResponseBody
    public AjaxResult unlock(String loginName)
    {
        passwordService.clearLoginRecordCache(loginName);
        return success();
    }*/
}
