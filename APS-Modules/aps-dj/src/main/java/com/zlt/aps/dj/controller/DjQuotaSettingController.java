package com.zlt.aps.dj.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.dj.api.domain.entity.DjQuotaSetting;
import com.zlt.aps.dj.service.DjQuotaSettingService;

import io.swagger.annotations.ApiOperation;

/**
 * 垫胶定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@RestController
@RequestMapping("/dj/quota")
public class DjQuotaSettingController extends BaseController {
    @Autowired
    private DjQuotaSettingService ncQuotaSettingService;

    /**
     * 查询垫胶定额设定列表
     */
    //@PreAuthorize(hasPermi = "dj:quota:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjQuotaSetting ncQuotaSetting) {
        startPage();
        ncQuotaSetting.setOrderStr(orderStr());
        List<DjQuotaSetting> list = ncQuotaSettingService.selectNcQuotaSettingList(ncQuotaSetting);
        return getDataTable(list);
    }

    /**
     * 获取垫胶定额设定详细信息
     */
    //@PreAuthorize(hasPermi = "dj:quota:query")
    @GetMapping(value = "/{id}")
    public DjQuotaSetting getInfo(@PathVariable("id") Long id) {
        return ncQuotaSettingService.selectNcQuotaSettingById(id);
    }

    /**
     * 新增垫胶定额设定
     */
    //@PreAuthorize(hasPermi = "dj:quota:add")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DjQuotaSetting ncQuotaSetting) {
        return toAjax(ncQuotaSettingService.insertNcQuotaSetting(ncQuotaSetting));
    }

    /**
     * 修改垫胶定额设定
     */
    //@PreAuthorize(hasPermi = "dj:quota:edit")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DjQuotaSetting ncQuotaSetting) {
        return toAjax(ncQuotaSettingService.updateNcQuotaSetting(ncQuotaSetting));
    }

    /**
     * 删除垫胶定额设定
     */
    //@PreAuthorize(hasPermi = "dj:quota:remove")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(ncQuotaSettingService.deleteNcQuotaSettingByIds(ids));
    }

    /**
     * 导出垫胶定额设定列表
     */
    //@PreAuthorize(hasPermi = "dj:quota:export")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<DjQuotaSetting> getList(@RequestBody DjQuotaSetting ncQuotaSetting) {
        startPage();
        ncQuotaSetting.setOrderStr(orderStr());
        return ncQuotaSettingService.selectNcQuotaSettingList(ncQuotaSetting);
    }

    /**
     * 校验垫胶定额设定唯一性
     */
    @ApiOperation("校验垫胶定额设定唯一性")
    @PostMapping("/checkNcQuotaSettingUnique")
    public String checkNcQuotaSettingUnique(@RequestBody DjQuotaSetting ncQuotaSetting) {
        return ncQuotaSettingService.checkNcQuotaSettingUnique(ncQuotaSetting);
    }

    //@PreAuthorize(hasPermi = "dj:quota:import")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入垫胶定额设定信息")
    public AjaxResult importData(@RequestBody List<DjQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
