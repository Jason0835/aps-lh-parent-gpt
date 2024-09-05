package com.zlt.aps.tc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcQuotaSetting;
import com.zlt.aps.tc.service.TcQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧定额设定Controller
 *
 * @author zlt
 * @date 2021-06-28
 */
@RestController
@RequestMapping("/quota")
public class TcQuotaSettingController extends BaseController {
    @Autowired
    private TcQuotaSettingService tcQuotaSettingService;

    /**
     * 查询胎侧定额设定列表
     */
    //@PreAuthorize(hasPermi = "tc:quota:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcQuotaSetting tcQuotaSetting) {
        startPage();
        tcQuotaSetting.setOrderStr(orderStr());
        List<TcQuotaSetting> list = tcQuotaSettingService.selectTcQuotaSettingList(tcQuotaSetting);
        return getDataTable(list);
    }

    /**
     * 获取胎侧定额设定详细信息
     */
    //@PreAuthorize(hasPermi = "tc:quota:query")
    @GetMapping(value = "/{id}")
    public TcQuotaSetting getInfo(@PathVariable("id") Long id) {
        return tcQuotaSettingService.selectTcQuotaSettingById(id);
    }

    /**
     * 新增胎侧定额设定
     */
    @Log(title = "ui.data.column.quota.tcModalName", businessType = BusinessType.INSERT)
    //@PreAuthorize(hasPermi = "tc:quota:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TcQuotaSetting tcQuotaSetting) {
        return toAjax(tcQuotaSettingService.insertTcQuotaSetting(tcQuotaSetting));
    }

    /**
     * 修改胎侧定额设定
     */
    @Log(title = "ui.data.column.quota.tcModalName", businessType = BusinessType.UPDATE)
    //@PreAuthorize(hasPermi = "tc:quota:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TcQuotaSetting tcQuotaSetting) {
        return toAjax(tcQuotaSettingService.updateTcQuotaSetting(tcQuotaSetting));
    }

    /**
     * 删除胎侧定额设定
     */
    @Log(title = "ui.data.column.quota.tcModalName", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "tc:quota:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tcQuotaSettingService.deleteTcQuotaSettingByIds(ids));
    }

    /**
     * 导出胎侧定额设定列表
     */
    @Log(title = "ui.data.column.quota.tcModalName", businessType = BusinessType.EXPORT)
    //@PreAuthorize(hasPermi = "tc:quota:export")
    @PostMapping("/getList")
    public List<TcQuotaSetting> getList(@RequestBody TcQuotaSetting tcQuotaSetting) {
        startPage();
        tcQuotaSetting.setOrderStr(orderStr());
        return tcQuotaSettingService.selectTcQuotaSettingList(tcQuotaSetting);
    }

    /**
     * 校验胎侧定额设定唯一性
     */
    @ApiOperation("校验胎侧定额设定唯一性")
    @PostMapping("/checkTcQuotaSettingUnique")
    public String checkTcQuotaSettingUnique(@RequestBody TcQuotaSetting tcQuotaSetting) {
        return tcQuotaSettingService.checkTcQuotaSettingUnique(tcQuotaSetting);
    }

    @Log(title = "ui.data.column.quota.tcModalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
