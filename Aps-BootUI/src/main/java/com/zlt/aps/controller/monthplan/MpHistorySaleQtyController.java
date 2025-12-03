package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleQty;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcel4MonthVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcelVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.service.IMpHistorySaleQtyService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ImportUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleQtyUIController.java
 * 描    述：历史销售记录 UI控制层类：....
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-13
 */
@Slf4j
@Api(tags = "历史销售记录")
@Controller
@RequestMapping("/monthplan/mpHistorySaleQty")
public class MpHistorySaleQtyController extends BaseUIController<MpHistorySaleQty> {

    @Autowired
    private IMpHistorySaleQtyService iMpHistorySaleQtyService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mpHistorySaleQty:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MpHistorySaleQty mpHistorySaleQty) {
        return iMpHistorySaleQtyService.list(mpHistorySaleQty);
    }

    /**
     * 根据条件查询计算备货数据
     */
    @ApiOperation("根据条件查询计算备货数据")
    @RequiresPermissions("monthplan:mdmStockUpPlan:createStockUpPlan")
    @PostMapping("/queryCalcStocking")
    @ResponseBody
    public TableDataInfo queryCalcStocking(QueryCalcStockingParamVo queryCalcStockingParamVo) {
        //TODO 后续需要前端传值
        queryCalcStockingParamVo.setFactoryCode("116");
        return iMpHistorySaleQtyService.queryCalcStocking(queryCalcStockingParamVo);
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("monthplan:mpHistorySaleQty:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MONTHPLAN,
                I18nUtil.getMessage("ui.data.column.mpHistorySaleQty.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        com.zlt.mix.common.core.utils.ExcelUtil<MpHistorySaleQtyExcelVo> util = new ExcelUtil<>(MpHistorySaleQtyExcelVo.class);
        List<MpHistorySaleQtyExcelVo> list = util.importExcel(in);
        //导入数据
        return iMpHistorySaleQtyService.importData(list, true, importLog.getId());
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.mpHistorySaleQty.modelName");
    }

    @GetMapping({"/importTemplate4Month"})
    @ApiOperation("下载月导入模板")
    @ResponseBody
    public AjaxResult importTemplate4Month(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        com.ruoyi.common.core.utils.poi.ExcelUtil<MpHistorySaleQtyExcel4MonthVo> util = new com.ruoyi.common.core.utils.poi.ExcelUtil<>(MpHistorySaleQtyExcel4MonthVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入-月
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("monthplan:mpHistorySaleQty:importMonth")
    @ApiOperation("excel数据导入-月")
    @PostMapping("/importMonthData")
    @ResponseBody
    public AjaxResult importMonthData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MONTHPLAN,
                I18nUtil.getMessage("ui.data.column.mpHistorySaleQty.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        com.zlt.mix.common.core.utils.ExcelUtil<MpHistorySaleQtyExcel4MonthVo> util = new ExcelUtil<>(MpHistorySaleQtyExcel4MonthVo.class);
        List<MpHistorySaleQtyExcel4MonthVo> list = util.importExcel(in);
        //导入数据
        return iMpHistorySaleQtyService.importMonthData(list, true, importLog.getId());
    }

    @RequiresPermissions("monthplan:mpHistorySaleQty:exportYear")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MpHistorySaleQty entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMpHistorySaleQtyService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        com.ruoyi.common.core.utils.poi.ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}
