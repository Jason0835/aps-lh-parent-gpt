package com.ruoyi.system.controller;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.api.gateway.system.domain.SysLogininfor;
import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.system.service.ISysLogininforService;

/**
 * 系统访问记录
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/logininfor")
public class SysLogininforController extends BaseController
{
    @Autowired
    private ISysLogininforService logininforService;

    @GetMapping("/list")
    public TableDataInfo list(SysLogininfor logininfor)
    {
        startPage();
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        return getDataTable(list);
    }


    @GetMapping("/totalList")
    public List<SysLogininfor> totalList(SysLogininfor role) {
        List<SysLogininfor> list = logininforService.selectLogininforList(role);
        return list;
    }

    @Log(title = "system.title.logininfo", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysLogininfor logininfor) throws IOException
    {
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<SysLogininfor> util = new ExcelUtil<SysLogininfor>(SysLogininfor.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.logininfo"));
    }

    @Log(title = "system.title.logininfo", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@PathVariable Long[] infoIds)
    {
        return toAjax(logininforService.deleteLogininforByIds(infoIds));
    }

    @Log(title = "system.title.logininfo", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        logininforService.cleanLogininfor();
        return AjaxResult.success();
    }

    @PostMapping
    public AjaxResult add(@RequestParam("username") String username, @RequestParam("status") String status,
            @RequestParam("message") String message)
    {
        String ip = IpUtils.getIpAddr(ServletUtils.getRequest());

        // 封装对象
        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(username);
        logininfor.setIpaddr(ip);
        logininfor.setMsg(message);
        // 日志状态
        if (Constants.LOGIN_SUCCESS.equals(status) || Constants.LOGOUT.equals(status))
        {
            logininfor.setStatus("0");
        }
        else if (Constants.LOGIN_FAIL.equals(status))
        {
            logininfor.setStatus("1");
        }
        return toAjax(logininforService.insertLogininfor(logininfor));
    }

    /**
     * POST请求分页
     * @param logininfor
     * @return
     */
    @PostMapping("/postList")
    public TableDataInfo postList(@RequestBody SysLogininfor logininfor)
    {
        return list(logininfor);
    }
}
