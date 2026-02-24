package com.zlt.aps.mp.mdm.controller;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.monthplan.api.domain.entity.EstimateExceedShort;
import com.zlt.aps.mp.mdm.service.IEstimateExceedShortService;
import com.zlt.common.controller.BusiController;
import com.zlt.common.utils.ImportExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：EstimateExceedShortController.java
 * 描    述：预计超欠产 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */
@Slf4j
@Api(tags = "预计超欠产")
@RestController
@RequestMapping("/estimateExceedShort")
@RequiredArgsConstructor
public class EstimateExceedShortController extends BusiController<EstimateExceedShort> {

    private final IEstimateExceedShortService estimateExceedShortService;
    private final IImportLogService iImportLogService;
    private final IImportErrorLogService iImportErrorLogService;

    /**
     * 查询预计超欠产列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody EstimateExceedShort queryVO) {
        startPage("year desc, month desc");
        List<EstimateExceedShort> list = estimateExceedShortService.selectEstimateExceedShortList(queryVO);
        return getDataTable(list);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.estimateExceedShort.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody EstimateExceedShort billVO) {
        return toAjax(estimateExceedShortService.save(billVO));
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.estimateExceedShort.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return toAjax(estimateExceedShortService.removeByIds(ids));
    }


    /**
     * 获取预计超欠产详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    public EstimateExceedShort getInfo(@PathVariable("billId") Long billId) {
        return estimateExceedShortService.getInfo(billId);
    }


    /**
     * 根据集合导入预计超欠产数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.estimateExceedShort.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport,HttpServletRequest request) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<EstimateExceedShort> util = new ExcelUtil(this.getTClass());
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<EstimateExceedShort> list = util.importExcel(is);
        return this.doImportData(list, updateSupport, importLog.getId(), importLog, beginTime);
    }

    public AjaxResult doImportData(List<EstimateExceedShort> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        if (list.size() > 500) {
            // 传递请求头信息（主要是语言包），避免主线程执行后清空request，拷贝一个虚拟的request
            ServletRequestAttributes virtualAttr = RemoteImportExcelUtils.copyRequestHeaderAttribute();
            estimateExceedShortService.importDataAsync(list, updateSupport, importLogId, importLog, beginTime, virtualAttr);
            return AjaxResult.success(I18nUtil.getMessage("ui.data.column.common.importTimeOut"));
        }
        AjaxResult result = estimateExceedShortService.importData(list, updateSupport, importLogId);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(result, this.iImportErrorLogService);
        return result;
    }


    // @Log(title = "ui.data.column.estimateExceedShort.modelName", businessType = BusinessType.IMPORT)
    // @ApiOperation("导入预计超欠产数据")
    // @PostMapping("/importData/{updateSupport}/{importLogId}")
    // public AjaxResult importData(@RequestBody List<EstimateExceedShort> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId) {
    //     if (CollectionUtils.isEmpty(list)) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
    //     }
    //     return estimateExceedShortService.importData(list, updateSupport, importLogId);
    // }

    /**
     * 导出列表
     */
    @Log(title = "ui.data.column.estimateExceedShort.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody EstimateExceedShort queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(queryVO, fileName, response);
    }

    @Override
    public List<EstimateExceedShort> listExportData(EstimateExceedShort queryVO) {
        return this.getList(queryVO);
    }

    /**
     * 校验预计超欠产唯一性
     */
    @ApiOperation("校验预计超欠产唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody EstimateExceedShort tEstimateExceedShort) {
        return estimateExceedShortService.checkUnique(tEstimateExceedShort);
    }

    @ApiOperation("查询列表")
    @PostMapping("/getList")
    public List<EstimateExceedShort> getList(@RequestBody EstimateExceedShort entity) {
        startPage("year desc, month desc");
        return estimateExceedShortService.selectEstimateExceedShortList(entity);
    }

    @ApiOperation("修改预计超欠数")
    @PostMapping("/updateExceedShortQty")
    AjaxResult updateExceedShortQty(@RequestBody EstimateExceedShort estimateExceedShort) {
        return toAjax(estimateExceedShortService.updateExceedShortQty(estimateExceedShort));
    }

}
