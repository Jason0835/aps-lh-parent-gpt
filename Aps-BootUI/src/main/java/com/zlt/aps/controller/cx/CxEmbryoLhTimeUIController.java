package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.cx.service.ICxEmbryoLhTimeRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 胎胚最早可供硫化时间 UI控制层类
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "胎胚最早可供硫化时间管理")
@Controller
@RequestMapping("/cx/cxEmbryoLhTime")
public class CxEmbryoLhTimeUIController extends BaseUIController<CxEmbryoLhTime> {

    @Autowired
    private ICxEmbryoLhTimeRemoteService iCxEmbryoLhTimeService;

    /**
     * 根据条件查询主表数据
     *
     * @param cxEmbryoLhTime 查询条件
     * @return 列表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxEmbryoLhTime cxEmbryoLhTime) {
        return iCxEmbryoLhTimeService.list(cxEmbryoLhTime);
    }

    /**
     * 修改或新增
     *
     * @param cxEmbryoLhTime 实体对象
     * @return 操作结果
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions({"cx:cxEmbryoLhTime:edit", "cx:cxEmbryoLhTime:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(CxEmbryoLhTime cxEmbryoLhTime) {
        if (UserConstants.NOT_UNIQUE.equals(iCxEmbryoLhTimeService.checkUnique(cxEmbryoLhTime))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.cxEmbryoLhTime.notUnique"));
        }
        return iCxEmbryoLhTimeService.save(cxEmbryoLhTime);
    }

    /**
     * 删除胎胚最早可供硫化时间
     *
     * @param ids 主键ID字符串，逗号分隔
     * @return 操作结果
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("cx:cxEmbryoLhTime:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxEmbryoLhTimeService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验胎胚最早可供硫化时间唯一性
     *
     * @param cxEmbryoLhTime 实体对象
     * @return 唯一性校验结果
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(CxEmbryoLhTime cxEmbryoLhTime) {
        return iCxEmbryoLhTimeService.checkUnique(cxEmbryoLhTime);
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
        return I18nUtil.getMessage("ui.data.column.cxEmbryoLhTime.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     *
     * @param response HTTP响应
     * @return 操作结果
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<CxEmbryoLhTime> util = new ExcelUtil<>(CxEmbryoLhTime.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导出
     *
     * @param response HTTP响应
     * @param entity   查询条件
     */
    @ApiOperation("数据导出")
    @RequiresPermissions("cx:cxEmbryoLhTime:export")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, CxEmbryoLhTime entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iCxEmbryoLhTimeService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 数据导入
     *
     * @param file          上传文件
     * @param updateSupport 已存在记录是否更新
     * @return 操作结果
     */
    @RequiresPermissions("cx:cxEmbryoLhTime:import")
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
        return iCxEmbryoLhTimeService.importData(context, updateSupport);
    }
}
