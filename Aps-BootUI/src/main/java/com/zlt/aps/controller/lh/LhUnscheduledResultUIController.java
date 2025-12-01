package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.service.ILhUnscheduledResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhUnscheduledResultUIController.java
 * 描    述：硫化未排结果 UI控制层类：....
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Api(tags = "硫化未排结果")
@Controller
@RequestMapping("/lh/lhUnscheduledResult")
public class LhUnscheduledResultUIController extends BaseUIController<LhUnscheduledResult> {

    @Autowired
    private ILhUnscheduledResultRemoteService iLhUnscheduledResultService;

    private final String prefix = "aps/lh/lhUnscheduledResult";


    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:lhUnscheduledResult:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhUnscheduledResult lhUnscheduledResult) {
        return iLhUnscheduledResultService.list(lhUnscheduledResult);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("lh:lhUnscheduledResult:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(LhUnscheduledResult lhUnscheduledResult) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iLhUnscheduledResultService.checkUnique(lhUnscheduledResult))) {
            return ajaxResult.error(I18nUtil.getMessage("ui.data.column.lhUnscheduledResult.checkUnique"));
        }

        return iLhUnscheduledResultService.save(lhUnscheduledResult);
    }

    /**
     * 删除硫化未排结果
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("lh:lhUnscheduledResult:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhUnscheduledResultService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验硫化未排结果唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(LhUnscheduledResult lhUnscheduledResult) {
        return iLhUnscheduledResultService.checkUnique(lhUnscheduledResult);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     * @return
     */
    @Override
    public String getExportTemplateFileName(){
        throw new ServiceException("没有定义导出模板的文件名");
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
    public void export(HttpServletResponse response, LhUnscheduledResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhUnscheduledResultService.exportData(entity,fileName);
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
        AjaxResult ajaxResult = iLhUnscheduledResultService.importData(context,false);
        return ajaxResult;
    }
}
