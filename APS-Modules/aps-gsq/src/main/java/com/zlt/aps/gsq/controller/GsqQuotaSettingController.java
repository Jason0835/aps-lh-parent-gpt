package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqQuotaSetting;
import com.zlt.aps.gsq.service.GsqQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈定额设定Controller
 *
 * @author zlt
 * @date 2021-06-29
 */
@RestController
@RequestMapping("/gsq/quota")
public class GsqQuotaSettingController extends BaseController {
    @Autowired
    private GsqQuotaSettingService gsqQuotaSettingService;

    /**
     * 查询钢丝圈定额设定列表
     */
    @ApiOperation("查询钢丝圈定额设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqQuotaSetting gsqQuotaSetting) {
        startPage();
        gsqQuotaSetting.setOrderStr(orderStr());
        List<GsqQuotaSetting> list = gsqQuotaSettingService.selectGsqQuotaSettingList(gsqQuotaSetting);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈定额设定详细信息
     */
    @ApiOperation("获取钢丝圈定额设定详细信息")
    @GetMapping(value = "/{id}")
    public GsqQuotaSetting getInfo(@PathVariable("id") Long id) {
        return gsqQuotaSettingService.selectGsqQuotaSettingById(id);
    }

    /**
     * 新增钢丝圈定额设定
     */
    @Log(title = "ui.data.column.quota.gsqModalName", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈定额设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GsqQuotaSetting gsqQuotaSetting) {
        return toAjax(gsqQuotaSettingService.insertGsqQuotaSetting(gsqQuotaSetting));
    }

    /**
     * 修改钢丝圈定额设定
     */
    @Log(title = "ui.data.column.quota.gsqModalName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢丝圈定额设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GsqQuotaSetting gsqQuotaSetting) {
        return toAjax(gsqQuotaSettingService.updateGsqQuotaSetting(gsqQuotaSetting));
    }

    /**
     * 删除钢丝圈定额设定
     */
    @Log(title = "ui.data.column.quota.gsqModalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈定额设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(gsqQuotaSettingService.deleteGsqQuotaSettingByIds(ids));
    }

    /**
     * 导出钢丝圈定额设定列表
     */
    @Log(title = "ui.data.column.quota.gsqModalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈定额设定列表")
    @PostMapping("/getList")
    public List<GsqQuotaSetting> getList(@RequestBody GsqQuotaSetting gsqQuotaSetting) {
        gsqQuotaSetting.setOrderStr(orderStr());
        return gsqQuotaSettingService.selectGsqQuotaSettingList(gsqQuotaSetting);
    }

    /**
     * 校验钢丝圈定额设定唯一性
     */
    @ApiOperation("校验钢丝圈定额设定唯一性")
    @PostMapping("/checkGsqQuotaSettingUnique")
    public String checkGsqQuotaSettingUnique(@RequestBody GsqQuotaSetting gsqQuotaSetting) {
        return gsqQuotaSettingService.checkGsqQuotaSettingUnique(gsqQuotaSetting);
    }

    @Log(title = "ui.data.column.quota.gsqModalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈定额设定信息")
    public AjaxResult importData(@RequestBody List<GsqQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
