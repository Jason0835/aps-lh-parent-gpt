package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.api.service.ISizeCapacityConfigurationRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SizeCapacityConfigurationUIController.java
 * 描    述：寸口产能配置 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/monthsetting/sizeCapacity")
@Api(tags = "寸口产能配置业务前端服务接口-->ZLT")
public class SizeCapacityConfigurationUIController extends BaseUIController<SizeCapacityConfiguration> {

    private final ISizeCapacityConfigurationRemoteService iSizeCapacityConfigurationService;

    /**
     * 根据条件查询主表数据
     */
    @ResponseBody
    @PostMapping("/list")
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthsetting:sizeCapacity:list")
    public TableDataInfo list(SizeCapacityConfiguration sizeCapacityConfiguration) {
        return iSizeCapacityConfigurationService.list(sizeCapacityConfiguration);
    }

    /**
     * 根据分厂、年月、需求版本获取寸口的需求信息
     */
    @ResponseBody
    @PostMapping("/getDemandInfo")
    @ApiOperation("根据分厂、年月、需求版本获取寸口的需求信息")
    public SizeCapacityConfigurationVo getDemandInfo(@RequestBody SizeCapacityConfiguration sizeCapacityConfiguration) {
        return iSizeCapacityConfigurationService.getDemandInfo(sizeCapacityConfiguration);
    }

    /**
     * 根据分厂、年、月、需求版本，生成寸口产能配置
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @PostMapping("/buildSizeCapacityConfiguration")
    @ApiOperation("根据分厂、年、月、需求版本，生成寸口产能配置")
    public AjaxResult autoBuildConfiguration(@RequestBody BuildSizeCapacityParamVo factoryProductionParam) {
        return iSizeCapacityConfigurationService.autoBuildConfiguration(factoryProductionParam);
    }

    /**
     * 根据分厂、年、月、查看产能配置详情
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @PostMapping("/getDaySizeCapacityInfo")
    @ApiOperation("根据分厂、年、月、查看产能配置详情")
    public List<DaySizeCapacityConfigurationDetailVo> getDaySizeCapacityInfo(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        return iSizeCapacityConfigurationService.getDaySizeCapacityInfo(factoryProductionParam);
    }

    /**
     * 根据分厂、年、月、查看产能配置详情
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @PostMapping("/getSizeDayCapacityInfo")
    @ApiOperation("根据分厂、年、月、查看寸口产能配置详情")
    public List<DaySizeCapacityConfigurationMouldMethodDetailVo> getSizeDayCapacityInfo(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        return iSizeCapacityConfigurationService.getSizeDayCapacityInfo(factoryProductionParam);
    }

    /**
     * 获取编辑信息
     */
    @ResponseBody
    @PostMapping("/getInfo")
    @ApiOperation("获取编辑信息")
    public SizeCapacityConfigurationVo getSizeCapacityConfiguration(@RequestBody SizeCapacityConfiguration sizeCapacityConfiguration) {
        if (null == sizeCapacityConfiguration) {
            return new SizeCapacityConfigurationVo();
        }
        Long id = sizeCapacityConfiguration.getId();
        if (null == id) {
            return new SizeCapacityConfigurationVo();
        }
        return iSizeCapacityConfigurationService.getSizeCapacityConfiguration(id);
    }

    /**
     * 修改或新增
     */
    @RequiresPermissions("monthsetting:sizeCapacity:edit")
    @ApiOperation("修改或新增")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(@RequestBody SizeCapacityConfiguration sizeCapacityConfiguration) {
        if (null == sizeCapacityConfiguration) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.noEmpty"));
        }
        if (StringUtils.isBlank(sizeCapacityConfiguration.getFactoryCode()) || null == sizeCapacityConfiguration.getYear() || null == sizeCapacityConfiguration.getMonth()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.noEmpty"));
        }
        if (null == sizeCapacityConfiguration.getProSize() || null == sizeCapacityConfiguration.getDayCapacity() || null == sizeCapacityConfiguration.getMachineNumber()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.noEmpty"));
        }
        if (StringUtils.isBlank(sizeCapacityConfiguration.getMonthPlanVersion()) || StringUtils.isBlank(sizeCapacityConfiguration.getMouldMethod())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.noEmpty"));
        }
        BigDecimal machineNumber = sizeCapacityConfiguration.getMachineNumber();
        BigDecimal machine = machineNumber.stripTrailingZeros();
        if (machine.scale() == 0 && null != sizeCapacityConfiguration.getNextProSize()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.noConfigurationNextProSize"));
        }
        String checkResult = iSizeCapacityConfigurationService.checkUnique(sizeCapacityConfiguration);
        if (UserConstants.NOT_UNIQUE.equals(checkResult)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.checkUnique"));
        }
        return iSizeCapacityConfigurationService.save(sizeCapacityConfiguration);
    }

    /**
     * 删除寸口产能配置
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("monthsetting:sizeCapacity:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSizeCapacityConfigurationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验寸口产能配置唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(SizeCapacityConfiguration sizeCapacityConfiguration) {
        return iSizeCapacityConfigurationService.checkUnique(sizeCapacityConfiguration);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0204";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.sizeCapacity.modelName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, SizeCapacityConfiguration entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iSizeCapacityConfigurationService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iSizeCapacityConfigurationService.importData(context, false);
        return ajaxResult;
    }
}
