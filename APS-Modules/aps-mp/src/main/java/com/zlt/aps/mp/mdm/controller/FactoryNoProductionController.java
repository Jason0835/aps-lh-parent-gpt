package com.zlt.aps.mp.mdm.controller;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.mp.mdm.service.IFactoryNoProductionService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryNoProductionController.java
 * 描    述：基础数据-分厂不排产 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
@Slf4j
@Api(tags = "基础数据-分厂不排产")
@RestController
@RequestMapping("/factoryNoProduction")
@RequiredArgsConstructor
public class FactoryNoProductionController extends BaseController<FactoryNoProduction> {

    private final IFactoryNoProductionService factoryNoProductionService;

    private final IExportLogService iExportLogService;

    private final IImportLogService iImportLogService;

    private final IImportErrorLogService iImportErrorLogService;

    @Autowired
    private BaseDao baseDao;
    /**
     * 查询分厂不排产品种列表
     */
//    @PreAuthorize(hasPermi = "fac:docFactoryNotProduction:list")
    @ApiOperation("查询分厂不排产品种列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody FactoryNoProduction factoryNoProduction) {
        startPage("fnp.create_time desc");
        List<FactoryNoProduction> list = factoryNoProductionService.selectFactoryNoProductionList(factoryNoProduction);
        return getDataTable(list);
    }


    /**
     * 新增分厂不排产品种
     */
    @Log(title = "ui.data.column.factoryNoProduction.modelName", businessType = BusinessType.INSERT)
//    @PreAuthorize(hasPermi = "fac:docFactoryNotProduction:add")
    @ApiOperation("新增分厂不排产品种")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody FactoryNoProduction factoryNoProduction) {
        String checkUnique = checkFactoryNoProductionUnique(factoryNoProduction);
        if (YesOrNoEnum.YES.getCode().equals(checkUnique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.factoryNoProduction.unique"));
        }
        String productCode = factoryNoProduction.getMaterialCode();
        String factoryCode = factoryNoProduction.getFactoryCode();
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(factoryCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.factoryNoProduction.checkData.empty"));
        }
        return toAjax(factoryNoProductionService.save(factoryNoProduction));
    }

    /**
     * 修改分厂不排产品种
     */
    @Log(title = "ui.data.column.factoryNoProduction.modelName", businessType = BusinessType.UPDATE)
//    @PreAuthorize(hasPermi = "fac:docFactoryNotProduction:edit")
    @ApiOperation("修改分厂不排产品种")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody FactoryNoProduction factoryNoProduction) {
        return toAjax(factoryNoProductionService.updateById(factoryNoProduction));
    }

    /**
     * 删除分厂不排产品种
     */
    @Log(title = "ui.data.column.factoryNoProduction.modelName", businessType = BusinessType.DELETE)
//    @PreAuthorize(hasPermi = "fac:docFactoryNotProduction:remove")
    @ApiOperation("删除分厂不排产品种")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        // Long类型数组转换为list
        List<Long> list = Arrays.stream(ids)
                .collect(Collectors.toList());
        boolean result = false;
        for (Long id : list) {
            result = factoryNoProductionService.removeById(id);
        }
        return toAjax(result);
    }


    /**
     * 导出分厂不排产品种列表
     */
    @Log(title = "ui.data.column.factoryNoProduction.modelName", businessType = BusinessType.EXPORT)
//    @PreAuthorize(hasPermi = "fac:docFactoryNotProduction:export")
    @ApiOperation("导出分厂不排产品种列表")
    @PostMapping("/getList")
    public List<FactoryNoProduction> getList(@RequestBody FactoryNoProduction factoryNoProduction) {
        startPage("fnp.create_time desc");
        return factoryNoProductionService.selectFactoryNoProductionList(factoryNoProduction);
    }

    /**
     * 校验分厂不排产品种唯一性
     */
    @ApiOperation("校验分厂不排产品种唯一性")
    @PostMapping("/checkFactoryNoProductionUnique")
    public String checkFactoryNoProductionUnique(@RequestBody FactoryNoProduction factoryNoProduction) {
        return factoryNoProductionService.checkFactoryNotProductionUnique(factoryNoProduction);
    }

    /**
     * 根据集合导入分厂不排产品种数据
     *
     * @param importContext 集合
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.factoryNoProduction.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入分厂不排产品种数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<FactoryNoProduction> util = new ExcelUtil(FactoryNoProduction.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<FactoryNoProduction> list = util.importExcel(is);
        AjaxResult ajaxResult = factoryNoProductionService.importData(list, updateSupport, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据集合导入分厂不排产品种数据FOR AI
     *
     * @param factoryNoProductionList 集合
     * @return 结果
     */
    @Log(title = "ui.data.column.factoryNoProduction.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入分厂不排产品种数据For AI")
    @PostMapping("/importDataForAI")
    public AjaxResult importDataForAI(@RequestBody List<FactoryNoProduction> factoryNoProductionList) throws Exception {
        if (PubUtil.isEmpty(factoryNoProductionList)){
            return AjaxResult.error("没有数据可导入！");
        }
        baseDao.insertBatch(factoryNoProductionList);
        return AjaxResult.success();
    }
}
