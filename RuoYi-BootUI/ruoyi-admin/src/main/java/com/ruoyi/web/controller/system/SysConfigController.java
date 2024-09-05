package com.ruoyi.web.controller.system;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
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
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 参数配置 信息操作处理
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/config")
public class SysConfigController extends BaseController
{
    private String prefix = "system/config";

    @Autowired
    private ISysConfigService configService;

    @RequiresPermissions("system:config:view")
    @GetMapping()
    public String config()
    {
        return prefix + "/config";
    }

    /**
     * 查询参数配置列表
     */
    @RequiresPermissions("system:config:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysConfig config)
    {
        return configService.list(config);
    }


    @ApiOperation("导出参数配置")
    @RequiresPermissions("system:config:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, SysConfig entity) throws IOException {
        List<SysConfig> list = configService.totalList(entity);
        ExcelUtil<SysConfig> util = new ExcelUtil(SysConfig.class);
        util.exportExcel(response, list, I18nUtil.getMessage("ui.config.info.export.sheetName"), I18nUtil.getMessage("ui.config.info.export.sheetName"));
    }

   /* @Log(title = "参数管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:config:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SysConfig config)
    {
        List<SysConfig> list = configService.selectConfigList(config);
        ExcelUtil<SysConfig> util = new ExcelUtil<SysConfig>(SysConfig.class);
        return util.exportExcel(list, "参数数据");
    }*/

    /**
     * 新增参数配置
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存参数配置
     */
    @RequiresPermissions("system:config:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(SysConfig config)
    {
        return configService.add(config);
    }

    /**
     * 修改参数配置
     */
    @GetMapping("/edit/{configId}")
    public String edit(@PathVariable("configId") Long configId, ModelMap mmap)
    {
        mmap.put("config", configService.selectConfigById(configId));
        return prefix + "/edit";
    }

    /**
     * 修改保存参数配置
     */
    @RequiresPermissions("system:config:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SysConfig config)
    {
        return configService.edit(config);
    }

    /**
     * 删除参数配置
     */
    @RequiresPermissions("system:config:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
        return configService.remove(arr);
    }

    /**
     * 清空缓存
     */
    @RequiresPermissions("system:config:remove")
    @GetMapping("/clearCache")
    @ResponseBody
    public AjaxResult clearCache()
    {
        return configService.clearCache();
    }

    /**
     * 校验参数键名
     */
    @PostMapping("/checkConfigKeyUnique")
    @ResponseBody
    public String checkConfigKeyUnique(SysConfig config)
    {
        return configService.checkConfigKeyUnique(config);
    }
}
