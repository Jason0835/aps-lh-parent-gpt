package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.SapImportBadNumber;
import com.zlt.aps.cx.service.SapImportBadNumberService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SAP导入不良数Controller
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
@RestController
@RequestMapping("/badNumber")
public class SapImportBadNumberController extends BaseController
{
    @Autowired
    private SapImportBadNumberService sapImportBadNumberService;

    /**
     * 查询SAP导入不良数列表
     */
    @ApiOperation("查询SAP导入不良数列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody SapImportBadNumber sapImportBadNumber)
    {
        startPage();
        sapImportBadNumber.setOrderStr(orderStr());
        List<SapImportBadNumber> list = sapImportBadNumberService.selectSapImportBadNumberList(sapImportBadNumber);
        return getDataTable(list);
    }

    /**
     * 根据集合导入SAP导入不良数数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.badNumber.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入SAP导入不良数数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<SapImportBadNumber> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return sapImportBadNumberService.importData(list, updateSupport, importLogId);
    }
}
