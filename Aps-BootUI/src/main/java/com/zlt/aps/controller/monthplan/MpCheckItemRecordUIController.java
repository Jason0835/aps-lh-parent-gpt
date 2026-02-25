package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.zlt.aps.mp.api.domain.entity.MpCheckItemRecord;
import com.zlt.aps.mp.api.service.IMpCheckItemRecordRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.base.BaseException;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpCheckItemRecordUIController.java
 * 描    述：S2-1202 检测项记录 UI控制层类：....
 *@author hsc
 *@date 2026-01-29
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */
@Slf4j
@Api(tags = "S2-1202 检测项记录")
@Controller
@RequestMapping("/monthplan/checkItemRecord")
public class MpCheckItemRecordUIController extends BaseUIController<MpCheckItemRecord> {

    @Autowired
    private IMpCheckItemRecordRemoteService iMpCheckItemRecordService;

    /**
     * 根据条件查询S2-1202 检测项记录列表
     */
    @ApiOperation("根据条件查询S2-1202 检测项记录列表")
//    @RequiresPermissions("monthplan:checkItemRecord:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpCheckItemRecord entity) {
        return iMpCheckItemRecordService.list(entity);
    }

    /**
     * 修改或新增S2-1202 检测项记录
     */
    @ApiOperation("修改或新增S2-1202 检测项记录")
    @RequiresPermissions("monthplan:checkItemRecord:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MpCheckItemRecord mpCheckItemRecord) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iMpCheckItemRecordService.checkMpCheckItemRecordUnique(mpCheckItemRecord))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.mpCheckItemRecord.checkUnique"));
        }
        if (mpCheckItemRecord.getId() != null){
            ajaxResult = iMpCheckItemRecordService.edit(mpCheckItemRecord);
        } else{
            ajaxResult = iMpCheckItemRecordService.add(mpCheckItemRecord);
        }
        return ajaxResult;
    }

    /**
     * 删除S2-1202 检测项记录
     */
    @ApiOperation("删除S2-1202 检测项记录（id不为空）")
    @RequiresPermissions("monthplan:checkItemRecord:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMpCheckItemRecordService.remove(arr);
    }

    /**
     * 校验S2-1202 检测项记录唯一性
     */
    @ApiOperation("校验S2-1202 检测项记录唯一性")
    @PostMapping("/checkMpCheckItemRecordUnique")
    @ResponseBody
    public String checkMpCheckItemRecordUnique(MpCheckItemRecord mpCheckItemRecord) {
        return iMpCheckItemRecordService.checkMpCheckItemRecordUnique(mpCheckItemRecord);
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
    public void export(HttpServletResponse response, MpCheckItemRecord entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpCheckItemRecordService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iMpCheckItemRecordService.importData(context,false);
        return ajaxResult;
    }
}
