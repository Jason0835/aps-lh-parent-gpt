package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.TireCapacityConfigurationVo;
import com.zlt.aps.monthplan.api.service.ITireCapacityConfigurationRemoteService;
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
import java.util.Arrays;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TireCapacityConfigurationUIController.java
 * 描    述：轮胎类型产能配置(特殊情况下配置) UI控制层类：....
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
@RequestMapping("/monthsetting/tireCapacity")
@Api(tags = "轮胎类型产能配置(特殊情况下配置)前端业务接口-->ZLT")
public class TireCapacityConfigurationUIController extends BaseUIController<TireCapacityConfiguration> {

    private final ITireCapacityConfigurationRemoteService iTireCapacityConfigurationService;

    /**
     * 根据条件查询主表数据
     */
    @ResponseBody
    @PostMapping("/list")
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthsetting:tireCapacity:list")
    public TableDataInfo list(TireCapacityConfiguration tireCapacityConfiguration) {
        return iTireCapacityConfigurationService.list(tireCapacityConfiguration);
    }

    /**
     * 根据分厂、年月、需求版本获取轮胎类型、寸口的需求信息
     */
    @ResponseBody
    @PostMapping("/getDemandInfo")
    @ApiOperation("根据分厂、年月、需求版本获取轮胎类型、寸口的需求信息")
    public TireCapacityConfigurationVo getDemandInfo(@RequestBody TireCapacityConfiguration tireCapacityConfiguration) {
        return iTireCapacityConfigurationService.getDemandInfo(tireCapacityConfiguration);
    }

    /**
     * 根据条件获取配置编辑信息
     */
    @ResponseBody
    @PostMapping("/getInfo")
    @ApiOperation("根据条件获取配置编辑信息")
    public TireCapacityConfigurationVo getTireCapacityConfiguration(@RequestBody TireCapacityConfiguration tireCapacityConfiguration) {
        if (null == tireCapacityConfiguration) {
            return new TireCapacityConfigurationVo();
        }
        Long id = tireCapacityConfiguration.getId();
        if (null == id) {
            return new TireCapacityConfigurationVo();
        }
        return iTireCapacityConfigurationService.getTireCapacityConfiguration(id);
    }

    /**
     * 修改或新增
     */
    @ResponseBody
    @PostMapping("/save")
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthsetting:tireCapacity:edit")
    public AjaxResult save(@RequestBody TireCapacityConfiguration tireCapacityConfiguration) {
        if (null == tireCapacityConfiguration) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.tireCapacityConfiguration.noEmpty"));
        }
        if (StringUtils.isBlank(tireCapacityConfiguration.getFactoryCode()) || null == tireCapacityConfiguration.getYear() || null == tireCapacityConfiguration.getMonth()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.noEmpty"));
        }
        if (StringUtils.isBlank(tireCapacityConfiguration.getMonthPlanVersion()) || null == tireCapacityConfiguration.getProSize()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.tireCapacityConfiguration.noEmpty"));
        }
        if (StringUtils.isBlank(tireCapacityConfiguration.getTireType()) || null == tireCapacityConfiguration.getMonthCapacity()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.tireCapacityConfiguration.noEmpty"));
        }
        String checkResult = iTireCapacityConfigurationService.checkUnique(tireCapacityConfiguration);
        if (UserConstants.NOT_UNIQUE.equals(checkResult)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.tireCapacityConfiguration.checkUnique"));
        }
        return iTireCapacityConfigurationService.save(tireCapacityConfiguration);
    }

    /**
     * 删除轮胎类型产能配置(特殊情况下配置)
     */
    @ApiOperation("删除,id不为空）")
    @RequiresPermissions("monthsetting:tireCapacity:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTireCapacityConfigurationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验轮胎类型产能配置(特殊情况下配置)唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TireCapacityConfiguration tireCapacityConfiguration) {
        return iTireCapacityConfigurationService.checkUnique(tireCapacityConfiguration);
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
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.tireCapacity.modelName");
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TireCapacityConfiguration entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTireCapacityConfigurationService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iTireCapacityConfigurationService.importData(context, false);
        return ajaxResult;
    }
}
