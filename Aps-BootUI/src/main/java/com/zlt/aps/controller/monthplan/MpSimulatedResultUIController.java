package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.aps.monthplan.api.service.IMpSimulatedResultRemoteService;
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
 * 文件名称：MpSimulatedResultUIController.java
 * 描    述：S2-1004.实单模拟排产 UI控制层类：....
 *@author yelq
 *@date 2026-01-09
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "S2-1004.实单模拟排产")
@Controller
@RequestMapping("/monthplan/simulatedResult")
public class MpSimulatedResultUIController extends BaseUIController<MpSimulatedResult> {

    @Autowired
    private IMpSimulatedResultRemoteService iMpSimulatedResultService;

    private final String prefix = "monthplan/monthplan/simulatedResult";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:simulatedResult:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/simulatedResult";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mpSimulatedResult", new MpSimulatedResult());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mpSimulatedResult", iMpSimulatedResultService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:simulatedResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpSimulatedResult mpSimulatedResult) {
        return iMpSimulatedResultService.list(mpSimulatedResult);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:simulatedResult:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MpSimulatedResult mpSimulatedResult) {
        if (UserConstants.NOT_UNIQUE.equals(iMpSimulatedResultService.checkUnique(mpSimulatedResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpSimulatedResult.checkUnique"));
        }

        return iMpSimulatedResultService.save(mpSimulatedResult);
    }

    /**
     * 删除S2-1004.实单模拟排产
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:simulatedResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpSimulatedResultService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验S2-1004.实单模拟排产唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MpSimulatedResult mpSimulatedResult) {
        return iMpSimulatedResultService.checkUnique(mpSimulatedResult);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        return I18nUtil.getMessage("ui.data.column.simulatedResult.modelName");
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return I18nUtil.getMessage("ui.data.column.simulatedResult.modelName");
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.simulatedResult.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MpSimulatedResult> util = new ExcelUtil<>(MpSimulatedResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @RequiresPermissions("monthplan:simulatedResult:export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MpSimulatedResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpSimulatedResultService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @RequiresPermissions("monthplan:simulatedResult:import")
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iMpSimulatedResultService.importData(context,false);
        return ajaxResult;
    }

    @PostMapping({"/createVmMonthPrediction"})
    @ResponseBody
    @RequiresPermissions("monthplan:simulatedResult:createVmMonthPrediction")
    @ApiOperation("实单模拟排产")
    public AjaxResult createVmMonthPrediction(MpSimulatedResult mpSimulatedResult)  {
        return iMpSimulatedResultService.createVmMonthPrediction(mpSimulatedResult);
    }
}
