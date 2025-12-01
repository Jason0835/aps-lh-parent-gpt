package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.MesLhScheduleResult;
import com.zlt.aps.lh.api.service.IMesLhScheduleResultRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MesLhScheduleResultUIController.java
 * 描    述：硫化排程下发接口 UI控制层类：....
 *@author zlt
 *@date 2025-03-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "硫化排程下发接口")
@Controller
@RequestMapping("/cxlh/mesLhScheduleResult")
public class MesLhScheduleResultUIController extends BaseUIController<MesLhScheduleResult> {

    @Autowired
    private IMesLhScheduleResultRemoteService iMesLhScheduleResultService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("cxlh:mesLhScheduleResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MesLhScheduleResult mesLhScheduleResult) {
        return iMesLhScheduleResultService.list(mesLhScheduleResult);
    }

    // /**
    //  * 修改或新增
    //  */
    // @ApiOperation("修改或新增")
    // @RequiresPermissions("cxlh:mesLhScheduleResult:edit")
    // @PostMapping("/save")
    // @ResponseBody
    // public AjaxResult save(MesLhScheduleResult mesLhScheduleResult) {
    //     if (UserConstants.NOT_UNIQUE.equals(iMesLhScheduleResultService.checkUnique(mesLhScheduleResult))) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mesLhScheduleResult.checkUnique"));
    //     }
    //
    //     return iMesLhScheduleResultService.save(mesLhScheduleResult);
    // }
    //
    // /**
    //  * 删除硫化排程下发接口
    //  */
    // @ApiOperation("删除,id不为空")
    // @RequiresPermissions("cxlh:mesLhScheduleResult:remove")
    // @PostMapping("/remove")
    // @ResponseBody
    // public AjaxResult remove(String ids) {
    //     Long[] arr = Convert.toLongArray(ids);
    //     return iMesLhScheduleResultService.removeByIds(Arrays.asList(arr));
    // }
    //
    // /**
    //  * 校验硫化排程下发接口唯一性
    //  */
    // @ApiOperation("校验唯一性")
    // @PostMapping("/checkUnique")
    // @ResponseBody
    // public String checkUnique(MesLhScheduleResult mesLhScheduleResult) {
    //     return iMesLhScheduleResultService.checkUnique(mesLhScheduleResult);
    // }
    //
    // /**
    //  * 导出模板文件的文件名，派生类重写名称。
    //  * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
    //  * @return
    //  */
    // @Override
    // public String getExportTemplateFileName(){
    //     return this.getFunctionName();
    // }
    //
    //
    // /**
    //  * 继承时重写方法。
    //  *
    //  * @return
    //  */
    // @Override
    // public String getProcedureCode() {
    //     return "0";
    // }
    //
    // /**
    //  * 继承时重写方法。
    //  *
    //  * @return
    //  */
    // @Override
    // public String getFunctionName() {
    //     return I18nUtil.getMessage("ui.no.export.sheetName");
    // }
    //
    // /**
    //  * 重写导入模板的生成逻辑
    //  */
    // @ApiOperation("下载导入模板")
    // @Override
    // public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
    //     String fileName = this.getExportTemplateFileName();
    //     ExcelUtil<MesLhScheduleResult> util = new ExcelUtil<>(MesLhScheduleResult.class);
    //     util.exportExcel(response, null, fileName, fileName);
    //     return AjaxResult.success();
    // }
    //
    // @ApiOperation("数据导出")
    // @GetMapping({"/export"})
    // @ResponseBody
    // @Override
    // public void export(HttpServletResponse response, MesLhScheduleResult entity) throws IOException {
    //     String fileName = this.getExportTemplateFileName();
    //     byte[] excelBytes = iMesLhScheduleResultService.exportData(entity,fileName);
    //     ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
    //     ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
    //     IOUtils.copy(in, response.getOutputStream());
    //     response.flushBuffer();
    // }
    //
    // @PostMapping({"/importData"})
    // @ResponseBody
    // @ApiOperation("数据导入")
    // @Override
    // public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
    //     byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
    //
    //     ImportContext context = new ImportContext();
    //     context.setImportFilePath(this.importFilePath);
    //     context.setFunctionName(this.getFunctionName());
    //     context.setProcedureCode(this.getProcedureCode());
    //     context.setOriFileName(file.getOriginalFilename());
    //     context.setFileBytes(data);
    //     AjaxResult ajaxResult = iMesLhScheduleResultService.importData(context,false);
    //     return ajaxResult;
    // }
}
