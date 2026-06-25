package com.zlt.aps.controller.tm;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.tm.api.domain.entity.TmStockCoverClass;
import com.zlt.aps.tm.api.service.ITmStockCoverClassRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 备库班数配置Controller
 *
 * @author zlt
 */
@Api(tags = "备库班数配置")
@Controller
@RequestMapping("/tm/tmStockCoverClass")
public class TmStockCoverClassUIController extends BaseUIController<TmStockCoverClass> {

    @Autowired
    private ITmStockCoverClassRemoteService iTmStockCoverClassService;

    private final String prefix = "aps/tm/tmStockCoverClass";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("tm:tmStockCoverClass:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/tmStockCoverClass";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmStockCoverClass", new TmStockCoverClass());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmStockCoverClass", iTmStockCoverClassService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("tm:tmStockCoverClass:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmStockCoverClass tmStockCoverClass) {
        return iTmStockCoverClassService.list(tmStockCoverClass);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("tm:tmStockCoverClass:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(TmStockCoverClass tmStockCoverClass) {
        if (UserConstants.NOT_UNIQUE.equals(iTmStockCoverClassService.checkUnique(tmStockCoverClass))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tm.stockCoverClass.notUnique"));
        }
        // 校验范围交叉
        if (UserConstants.NOT_UNIQUE.equals(iTmStockCoverClassService.checkRangeCross(tmStockCoverClass))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tm.stockCoverClass.rangeCross"));
        }
        return iTmStockCoverClassService.save(tmStockCoverClass);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("tm:tmStockCoverClass:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmStockCoverClassService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(TmStockCoverClass tmStockCoverClass) {
        return iTmStockCoverClassService.checkUnique(tmStockCoverClass);
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "TM0816";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.tm.stockCoverClass.column.modalName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<TmStockCoverClass> util = new ExcelUtil<>(TmStockCoverClass.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({ "/export" })
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, TmStockCoverClass entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iTmStockCoverClassService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({ "/importData" })
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
        AjaxResult ajaxResult = iTmStockCoverClassService.importData(context, updateSupport);
        return ajaxResult;
    }
}
