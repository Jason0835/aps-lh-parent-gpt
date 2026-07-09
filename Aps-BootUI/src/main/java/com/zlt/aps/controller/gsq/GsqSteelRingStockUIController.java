package com.zlt.aps.controller.gsq;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.gsq.api.domain.entity.GsqSteelRingStock;
import com.zlt.aps.gsq.api.service.IGsqSteelRingStockRemoteService;
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
 * 钢丝圈库存管理 UI 控制层
 *
 * @author zlt
 * @date 2026-07-08
 */
@Api(tags = "钢丝圈库存管理")
@Controller
@RequestMapping("/gsq/steelRingStock")
public class GsqSteelRingStockUIController extends BaseUIController<GsqSteelRingStock> {

    @Resource
    private IGsqSteelRingStockRemoteService gsqSteelRingStockRemoteService;

    /** 查询钢丝圈库存列表 */
    @ApiOperation("查询钢丝圈库存列表")
    @RequiresPermissions("gsq:steelRingStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqSteelRingStock queryVO) {
        return gsqSteelRingStockRemoteService.list(queryVO);
    }

    /** 获取钢丝圈库存详情 */
    @ApiOperation("获取钢丝圈库存详情")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public GsqSteelRingStock getInfo(@PathVariable("id") Long id) {
        return gsqSteelRingStockRemoteService.getInfo(id);
    }

    /** 新增钢丝圈库存 */
    @ApiOperation("新增钢丝圈库存")
    @RequiresPermissions("gsq:steelRingStock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody GsqSteelRingStock entity) {
        if (UserConstants.NOT_UNIQUE.equals(gsqSteelRingStockRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.steelRingStock.checkUnique"));
        }
        return gsqSteelRingStockRemoteService.add(entity);
    }

    /** 编辑钢丝圈库存 */
    @ApiOperation("编辑钢丝圈库存")
    @RequiresPermissions("gsq:steelRingStock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(@RequestBody GsqSteelRingStock entity) {
        if (UserConstants.NOT_UNIQUE.equals(gsqSteelRingStockRemoteService.checkUnique(entity))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.gsq.steelRingStock.checkUnique"));
        }
        return gsqSteelRingStockRemoteService.edit(entity);
    }

    /** 删除钢丝圈库存 */
    @ApiOperation("删除钢丝圈库存")
    @RequiresPermissions("gsq:steelRingStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] idArray = Convert.toLongArray(ids);
        return gsqSteelRingStockRemoteService.removeByIds(Arrays.asList(idArray));
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
        return I18nUtil.getMessage("ui.data.column.gsq.steelRingStock.modelName");
    }

    /** 下载导入模板 */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = getExportTemplateFileName();
        ExcelUtil<GsqSteelRingStock> util = new ExcelUtil<>(GsqSteelRingStock.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /** 导出钢丝圈库存 */
    @ApiOperation("导出钢丝圈库存")
    @RequiresPermissions("gsq:steelRingStock:export")
    @GetMapping("/export")
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, GsqSteelRingStock entity) throws IOException {
        String fileName = getExportTemplateFileName();
        byte[] excelBytes = gsqSteelRingStockRemoteService.exportData(entity, fileName);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
    }

    /** 导入钢丝圈库存 */
    @ApiOperation("导入钢丝圈库存")
    @RequiresPermissions("gsq:steelRingStock:import")
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
        return gsqSteelRingStockRemoteService.importData(context, updateSupport);
    }
}
