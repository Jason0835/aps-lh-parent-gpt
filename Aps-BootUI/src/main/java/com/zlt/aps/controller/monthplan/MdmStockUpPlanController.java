package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmStockUpPlan;
import com.zlt.aps.monthplan.api.domain.vo.MdmStockUpPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.domain.vo.StockUpPlanExcelVo;
import com.zlt.aps.monthplan.api.service.IMdmStockUpPlanService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmStockUpPlanController.java
 * 描    述：备货计划 UI控制层类：....
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
@Controller
@RequestMapping("/monthplan/mdmStockUpPlan")
public class MdmStockUpPlanController extends BaseUIController<MdmStockUpPlan> {

    @Autowired
    private IMdmStockUpPlanService iMdmStockUpPlanService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmStockUpPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmStockUpPlanVo mdmStockUpPlan) {
        return iMdmStockUpPlanService.list(mdmStockUpPlan);
    }

    /**
     * 生成备货计划
     */
    @ApiOperation("生成备货计划")
    @RequiresPermissions("monthplan:mdmStockUpPlan:createStockUpPlan")
    @PostMapping("/createStockUpPlan")
    @ResponseBody
    public AjaxResult createStockUpPlan(QueryCalcStockingParamVo queryCalcStockingParamVo) {
        //TODO 后续需要前端传值
        queryCalcStockingParamVo.setFactoryCode("116");
        return iMdmStockUpPlanService.createStockUpPlan(queryCalcStockingParamVo);
    }

    /**
     * 修改或新增
     */
    @ResponseBody
    @ApiOperation("修改备货计划的备货量")
    @RequiresPermissions("monthplan:mdmStockUpPlan:edit")
    @PostMapping("/save")
    public AjaxResult save(MdmStockUpPlanVo stockUpPlan) {
        return iMdmStockUpPlanService.saveStockUpPlan(stockUpPlan);
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("monthplan:mdmStockUpPlan:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MONTHPLAN,
                I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        com.zlt.mix.common.core.utils.ExcelUtil<StockUpPlanExcelVo> util = new ExcelUtil<>(StockUpPlanExcelVo.class);
        List<StockUpPlanExcelVo> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iMdmStockUpPlanService.importData(list, true, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    @ResponseBody
    @RequiresPermissions("monthplan:mdmStockUpPlan:export")
    @GetMapping({"/export"})
    @ApiOperation("导出备货计划")
    @Override
    public void export(HttpServletResponse response, MdmStockUpPlan entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmStockUpPlanService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        com.ruoyi.common.core.utils.poi.ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.mdmStockUpPlan.fileName");
    }
}
