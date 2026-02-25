package com.zlt.aps.mp.demand.controller;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.mp.api.domain.entity.ProductStockMonth;
import com.zlt.aps.mp.demand.service.IProductStockMonthService;
import com.zlt.common.controller.BusiController;
import com.zlt.common.utils.ImportExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductStockMonthController.java
 * 描    述：物料月库存信息 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-03-12
 */
@Slf4j
@Api(tags = "物料月库存信息")
@RestController
@RequestMapping("/monthStock")
@RequiredArgsConstructor
public class ProductStockMonthController extends BusiController<ProductStockMonth> {

    private final IProductStockMonthService productStockMonthService;
    private final IImportLogService iImportLogService;
    private final IImportErrorLogService iImportErrorLogService;


    /**
     * 查询物料月库存信息列表
     */
    @RequiresPermissions("monthplan:monthStock:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ProductStockMonth queryVO) {
        try {
            startPage("create_time desc");
            List<ProductStockMonth> monthList = productStockMonthService.selectList(queryVO);
            return getDataTable(monthList);
        } finally {
            PageUtils.clearPage();
        }
    }

    // /**
    //  * 保存
    //  */
    // @Log(title = "ui.data.column.monthStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    // @RequiresPermissions("monthplan:monthStock:save")
    // @ApiOperation("保存")
    // @PostMapping("/save")
    // public AjaxResult save(@RequestBody ProductStockMonth billVO) {
    //     return super.save(billVO);
    // }
    //
    // /**
    //  * 删除
    //  */
    // @Log(title = "ui.data.column.monthStock.modelName", businessType = BusinessType.DELETE)
    // @RequiresPermissions("monthplan:monthStock:remove")
    // @ApiOperation("删除")
    // @DeleteMapping("/remove")
    // @Override
    // public AjaxResult removeByIds(@RequestBody List<Long> ids) {
    //     return super.removeByIds(ids);
    // }
    //
    //
    // /**
    //  * 获取物料月库存信息详细信息
    //  */
    // @RequiresPermissions("monthplan:monthStock:query")
    // @ApiOperation("获取详细信息")
    // @GetMapping(value = "/{billId}")
    // @Override
    // public ProductStockMonth getInfo(@PathVariable("billId") Long billId) {
    //     return super.getInfo(billId);
    // }

    /**
     * 根据集合导入物料月库存信息数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:monthStock:import")
    @Log(title = "ui.data.column.monthStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<ProductStockMonth> util = new ExcelUtil(this.getTClass());
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<ProductStockMonth> list = util.importExcel(is);
        return this.doImportData(list, updateSupport, importLog.getId(), importLog, beginTime);
    }

    protected AjaxResult doImportData(List<ProductStockMonth> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        if (list.size() > 500) {
            // 传递请求头信息（主要是语言包），避免主线程执行后清空request，拷贝一个虚拟的request
            ServletRequestAttributes virtualAttr = RemoteImportExcelUtils.copyRequestHeaderAttribute();
            productStockMonthService.importDataAsync(list, updateSupport, importLogId, importLog, beginTime, virtualAttr);
            return AjaxResult.success(I18nUtil.getMessage("ui.data.column.common.importTimeOut"));
        }

        AjaxResult result = productStockMonthService.doImportData(list, updateSupport, importLogId);
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
    @RequiresPermissions("monthplan:monthStock:export")
    @Log(title = "物料月库存信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody ProductStockMonth queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @Override
    protected List<ProductStockMonth> listExportData(ProductStockMonth queryVO) {
        return productStockMonthService.selectList(queryVO);
    }

    /**
     * 查询物料月库存信息列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/selectList")
    public List<ProductStockMonth> selectList(@RequestBody ProductStockMonth queryVO) {
        return productStockMonthService.selectList(queryVO);
    }
}
