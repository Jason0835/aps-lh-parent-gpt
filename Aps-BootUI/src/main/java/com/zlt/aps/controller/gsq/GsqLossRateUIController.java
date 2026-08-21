package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqLossRate;
import com.zlt.aps.gsq.api.service.IGsqLossRateService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 钢丝圈损耗率管理 UI 控制层
 *
 * @author zlt
 * @date 2026-07-08
 */
@Api(tags = "钢丝圈损耗率管理")
@Controller
@RequestMapping("/gsq/lossRate")
public class GsqLossRateUIController extends BaseUIController<GsqLossRate> {

    @Resource
    private IGsqLossRateService gsqLossRateService;

    /** 查询钢丝圈损耗率列表 */
    @ApiOperation("查询钢丝圈损耗率列表")
    @RequiresPermissions("gsq:lossRate:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqLossRate queryVO) {
        return gsqLossRateService.list(queryVO);
    }

    /** 获取钢丝圈损耗率详情 */
    @ApiOperation("获取钢丝圈损耗率详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GsqLossRate getInfo(@PathVariable("id") Long id) {
        return gsqLossRateService.getInfo(id);
    }

    /** 新增钢丝圈损耗率（前端统一以multipart/form-data提交，不能用@RequestBody，需用表单绑定） */
    @ApiOperation("新增钢丝圈损耗率")
    @RequiresPermissions("gsq:lossRate:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(GsqLossRate entity) {
        if (UserConstants.NOT_UNIQUE.equals(gsqLossRateService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.lossRate.checkUnique"));
        }
        return gsqLossRateService.add(entity);
    }

    /** 编辑钢丝圈损耗率（前端统一以multipart/form-data提交，不能用@RequestBody，需用表单绑定） */
    @ApiOperation("编辑钢丝圈损耗率")
    @RequiresPermissions("gsq:lossRate:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(GsqLossRate entity) {
        if (UserConstants.NOT_UNIQUE.equals(gsqLossRateService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.lossRate.checkUnique"));
        }
        return gsqLossRateService.edit(entity);
    }

    /** 删除钢丝圈损耗率 */
    @ApiOperation("删除钢丝圈损耗率")
    @RequiresPermissions("gsq:lossRate:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return gsqLossRateService.removeByIds(Arrays.asList(idArray));
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "GSQ";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.gsq.lossRate.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<GsqLossRate> util = new ExcelUtil<>(GsqLossRate.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出钢丝圈损耗率 */
    @ApiOperation("导出钢丝圈损耗率")
    @RequiresPermissions("gsq:lossRate:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GsqLossRate entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = gsqLossRateService.exportData(entity, fileName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入钢丝圈损耗率 */
    @ApiOperation("导入钢丝圈损耗率")
    @RequiresPermissions("gsq:lossRate:import")
    @PostMapping("/importData")
    @ResponseBody
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(getFunctionName());
        context.setProcedureCode(getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return gsqLossRateService.importData(context, updateSupport);
    }
}
