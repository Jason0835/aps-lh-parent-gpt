package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanAdjustPlanVo;
import com.zlt.aps.monthplan.api.service.IFactoryMonthPlanAdjustRemoteService;
import com.zlt.aps.monthplan.api.service.IFactoryMonthPlanProdFinalRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 分厂月计划调整业务
 *
 * @author ZLT
 * @date 20250320
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/factory/monthPlanAdjust")
@Api(tags = "分厂月生产计划调整业务前端接口服务类-->ZLT")
public class FactoryMonthPlanAdjustUiController extends BaseUIController<FactoryMonthPlanProdFinal> {

    private final IFactoryMonthPlanAdjustRemoteService factoryMonthPlanAdjustRemoteService;

    private final IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalRemoteService;

    /**
     * 根据分厂、年份、月份获取计划调整控制信息
     */
    @ResponseBody
    @PostMapping("/getAdjustControlInfo")
    @ApiOperation("根据分厂、年份、月份获取计划调整控制信息")
    public AjaxResult getMonthPlanAdjustControlInfo(@RequestBody FactoryMonthPlanProdResultDto param) {
        if (null == param || StringUtils.isBlank(param.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryNoEmpty"));
        }
        return factoryMonthPlanAdjustRemoteService.getAdjustControlInfo(param);
    }

    @RequiresPermissions("monthplan:planAdjust:import")
    @ApiOperation("计划调整导入")
    @PostMapping({"/importData"})
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = factoryMonthPlanProdFinalRemoteService.importData(context, false);
        return ajaxResult;
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<FactoryMonthPlanProdFinal> util = new ExcelUtil<>(FactoryMonthPlanProdFinal.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("计划导出")
    @RequiresPermissions("monthplan:planAdjust:export")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, FactoryMonthPlanProdFinal entity) throws IOException {
        // 分厂、年份、月份不可为空
        if (null == entity || null == entity.getMonth() || null == entity.getYear() || StringUtils.isBlank(entity.getFactoryCode())) {
            String fileName = this.getExportTemplateFileName();
            ExcelUtil<FactoryMonthPlanProdFinal> util = new ExcelUtil<>(FactoryMonthPlanProdFinal.class);
            util.exportExcel(response, null, fileName, fileName);
            return;
        }

        String fileName = this.getExportTemplateFileName();
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(entity, prodFinal);
        byte[] excelBytes = factoryMonthPlanProdFinalRemoteService.exportData(prodFinal, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }


    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.importTemplateName");
    }

    /**
     * 对分厂月计划执行调整
     *
     * @param adjustPlan 调整计划
     * @return
     */
    @ResponseBody
    @ApiOperation("对分厂月计划执行调整")
    @PostMapping("/adjustFactoryMonthPlan")
    public AjaxResult adjustMonthPlan(@RequestBody FactoryMonthPlanAdjustPlanVo adjustPlan) {
        if (null == adjustPlan) {
            return AjaxResult.success();
        }
        String factoryCode = adjustPlan.getFactoryCode();
        Integer year = adjustPlan.getYear();
        Integer month = adjustPlan.getMonth();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        Integer adjustNumber = adjustPlan.getAdjustNumber();
        if (null == adjustNumber || adjustNumber == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkAdjustNumber"));
        }
        if (null == adjustPlan.getStartDate()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkStartDate"));
        }
        String productionNo = adjustPlan.getProductionNo();
        //新增插入规格
        if (StringUtils.isBlank(productionNo)) {
            if (adjustNumber < 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.addProductNoSubtract"));
            }
            String productCode = adjustPlan.getProductCode();
            String mouldNo = adjustPlan.getMouldNo();
            String locationType = adjustPlan.getLocationType();
            String specCode = adjustPlan.getSpecCode();
            if (StringUtils.isBlank(productCode) || StringUtils.isBlank(mouldNo) || StringUtils.isBlank(locationType) || StringUtils.isBlank(specCode)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkSpecCodeInfo"));
            }
        }
        return factoryMonthPlanAdjustRemoteService.adjustMonthPlan(adjustPlan);
    }
}
