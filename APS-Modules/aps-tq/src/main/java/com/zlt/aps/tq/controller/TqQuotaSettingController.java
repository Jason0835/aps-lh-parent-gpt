package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.api.domain.entity.TqQuotaSetting;
import com.zlt.aps.tq.service.TqQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@RestController
@RequestMapping("/tq/quota")
public class TqQuotaSettingController extends BaseController {
    @Autowired
    private TqQuotaSettingService tqQuotaSettingService;

    /**
     * 查询胎圈定额设定列表
     */
    @ApiOperation("查询胎圈定额设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqQuotaSetting tqQuotaSetting) {
        startPage();
        tqQuotaSetting.setOrderStr(orderStr());
        List<TqQuotaSetting> list = tqQuotaSettingService.selectTqQuotaSettingList(tqQuotaSetting);
        return getDataTable(list);
    }

    /**
     * 获取胎圈定额设定详细信息
     */
    @ApiOperation("获取胎圈定额设定详细信息")
    @GetMapping(value = "/{id}")
    public TqQuotaSetting getInfo(@PathVariable("id") Long id) {
        return tqQuotaSettingService.selectTqQuotaSettingById(id);
    }

    /**
     * 新增胎圈定额设定
     */
    @Log(title = "ui.data.column.quota.tqModalName", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎圈定额设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TqQuotaSetting tqQuotaSetting) {
        return toAjax(tqQuotaSettingService.insertTqQuotaSetting(tqQuotaSetting));
    }

    /**
     * 修改胎圈定额设定
     */
    @Log(title = "ui.data.column.quota.tqModalName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎圈定额设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TqQuotaSetting tqQuotaSetting) {
        return toAjax(tqQuotaSettingService.updateTqQuotaSetting(tqQuotaSetting));
    }

    /**
     * 删除胎圈定额设定
     */
    @Log(title = "ui.data.column.quota.tqModalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除胎圈定额设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tqQuotaSettingService.deleteTqQuotaSettingByIds(ids));
    }

    /**
     * 导出胎圈定额设定列表
     */
    @Log(title = "ui.data.column.quota.tqModalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈定额设定列表")
    @PostMapping("/getList")
    public List<TqQuotaSetting> getList(@RequestBody TqQuotaSetting tqQuotaSetting) {
        startPage();
        tqQuotaSetting.setOrderStr(orderStr());
        return tqQuotaSettingService.selectTqQuotaSettingList(tqQuotaSetting);
    }

    /**
     * 校验胎圈定额设定唯一性
     */
    @ApiOperation("校验胎圈定额设定唯一性")
    @PostMapping("/checkTqQuotaSettingUnique")
    public String checkTqQuotaSettingUnique(@RequestBody TqQuotaSetting tqQuotaSetting) {
        return tqQuotaSettingService.checkTqQuotaSettingUnique(tqQuotaSetting);
    }

    @Log(title = "ui.data.column.quota.tqModalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎圈定额设定信息")
    public AjaxResult importData(@RequestBody List<TqQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tqQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
