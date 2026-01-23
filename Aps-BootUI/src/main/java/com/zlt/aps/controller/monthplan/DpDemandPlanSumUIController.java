package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.monthplan.api.service.IDpDemandPlanSumRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanSumUIController.java
 * 描    述：需求计划汇总 UI控制层类：....
 *@author yelq
 *@date 2026-01-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "需求计划汇总")
@Controller
@RequestMapping("/monthplan/demandPlanSum")
public class DpDemandPlanSumUIController extends BaseUIController<DpDemandPlanSum> {

    @Autowired
    private IDpDemandPlanSumRemoteService iDpDemandPlanSumService;

    /**
     * 根据条件查询主表数据
     */
    @RequiresPermissions("monthplan:demandPlan:list")
    @ApiOperation("根据条件查询主表数据")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DpDemandPlanSum dpDemandPlanSum) {
        return iDpDemandPlanSumService.list(dpDemandPlanSum);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return I18nUtil.getMessage("ui.data.column.demandPlan.modelName");
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return I18nUtil.getMessage("ui.data.column.demandPlan.modelName");
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.no.export.sheetName");
    }


    @RequiresPermissions("monthplan:demandPlan:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, DpDemandPlanSum entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iDpDemandPlanSumService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:demandPlan:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(DpDemandPlanSum dpDemandPlanSum) {
        return iDpDemandPlanSumService.save(dpDemandPlanSum);
    }

    /**
     * 查询需求计划版本号
     */
    @ApiOperation("查询需求计划版本号")
    @PostMapping("/findMonthPlanVersion")
    @ResponseBody
    public AjaxResult findMonthPlanVersion(DpDemandPlanSum queryCondition) {
        return iDpDemandPlanSumService.findMonthPlanVersion(queryCondition);
    }

}
