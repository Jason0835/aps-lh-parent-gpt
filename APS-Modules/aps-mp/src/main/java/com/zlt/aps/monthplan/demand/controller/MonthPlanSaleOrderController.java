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
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanSaleOrder;
import com.zlt.aps.monthplan.demand.service.IMonthPlanSaleOrderService;
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
import org.springframework.beans.factory.annotation.Autowired;
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
 * 文件名称：MonthPlanSaleOrderController.java
 * 描    述：月度销售计划订单 控制层类
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Api(tags = "月度销售计划订单")
@RestController
@RequestMapping("/monthSaleOrderPlan")
public class MonthPlanSaleOrderController extends BaseController<MonthPlanSaleOrder> {

    @Autowired
    private IMonthPlanSaleOrderService monthPlanSaleOrderService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 查询月度销售计划订单列表
     */
    @PostMapping("/list")
    @ApiOperation("查询月度销售计划订单列表")
    public TableDataInfo list(@RequestBody MonthPlanSaleOrder queryCondition) {
        List<MonthPlanSaleOrder> dataList = getData(queryCondition, true);
        return getDataTable(dataList);
    }

    /**
     * 新增月度销售计划订单
     */
    @Log(title = "ui.data.column.sale.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增月度销售计划订单")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MonthPlanSaleOrder monthPlanSaleOrder) {
        return toAjax(monthPlanSaleOrderService.insertMonthPlanSaleOrder(monthPlanSaleOrder));
    }

    /**
     * 修改月度销售计划订单
     */
    @Log(title = "ui.data.column.sale.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改月度销售计划订单")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MonthPlanSaleOrder monthPlanSaleOrder) {
        return toAjax(monthPlanSaleOrderService.updateMonthPlanSaleOrder(monthPlanSaleOrder));
    }

    /**
     * 校验月度销售计划订单唯一性
     */
    @ApiOperation("校验月度销售计划订单唯一性")
    @PostMapping("/checkMonthPlanSaleOrderUnique")
    public String checkMonthPlanSaleOrderUnique(@RequestBody MonthPlanSaleOrder monthPlanSaleOrder) {
        return monthPlanSaleOrderService.checkMonthPlanSaleOrderUnique(monthPlanSaleOrder);
    }

    /**
     * 删除
     */
    @DeleteMapping("/remove")
    @ApiOperation("删除月度销售计划订单数据")
    @Log(title = "ui.data.column.sale.modelName", businessType = BusinessType.DELETE)
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
        int removeResult = monthPlanSaleOrderService.removeByIds(realDeletedIds);
        return removeResult > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 根据集合导入月度销售计划订单数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @PostMapping("/importData")
    @ApiOperation("导入月度销售计划订单原始数据")
    @Log(title = "ui.data.column.sale.modelName", businessType = BusinessType.IMPORT)
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MonthPlanSaleOrder> util = new ExcelUtil(MonthPlanSaleOrder.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<MonthPlanSaleOrder> list = util.importExcel(is);
        AjaxResult ajaxResult = monthPlanSaleOrderService.importData(list, updateSupport, importLog.getId());
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
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导入月度销售计划订单-原始订单数据")
    @Log(title = "月度销售计划订单", businessType = BusinessType.EXPORT)
    public byte[] exportData(@RequestBody MonthPlanSaleOrder queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<MonthPlanSaleOrder> list = getData(queryVO, false);
        ExcelUtil<MonthPlanSaleOrder> util = new ExcelUtil(MonthPlanSaleOrder.class);
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

    /**
     * 根据查询条件，获取查询数据-带有分页处理
     *
     * @param queryVO 查询条件
     * @return
     */
    private List<MonthPlanSaleOrder> getData(MonthPlanSaleOrder queryVO, boolean isPage) {
        QueryWrapper queryWrapper = new QueryWrapper();
        //构建查询条件
        this.builderCondition(queryWrapper, queryVO);
        //分页
        PageUtils.startPage(isPage, getOrderBy(queryVO));
        List<MonthPlanSaleOrder> dataList = monthPlanSaleOrderService.getList(queryWrapper);
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
     * 构建查询条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void builderCondition(QueryWrapper<MonthPlanSaleOrder> queryWrapper, MonthPlanSaleOrder queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customCode")), "CUSTOM_CODE", queryVO.getFieldValueByFieldName("customCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customName")), "CUSTOM_NAME", queryVO.getFieldValueByFieldName("customName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("channel")), "CHANNEL", queryVO.getFieldValueByFieldName("channel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireType")), "TIRE_TYPE", queryVO.getFieldValueByFieldName("tireType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salePerson")), "SALE_PERSON", queryVO.getFieldValueByFieldName("salePerson"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImportantCustom")), "IS_IMPORTANT_CUSTOM", queryVO.getFieldValueByFieldName("isImportantCustom"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEnsurePlan")), "IS_ENSURE_PLAN", queryVO.getFieldValueByFieldName("isEnsurePlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEmergency")), "IS_EMERGENCY", queryVO.getFieldValueByFieldName("isEmergency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("contractNo")), "CONTRACT_NO", queryVO.getFieldValueByFieldName("contractNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nation")), "NATION", queryVO.getFieldValueByFieldName("nation"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("submissionDate")), "SUBMISSION_DATE", queryVO.getFieldValueByFieldName("submissionDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryDateDue")), "DELIVERY_DATE_DUE", queryVO.getFieldValueByFieldName("deliveryDateDue"));
    }

    /**
     * 获取排序
     *
     * @param queryVO
     * @return
     */
    private String getOrderBy(MonthPlanSaleOrder queryVO) {
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

    /**
     * 数据字典，基础数据转换等配置
     *
     * @return
     */
    private String[] getQueryFormulas() {
        return null;
    }

}
