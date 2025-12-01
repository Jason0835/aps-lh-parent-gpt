package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.api.domain.vo.MdmStockUpPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.domain.vo.StockUpPlanExcelVo;
import com.zlt.aps.monthplan.factory.service.IMdmStockUpPlanService;
import com.zlt.common.utils.ExcelReadUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStockUpPlanController.java
 * 描    述：备货计划 控制层类：....
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-18
 */
@Slf4j
@Api(tags = "备货计划")
@RestController
@RequestMapping("/mdmStockUpPlan")
public class MdmStockUpPlanController extends BaseController {

    @Autowired
    private IMdmStockUpPlanService mdmStockUpPlanService;
    @Autowired
    private IExportLogService iExportLogService;


    /**
     * 查询备货计划列表
     */
    @RequiresPermissions("monthplan:mdmStockUpPlan:list")
    @ApiOperation("查询备货计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmStockUpPlanVo mdmStockUpPlan) {
        startPage("create_time desc");
        List<MdmStockUpPlanVo> list = mdmStockUpPlanService.selectMdmStockUpPlanList(mdmStockUpPlan);
        return getDataTable(list);
    }

    /**
     * 生成备货计划
     */
    @RequiresPermissions("monthplan:mdmStockUpPlan:createStockUpPlan")
    @ApiOperation("生成备货计划")
    @PostMapping("/createStockUpPlan")
    AjaxResult createStockUpPlan(@RequestBody QueryCalcStockingParamVo queryCalcStockingParamVo) {
        return mdmStockUpPlanService.createStockUpPlan(queryCalcStockingParamVo);
    }


    /**
     * 修改保存备货计划
     */
    @RequiresPermissions("monthplan:mdmStockUpPlan:edit")
    @ApiOperation("修改保存备货计划")
    @PostMapping("/saveStockUpPlan")
    AjaxResult saveStockUpPlan(@RequestBody MdmStockUpPlanVo saveStockUpPlan) {
        return mdmStockUpPlanService.saveStockUpPlan(saveStockUpPlan);
    }

    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<StockUpPlanExcelVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mdmStockUpPlanService.importData(list, updateSupport, importLogId);
    }

    @ApiOperation("导出列表")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MdmStockUpPlanVo entity, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<MdmStockUpPlanVo> list = mdmStockUpPlanService.selectMdmStockUpPlanList(entity);
        ExcelUtil<MdmStockUpPlanVo> util = new ExcelUtil(MdmStockUpPlanVo.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(entity.toString());
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
