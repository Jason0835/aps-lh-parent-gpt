package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmQuotaSetting;
import com.zlt.aps.tm.service.TmQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 胎面定额设定Controller
 *
 * @author zlt
 * @date 2021-06-28
 */
@RestController
@RequestMapping("/tm/quota")
public class TmQuotaSettingController extends BaseController {
    @Autowired
    private TmQuotaSettingService tmQuotaSettingService;

    /**
     * 查询胎面定额设定列表
     */
    //@PreAuthorize(hasPermi = "tm:quota:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmQuotaSetting tmQuotaSetting) {
        startPage();
        tmQuotaSetting.setOrderStr(orderStr());
        List<TmQuotaSetting> list = tmQuotaSettingService.selectTmQuotaSettingList(tmQuotaSetting);
        return getDataTable(list);
    }

    /**
     * 获取胎面定额设定详细信息
     */
    //@PreAuthorize(hasPermi = "tm:quota:query")
    @GetMapping(value = "/{id}")
    public TmQuotaSetting getInfo(@PathVariable("id") Long id) {
        return tmQuotaSettingService.selectTmQuotaSettingById(id);
    }

    /**
     * 新增胎面定额设定
     */
    @Log(title = "ui.data.column.quota.tmModalName", businessType = BusinessType.INSERT)
    //@PreAuthorize(hasPermi = "tm:quota:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TmQuotaSetting tmQuotaSetting) {
        return toAjax(tmQuotaSettingService.insertTmQuotaSetting(tmQuotaSetting));
    }

    /**
     * 修改胎面定额设定
     */
    @Log(title = "ui.data.column.quota.tmModalName", businessType = BusinessType.UPDATE)
    //@PreAuthorize(hasPermi = "tm:quota:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TmQuotaSetting tmQuotaSetting) {
        return toAjax(tmQuotaSettingService.updateTmQuotaSetting(tmQuotaSetting));
    }

    /**
     * 删除胎面定额设定
     */
    @Log(title = "ui.data.column.quota.tmModalName", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "tm:quota:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tmQuotaSettingService.deleteTmQuotaSettingByIds(ids));
    }

    /**
     * 导出胎面定额设定列表
     */
    @Log(title = "ui.data.column.quota.tmModalName", businessType = BusinessType.EXPORT)
    //@PreAuthorize(hasPermi = "tm:quota:export")
    @PostMapping("/getList")
    public List<TmQuotaSetting> getList(@RequestBody TmQuotaSetting tmQuotaSetting) throws IOException {
        startPage();
        tmQuotaSetting.setOrderStr(orderStr());
        return tmQuotaSettingService.selectTmQuotaSettingList(tmQuotaSetting);
    }

    /**
     * 校验胎面定额唯一性
     */
    @ApiOperation("校验胎面定额唯一性")
    @PostMapping("/checkTmQuotaSettingUnique")
    public String checkTmQuotaSettingUnique(@RequestBody TmQuotaSetting tmQuotaSetting) {
        return tmQuotaSettingService.checkTmQuotaSettingUnique(tmQuotaSetting);
    }

    @Log(title = "ui.data.column.quota.tmModalName", businessType = BusinessType.IMPORT)
    //@PreAuthorize(hasPermi = "tm:quota:import")
    @PostMapping("/importData")
    @ApiOperation("导入胎面定额设定信息")
    public AjaxResult importData(@RequestBody List<TmQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
