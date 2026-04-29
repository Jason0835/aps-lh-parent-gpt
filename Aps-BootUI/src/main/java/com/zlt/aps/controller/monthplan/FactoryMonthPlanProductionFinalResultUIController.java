package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.ruoyi.common4ui.exception.BusinessException;
import com.zlt.aps.mp.api.domain.dto.FactoryMonthPlanProductionFinalResultParam;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanMouldDayResultRemoteService;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProductionFinalResultRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import cn.hutool.core.date.DateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultUIController.java
 * 描    述：工厂月度生产计划-最终排产计划定稿 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Slf4j
@Api(tags = "工厂月生产计划-最终排产计划定稿")
@Controller
@RequiredArgsConstructor
@RequestMapping("/monthplan/factoryMonthPlanFinalResult")
public class FactoryMonthPlanProductionFinalResultUIController extends BaseUIController<FactoryMonthPlanProductionFinalResult> {

    private final IFactoryMonthPlanProductionFinalResultRemoteService iFactoryMonthPlanProductionFinalResultService;
    private final IFactoryMonthPlanMouldDayResultRemoteService iFactoryMonthPlanMouldDayResultService;
    
    /**
     * 根据条件查询主表数据
     */
    @RequiresPermissions("monthplan:factoryMonthPlanFinalResult:list")
    @ResponseBody
    @PostMapping("/list")
    @ApiOperation("根据条件查询主表数据")
    public TableDataInfo list(FactoryMonthPlanProductionFinalResultParam param) {
        if (null == param || null == param.getMonth() || null == param.getYear() || StringUtils.isBlank(param.getFactoryCode())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProductionFinalResult condition = new FactoryMonthPlanProductionFinalResult();
        BeanUtils.copyProperties(param, condition);
        return iFactoryMonthPlanProductionFinalResultService.list(condition);
    }

    /**
     * 获取SKU排产明细
     */
    @ResponseBody
    @PostMapping("/listSkuScheduleItems")
    @ApiOperation("获取SKU排产明细")
    public TableDataInfo listSkuScheduleItems(FactoryMonthPlanProductionFinalResultParam param) {
        return iFactoryMonthPlanProductionFinalResultService.listSkuScheduleItems(param);
    }


    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(FactoryMonthPlanProductionFinalResult factoryMonthPlanProductionFinalResult) {
        return iFactoryMonthPlanProductionFinalResultService.save(factoryMonthPlanProductionFinalResult);
    }


    /**
     * 校验工厂月生产计划-最终排产计划定稿唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(FactoryMonthPlanProductionFinalResult factoryMonthPlanProductionFinalResult) {
        return iFactoryMonthPlanProductionFinalResultService.checkUnique(factoryMonthPlanProductionFinalResult);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
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
        return I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<FactoryMonthPlanProductionFinalResult> util = new ExcelUtil<>(FactoryMonthPlanProductionFinalResult.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, FactoryMonthPlanProductionFinalResult entity) throws IOException {
        String fileName = this.getExportTemplateFileName()+ DateUtil.format(LocalDateTime.now(),"yyyyMMdd");
        byte[] excelBytes = iFactoryMonthPlanProductionFinalResultService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @ApiOperation("导出SKU排产明细")
    @RequiresPermissions("monthplan:factoryMonthPlanFinalResult:export")
    @GetMapping({"/exportSkuScheduleItems"})
    @ResponseBody
    public void exportSkuScheduleItems(HttpServletResponse response, FactoryMonthPlanProductionFinalResult entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.modelName") + DateUtil.format(LocalDateTime.now(),"yyyyMMdd");
        FactoryMonthPlanMouldDayResult result = new FactoryMonthPlanMouldDayResult();
        result.setFactoryCode(entity.getFactoryCode());
        result.setProductionVersion(entity.getProductionVersion());
        result.setStructureName(entity.getStructureName());
        result.setYear(entity.getYear());
        result.setMonth(entity.getMonth());
        byte[] excelBytes = iFactoryMonthPlanMouldDayResultService.exportFinalData(result, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 导入
     * 
     * @param file          文件
     * @param updateSupport 是否覆盖
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("monthplan:factoryMonthPlanFinalResult:import")
    @PostMapping({ "/importFinalData" })
    @ResponseBody
    @ApiOperation("数据导入")
    public AjaxResult importFinalData(@RequestPart("file") MultipartFile file,
                                      @RequestParam("updateSupport") boolean updateSupport,
                                      FactoryMonthPlanProductionFinalResult params)
            throws IOException {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        
        // 将params对象序列化为JSON字符串
        String paramsJson = null;
        if (params != null) {
            try {
                paramsJson = com.alibaba.fastjson.JSON.toJSONString(params);
            } catch (Exception e) {
                log.warn("序列化params失败", e);
            }
        }
        
        AjaxResult ajaxResult = iFactoryMonthPlanProductionFinalResultService.importSkuScheduleItems(context,
                updateSupport, paramsJson);
        return ajaxResult;
    }

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    @ResponseBody
    public TableDataInfo getVersionList(FactoryMonthPlanProductionFinalResult queryVO) {
        return iFactoryMonthPlanProductionFinalResultService.getVersionList(queryVO);
    }

}
