package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.base.BaseException;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.service.IMpDemandPlanRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpDemandPlanUIController.java
 * 描    述：需求计划 UI控制层类：....
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Api(tags = "需求计划")
@Controller
@RequestMapping("/monthplan/demandPlan")
public class MpDemandPlanUIController extends BaseUIController<DpDemandPlan> {

    @Autowired
    private IMpDemandPlanRemoteService iMpDemandPlanService;

    private final String prefix = "monthplan/monthplan/demandPlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:demandPlan:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/demandPlan";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mpDemandPlan", new DpDemandPlan());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mpDemandPlan", iMpDemandPlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询需求计划列表
     */
    @ApiOperation("根据条件查询需求计划列表")
    @RequiresPermissions("monthplan:demandPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DpDemandPlan entity) {
        return iMpDemandPlanService.list(entity);
    }

    /**
     * 修改或新增需求计划
     */
    @ApiOperation("修改或新增需求计划")
    @RequiresPermissions("monthplan:demandPlan:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(DpDemandPlan mpDemandPlan) {
        AjaxResult ajaxResult;
        if (UserConstants.NOT_UNIQUE.equals(iMpDemandPlanService.checkMpDemandPlanUnique(mpDemandPlan))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpDemandPlan.checkUnique"));
        }
        if (mpDemandPlan.getId() != null){
            ajaxResult = iMpDemandPlanService.edit(mpDemandPlan);
        } else{
            ajaxResult = iMpDemandPlanService.add(mpDemandPlan);
        }
        return ajaxResult;
    }

    /**
     * 删除需求计划
     */
    @ApiOperation("删除需求计划（id不为空）")
    @RequiresPermissions("monthplan:demandPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpDemandPlanService.remove(arr);
    }

    /**
     * 校验需求计划唯一性
     */
    @ApiOperation("校验需求计划唯一性")
    @PostMapping("/checkMpDemandPlanUnique")
    @ResponseBody
    public String checkMpDemandPlanUnique(DpDemandPlan mpDemandPlan) {
        return iMpDemandPlanService.checkMpDemandPlanUnique(mpDemandPlan);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        throw new BaseException("没有定义导出模板的文件名");
    }


    /**
 * 继承时重写方法。
 *
 * @return
 */
    @Override
    public String getProcedureCode() {
        return "0";
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

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, DpDemandPlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpDemandPlanService.exportData(entity,fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
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
        AjaxResult ajaxResult = iMpDemandPlanService.importData(context,false);
        return ajaxResult;
    }
}
