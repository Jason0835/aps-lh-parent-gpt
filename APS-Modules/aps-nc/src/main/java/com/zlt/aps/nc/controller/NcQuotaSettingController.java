package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;
import com.zlt.aps.nc.service.NcQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@RestController
@RequestMapping("/quota")
public class NcQuotaSettingController extends BaseController {
    @Autowired
    private NcQuotaSettingService ncQuotaSettingService;

    /**
     * 查询内衬定额设定列表
     */
    //@PreAuthorize(hasPermi = "nc:quota:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcQuotaSetting ncQuotaSetting) {
        startPage();
        ncQuotaSetting.setOrderStr(orderStr());
        List<NcQuotaSetting> list = ncQuotaSettingService.selectNcQuotaSettingList(ncQuotaSetting);
        return getDataTable(list);
    }

    /**
     * 获取内衬定额设定详细信息
     */
    //@PreAuthorize(hasPermi = "nc:quota:query")
    @GetMapping(value = "/{id}")
    public NcQuotaSetting getInfo(@PathVariable("id") Long id) {
        return ncQuotaSettingService.selectNcQuotaSettingById(id);
    }

    /**
     * 新增内衬定额设定
     */
    //@PreAuthorize(hasPermi = "nc:quota:add")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody NcQuotaSetting ncQuotaSetting) {
        return toAjax(ncQuotaSettingService.insertNcQuotaSetting(ncQuotaSetting));
    }

    /**
     * 修改内衬定额设定
     */
    //@PreAuthorize(hasPermi = "nc:quota:edit")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody NcQuotaSetting ncQuotaSetting) {
        return toAjax(ncQuotaSettingService.updateNcQuotaSetting(ncQuotaSetting));
    }

    /**
     * 删除内衬定额设定
     */
    //@PreAuthorize(hasPermi = "nc:quota:remove")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(ncQuotaSettingService.deleteNcQuotaSettingByIds(ids));
    }

    /**
     * 导出内衬定额设定列表
     */
    //@PreAuthorize(hasPermi = "nc:quota:export")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<NcQuotaSetting> getList(@RequestBody NcQuotaSetting ncQuotaSetting) {
        startPage();
        ncQuotaSetting.setOrderStr(orderStr());
        return ncQuotaSettingService.selectNcQuotaSettingList(ncQuotaSetting);
    }

    /**
     * 校验内衬定额设定唯一性
     */
    @ApiOperation("校验内衬定额设定唯一性")
    @PostMapping("/checkNcQuotaSettingUnique")
    public String checkNcQuotaSettingUnique(@RequestBody NcQuotaSetting ncQuotaSetting) {
        return ncQuotaSettingService.checkNcQuotaSettingUnique(ncQuotaSetting);
    }

    //@PreAuthorize(hasPermi = "nc:quota:import")
    @Log(title = "ui.data.column.quota.ncModalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入内衬定额设定信息")
    public AjaxResult importData(@RequestBody List<NcQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
