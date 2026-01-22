package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import com.zlt.aps.monthplan.api.service.IMpProductionPredictionRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

import java.time.YearMonth;
import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpProductionPredictionUIController.java
 * 描    述：S2-1002.未来产量预测 UI控制层类：....
 *@author yelq
 *@date 2025-12-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "S2-1002.未来产量预测")
@Controller
@RequestMapping("/monthplan/productionPrediction")
public class MpProductionPredictionUIController extends BaseUIController<MpProductionPrediction> {

    @Autowired
    private IMpProductionPredictionRemoteService iMpProductionPredictionService;

    private final String prefix = "monthplan/monthplan/productionPrediction";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:productionPrediction:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/productionPrediction";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mpProductionPrediction", new MpProductionPrediction());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mpProductionPrediction", iMpProductionPredictionService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:productionPrediction:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpProductionPrediction mpProductionPrediction) {
        return iMpProductionPredictionService.list(mpProductionPrediction);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:productionPrediction:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MpProductionPrediction mpProductionPrediction) {
        if (UserConstants.NOT_UNIQUE.equals(iMpProductionPredictionService.checkUnique(mpProductionPrediction))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpProductionPrediction.checkUnique"));
        }

        return iMpProductionPredictionService.save(mpProductionPrediction);
    }

    /**
     * 删除S2-1002.未来产量预测
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:productionPrediction:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpProductionPredictionService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验S2-1002.未来产量预测唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MpProductionPrediction mpProductionPrediction) {
        return iMpProductionPredictionService.checkUnique(mpProductionPrediction);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return I18nUtil.getMessage("ui.data.column.productionPrediction.modelName");
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.productionPrediction.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MpProductionPrediction> util = new ExcelUtil<>(MpProductionPrediction.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @RequiresPermissions("monthplan:productionPrediction:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MpProductionPrediction entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpProductionPredictionService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @RequiresPermissions("monthplan:productionPrediction:import")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iMpProductionPredictionService.importData(context,false);
    }

    @ApiOperation("生成订单预测")
    @ResponseBody
    @RequiresPermissions("monthplan:productionPrediction:createMonthPrediction")
    @PostMapping("/createMonthPrediction")
    public AjaxResult createMonthPrediction(MpProductionPrediction createCondition){
        // 获取操作日所在月份
        YearMonth currentMonth = YearMonth.now();
        // T月 = 当月 + 1个月
        YearMonth tMonth = currentMonth.plusMonths(1);
        createCondition.setYear(tMonth.getYear());
        createCondition.setMonth(tMonth.getMonthValue());
        return iMpProductionPredictionService.createMonthPrediction(createCondition);
    }

    /**
     * 查询预测版本号
     */
    @ApiOperation("查询预测版本号")
    @PostMapping("/findPredictionVersion")
    @ResponseBody
    public AjaxResult findPredictionVersion(MpProductionPrediction queryCondition) {
        return iMpProductionPredictionService.findPredictionVersion(queryCondition);
    }
}
