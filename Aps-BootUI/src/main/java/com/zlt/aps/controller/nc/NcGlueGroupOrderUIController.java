package com.zlt.aps.controller.nc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.nc.api.domain.entity.NcGlueGroupOrder;
import com.zlt.aps.nc.api.service.INcGlueGroupOrderRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬胶料组别顺序UIController
 *
 * @author zlt
 */
@Api(tags = "内衬胶料组别顺序")
@Controller
@RequestMapping("/nc/glueGroupOrder")
public class NcGlueGroupOrderUIController extends BaseUIController<NcGlueGroupOrder> {

    @Autowired
    private INcGlueGroupOrderRemoteService iNcGlueGroupOrderService;

    private final String prefix = "aps/nc/glueGroupOrder";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("nc:glueGroupOrder:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/glueGroupOrder";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ncGlueGroupOrder", new NcGlueGroupOrder());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ncGlueGroupOrder", iNcGlueGroupOrderService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("nc:glueGroupOrder:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(NcGlueGroupOrder ncGlueGroupOrder) {
        return iNcGlueGroupOrderService.list(ncGlueGroupOrder);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("nc:glueGroupOrder:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(NcGlueGroupOrder ncGlueGroupOrder) {
        if (UserConstants.NOT_UNIQUE.equals(iNcGlueGroupOrderService.checkUnique(ncGlueGroupOrder))) {
            return AjaxResult.error("新增组别编码'" + ncGlueGroupOrder.getGlueGroupCode() + "'失败，组别编码已存在");
        }
        return iNcGlueGroupOrderService.save(ncGlueGroupOrder);
    }

    /**
     * 删除
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("nc:glueGroupOrder:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcGlueGroupOrderService.removeByIds(Arrays.asList(arr));
    }

    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }

    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.nc.glueGroupOrder.column.modalName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<NcGlueGroupOrder> util = new ExcelUtil<>(NcGlueGroupOrder.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({ "/export" })
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, NcGlueGroupOrder entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iNcGlueGroupOrderService.exportData(entity, fileName);
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
        AjaxResult ajaxResult = iNcGlueGroupOrderService.importData(context, updateSupport);
        return ajaxResult;
    }
}
