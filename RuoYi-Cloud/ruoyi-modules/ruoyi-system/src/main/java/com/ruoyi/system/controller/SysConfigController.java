package com.ruoyi.system.controller;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 参数配置 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/config")
public class SysConfigController extends BaseController {
    @Autowired
    private ISysConfigService configService;

    /**
     * 获取参数配置列表
     */
    @PreAuthorize(hasPermi = "system:config:list")
    @GetMapping("/list")
    public TableDataInfo list(SysConfig config) {
        startPage();
        List<SysConfig> list = configService.selectConfigList(config);
        return getDataTable(list);
    }


    @PreAuthorize(hasPermi = "system:config:list")
    @GetMapping("/totalList")
    public List<SysConfig> totalList(SysConfig role)
    {
        List<SysConfig> list = configService.selectConfigList(role);
        return list;
    }

    @Log(title = "system.title.configmanage", businessType = BusinessType.EXPORT)
    @PreAuthorize(hasPermi = "system:config:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysConfig config) throws IOException {
        List<SysConfig> list = configService.selectConfigList(config);
        ExcelUtil<SysConfig> util = new ExcelUtil<SysConfig>(SysConfig.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.configdate"));
    }

    /**
     * 根据参数编号获取详细信息
     */
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable Long configId) {
        return AjaxResult.success(configService.selectConfigById(configId));
    }

    /**
     * 根据参数键名查询参数值
     */
    @GetMapping(value = "/configKey/{configKey}")
    public AjaxResult getConfigKey(@PathVariable String configKey) {
        return AjaxResult.success(configService.selectConfigByKey(configKey));
    }

    /**
     * 新增参数配置
     */
    @PreAuthorize(hasPermi = "system:config:add")
    @Log(title = "system.title.configmanage", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysConfig config) {
        if (UserConstants.NOT_UNIQUE.equals(configService.checkConfigKeyUnique(config))) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.conf.add.exist"), config.getConfigName());
            return AjaxResult.error(errMsg);
        }
        config.setCreateBy(SecurityUtils.getUsername());
        return toAjax(configService.insertConfig(config));
    }

    /**
     * 修改参数配置
     */
    @PreAuthorize(hasPermi = "system:config:edit")
    @Log(title = "system.title.configmanage", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysConfig config) {
        if (UserConstants.NOT_UNIQUE.equals(configService.checkConfigKeyUnique(config))) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.conf.update.exist"), config.getConfigName());
            return AjaxResult.error(errMsg);
        }
        config.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(configService.updateConfig(config));
    }

    /**
     * 删除参数配置
     */
    @PreAuthorize(hasPermi = "system:config:remove")
    @Log(title = "system.title.configmanage", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds) {
        return toAjax(configService.deleteConfigByIds(configIds));
    }

    /**
     * 清空缓存
     */
    @PreAuthorize(hasPermi = "system:config:remove")
    @Log(title = "system.title.configmanage", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clearCache")
    public AjaxResult clearCache() {
        configService.clearCache();
        return AjaxResult.success();
    }

    /**
     * 根据配置文件key读取配置文件
     * @param configKey
     * @return
     */
    @PostMapping("/selectConfigByKey")
    public String selectConfigByKey(String configKey){
        return configService.selectConfigByKey(configKey);
    }

    /**
     * 根据配置ID获取参数配置信息
     * @param configId
     * @return
     */
    @PostMapping("/selectConfigById")
    public SysConfig selectConfigById(Long configId){
       return configService.selectConfigById(configId);
    }

    /**
     * 校验参数键名
     */
    @PostMapping("/checkConfigKeyUnique")
    public String checkConfigKeyUnique(@RequestBody SysConfig config){
        return configService.checkConfigKeyUnique(config);
    }
}
