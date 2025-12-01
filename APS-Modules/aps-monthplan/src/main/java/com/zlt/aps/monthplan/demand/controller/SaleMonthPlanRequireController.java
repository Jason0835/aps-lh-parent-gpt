package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.demand.service.ISaleMonthPlanRequireService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import com.zlt.core.util.EntityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SaleMonthPlanRequireController.java
 * 描    述：月度生产需求计划 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Api(tags = "月度生产需求计划")
@RestController
@RequestMapping("/saleRequireProductionPlan")
public class SaleMonthPlanRequireController extends BaseController<SaleMonthPlanRequire> {

    private final IExportLogService iExportLogService;

    private final IImportLogService iImportLogService;

    private final IImportErrorLogService iImportErrorLogService;

    private final ISaleMonthPlanRequireService saleMonthPlanRequireService;

    public SaleMonthPlanRequireController(IExportLogService iExportLogService,
                                          IImportLogService iImportLogService,
                                          IImportErrorLogService iImportErrorLogService,
                                          ISaleMonthPlanRequireService saleMonthPlanRequireService) {
        this.iExportLogService = iExportLogService;
        this.iImportLogService = iImportLogService;
        this.iImportErrorLogService = iImportErrorLogService;
        this.saleMonthPlanRequireService = saleMonthPlanRequireService;
    }

    /**
     * 查询月度生产需求计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody SaleMonthPlanRequire queryVO) {
        List<SaleMonthPlanRequire> dataList = getData(queryVO, true);
        return getDataTable(dataList);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.require.modelName", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove")
    @ApiOperation("根据主键集合，批量删除对应数据")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.success();
        }
        List<Long> realDeletedIds = new ArrayList<>();
        ids.stream().forEach(id -> {
            if (null != id) {
                realDeletedIds.add(id);
            }
        });
        if (CollectionUtils.isEmpty(realDeletedIds)) {
            return AjaxResult.success();
        }
        int removeResult = saleMonthPlanRequireService.removeByIds(realDeletedIds);
        return removeResult > 0 ? AjaxResult.success() : AjaxResult.error();
    }


    /**
     * 根据集合导入月度生产需求计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.require.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData/{updateSupport}")
    @ApiOperation("手工动作-导入销售月度生产需求计划数据，用以进行月度计划排产")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<SaleMonthPlanRequire> util = new ExcelUtil(SaleMonthPlanRequire.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<SaleMonthPlanRequire> list = util.importExcel(is);
        AjaxResult ajaxResult = saleMonthPlanRequireService.importData(list, updateSupport, importLog.getId());
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
     * 导出列表
     */
    @Log(title = "月度生产需求计划", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导出月度生产需求计划数据")
    public byte[] exportData(@RequestBody SaleMonthPlanRequire queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<SaleMonthPlanRequire> list = getData(queryVO, false);
        ExcelUtil<SaleMonthPlanRequire> util = new ExcelUtil(SaleMonthPlanRequire.class);
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

    @ApiOperation("查询对应年月+分厂的需求计划版本")
    @PostMapping("/versionList")
    public AjaxResult versionList(@RequestBody SaleMonthPlanRequire saleMonthPlanRequire) {
        return AjaxResult.success(saleMonthPlanRequireService.versionList(saleMonthPlanRequire));
    }

    /**
     * 根据查询条件，获取查询数据-带有分页处理
     *
     * @param queryVO 查询条件
     * @return
     */
    private List<SaleMonthPlanRequire> getData(SaleMonthPlanRequire queryVO, boolean isPage) {
        QueryWrapper queryWrapper = new QueryWrapper();
        //构建查询条件
        this.builderCondition(queryWrapper, queryVO);
        //分页
        PageUtils.startPage(isPage, getOrderBy(queryVO));
        List<SaleMonthPlanRequire> dataList = saleMonthPlanRequireService.getList(queryWrapper);
        if (isPage) {
            clearPage();
        }
        //数据字典转换或是基础数据转换
        try {
            QueryFormulaUtil.execFormula(dataList, getQueryFormulas());
        } catch (QueryExprException var6) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return dataList;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void builderCondition(QueryWrapper<SaleMonthPlanRequire> queryWrapper, SaleMonthPlanRequire queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tradeMode")), "TRADE_MODE", queryVO.getFieldValueByFieldName("tradeMode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImportantCustom")), "IS_IMPORTANT_CUSTOM", queryVO.getFieldValueByFieldName("isImportantCustom"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEnsurePlan")), "IS_ENSURE_PLAN", queryVO.getFieldValueByFieldName("isEnsurePlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEmergency")), "IS_EMERGENCY", queryVO.getFieldValueByFieldName("isEmergency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDebitPlan")), "IS_DEBIT_PLAN", queryVO.getFieldValueByFieldName("isDebitPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isStockUp")), "IS_STOCK_UP", queryVO.getFieldValueByFieldName("isStockUp"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryDateDue")), "DELIVERY_DATE_DUE", queryVO.getFieldValueByFieldName("deliveryDateDue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planQty")), "PLAN_QTY", queryVO.getFieldValueByFieldName("planQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        // queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productName")), "PRODUCT_NAME", queryVO.getFieldValueByFieldName("productName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImport")), "IS_IMPORT", queryVO.getFieldValueByFieldName("isImport"));
        // 是否有交期
        if (YesOrNoEnum.YES.getValue().equals(queryVO.getIsDeliveryDateDue())) {
            queryWrapper.isNotNull("DELIVERY_DATE_DUE");
        } else {
            queryWrapper.isNull("DELIVERY_DATE_DUE");
        }
    }

    private String getOrderBy(SaleMonthPlanRequire queryVO) {
        Map<String, Object> params = queryVO.getParams();
        if (params != null && params.containsKey("orderBy")) {
            String orderByField = (String) params.get("orderBy");
            String dbField = EntityUtil.getColumnNameByFieldName(this.getTClass(), orderByField);
            String isAscStr = (String) params.get("isAsc");
            return dbField + " " + (isAscStr.equals("1") ? "asc" : "desc");
        } else {
            return null;
        }
    }

    private String[] getQueryFormulas() {
        return null;
    }

    /**
     * 根据条件查询统计数据
     */
    @ApiOperation("根据条件查询统计数据")
    @PostMapping("/getSummaryVo")
    public AjaxResult getSummaryVo(@RequestBody SaleMonthPlanRequire queryVO) {
        return AjaxResult.success(saleMonthPlanRequireService.getSummaryVo(queryVO));
    }
}
