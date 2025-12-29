package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionCalendar;
import com.zlt.aps.monthplan.api.service.IMdmProductionCalendarRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductionCalendarUIController.java
 * 描    述：生产日历 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Api(tags = "生产日历")
@Controller
// @RequestMapping("/maindata/productionCalendar")
@RequestMapping("/lean/productioncalendar")
public class MdmProductionCalendarUIController extends BaseUIController {

    @Autowired
    private IMdmProductionCalendarRemoteService iMdmProductionCalendarService;

    /**
     * 根据条件查询生产日历列表
     */
    @ApiOperation("根据条件查询生产日历列表")
    // @RequiresPermissions("maindata:productionCalendar:list")
    @RequiresPermissions("lean:productioncalendar:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmProductionCalendar entity) {
        return iMdmProductionCalendarService.list(entity);
    }

    /**
     * 修改或新增生产日历
     */
    @ApiOperation("修改或新增生产日历")
    // @RequiresPermissions("maindata:productionCalendar:edit")
    @RequiresPermissions(value = {"lean:productioncalendar:edit", "lean:productioncalendar:add"}, logical = Logical.OR)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmProductionCalendar mdmProductionCalendar) {
        if (mdmProductionCalendar.getBeginDate() != null && mdmProductionCalendar.getEndDate() != null
                && mdmProductionCalendar.getEndDate().before(mdmProductionCalendar.getBeginDate())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmProductionCalendar.dateCheck"));
        }
        if (UserConstants.NOT_UNIQUE.equals(iMdmProductionCalendarService.checkMdmProductionCalendarUnique(mdmProductionCalendar))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmProductionCalendar.checkUnique"));
        }
        if (mdmProductionCalendar.getId() != null) {
            return iMdmProductionCalendarService.edit(mdmProductionCalendar);
        } else {
            return iMdmProductionCalendarService.add(mdmProductionCalendar);
        }
    }

    /**
     * 删除生产日历
     */
    @ApiOperation("删除生产日历（id不为空）")
    // @RequiresPermissions("maindata:productionCalendar:remove")
    @RequiresPermissions("lean:productioncalendar:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmProductionCalendarService.remove(arr);
    }

    /**
     * 校验生产日历唯一性
     */
    @ApiOperation("校验生产日历唯一性")
    @PostMapping("/checkMdmProductionCalendarUnique")
    @ResponseBody
    public String checkMdmProductionCalendarUnique(MdmProductionCalendar mdmProductionCalendar) {
        return iMdmProductionCalendarService.checkMdmProductionCalendarUnique(mdmProductionCalendar);
    }

    // /**
    //  * 导出模板文件的文件名，派生类重写名称。
    //  * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
    //  *
    //  * @return
    //  */
    // @Override
    // public String getExportTemplateFileName() {
    //     throw new BaseException("没有定义导出模板的文件名");
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

    // @ApiOperation("数据导出")
    // @GetMapping({"/export"})
    // @ResponseBody
    // @Override
    // public void export(HttpServletResponse response, MdmProductionCalendar entity) throws IOException {
    //     String fileName = this.getExportTemplateFileName();
    //     byte[] excelBytes = iMdmProductionCalendarService.exportData(entity, fileName);
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
    //     AjaxResult ajaxResult = iMdmProductionCalendarService.importData(context, updateSupport);
    //     return ajaxResult;
    // }
}
