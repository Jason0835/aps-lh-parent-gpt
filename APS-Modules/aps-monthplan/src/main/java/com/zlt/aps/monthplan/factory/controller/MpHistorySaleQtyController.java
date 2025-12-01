package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleQty;
import com.zlt.aps.monthplan.api.domain.vo.CalcStockingResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcel4MonthVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcelVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.common.utils.RemoteImportExcelUtils;
import com.zlt.aps.monthplan.factory.service.IMpHistorySaleQtyService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleQtyController.java
 * 描    述：历史销售记录 控制层类：....
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-13
 */
@Slf4j
@Api(tags = "历史销售记录")
@RestController
@RequestMapping("/mpHistorySaleQty")
@RequiredArgsConstructor
public class MpHistorySaleQtyController extends BaseController {

    private final IMpHistorySaleQtyService mpHistorySaleQtyService;

    private final IImportLogService iImportLogService;

    private final IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IExportLogService iExportLogService;

    private final IFactoryParamService factoryParamService;
    /**
     * 查询历史销售记录列表
     */
    @RequiresPermissions("monthplan:mpHistorySaleQty:list")
    @ApiOperation("查询历史销售记录列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MpHistorySaleQty mpHistorySaleQty) {
        startPage("SALE_QTY desc");
        List<MpHistorySaleQty> list = mpHistorySaleQtyService.selectMpHistorySaleQtyList(mpHistorySaleQty);
        return getDataTable(list);
    }

    /**
     * 查询历史销售记录列表
     */
    @RequiresPermissions("monthplan:mdmStockUpPlan:createStockUpPlan")
    @ApiOperation("查询计算备货数据")
    @PostMapping("/queryCalcStocking")
    public TableDataInfo queryCalcStocking(@RequestBody QueryCalcStockingParamVo queryCalcStockingParamVo) {
        //20250521 ZLT 会出现可能需要跨月提前值 ，因为近一个月月数据没有或是没有意义
        Integer lastMonth = factoryParamService.getStockUpLastMonth(queryCalcStockingParamVo.getFactoryCode());
        startPage("d.factory_code,d.product_code,d.location_type");
        List<CalcStockingResultVo> list = mpHistorySaleQtyService.selectCalcStocking(queryCalcStockingParamVo, lastMonth);
        return getDataTable(list);
    }

    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<MpHistorySaleQtyExcelVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = new ImportLog();
        importLog.setId(importLogId);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        if (list.size() > 500) {
            // 传递请求头信息（主要是语言包），避免主线程执行后清空request，拷贝一个虚拟的request
            ServletRequestAttributes virtualAttr = RemoteImportExcelUtils.copyRequestHeaderAttribute();
            mpHistorySaleQtyService.importDataAsync(list, updateSupport, importLog.getId(), importLog, beginTime, virtualAttr);
            return AjaxResult.success(I18nUtil.getMessage("ui.mpHistorySaleQty.import.warn"));
        }

        AjaxResult result = AjaxResult.success(mpHistorySaleQtyService.importData(list, updateSupport, importLog.getId()));
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(result, this.iImportErrorLogService);
        return result;
    }

    @PostMapping("/importMonthData")
    public AjaxResult importMonthData(@RequestBody List<MpHistorySaleQtyExcel4MonthVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = new ImportLog();
        importLog.setId(importLogId);
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        if (list.size() > 500) {
            // 传递请求头信息（主要是语言包），避免主线程执行后清空request，拷贝一个虚拟的request
            ServletRequestAttributes virtualAttr = RemoteImportExcelUtils.copyRequestHeaderAttribute();
            mpHistorySaleQtyService.importMonthDataAsync(list, updateSupport, importLog.getId(), importLog, beginTime, virtualAttr);
            return AjaxResult.success(I18nUtil.getMessage("ui.mpHistorySaleQty.import.warn"));
        }

        AjaxResult result = mpHistorySaleQtyService.importMonthData(list, updateSupport, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(result, this.iImportErrorLogService);
        return result;
    }

    /**
     * 导出列表
     */
    @Log(title = "历史销售记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MpHistorySaleQty queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<MpHistorySaleQtyExcelVo> list = mpHistorySaleQtyService.selectMpHistorySaleQtyList4ExportData(queryVO);
        ExcelUtil<MpHistorySaleQtyExcelVo> util = new ExcelUtil<>(MpHistorySaleQtyExcelVo.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }
}
