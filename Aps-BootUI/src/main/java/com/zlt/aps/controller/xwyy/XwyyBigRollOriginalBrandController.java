package com.zlt.aps.controller.xwyy;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import com.zlt.aps.xwyy.api.service.IXwyyBigRollOriginalBrandService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 帘布大卷原线品牌Controller
 *
 * @author chen
 * @date 2022-05-11
 */
@Api(tags = "帘布大卷原线品牌")
@Controller
@RequestMapping("/xwyy/bigRollOriginalBrand")
public class XwyyBigRollOriginalBrandController extends BaseController {

    @Autowired
    private IXwyyBigRollOriginalBrandService iXwyyBigRollOriginalBrandService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "xwyy/bigRollOriginalBrand";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("xwyy:bigRollOriginalBrand:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/bigRollOriginalBrand";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("xwyyBigRollOriginalBrand", new XwyyBigRollOriginalBrand());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("xwyyBigRollOriginalBrand", iXwyyBigRollOriginalBrandService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询帘布大卷原线品牌列表
     */
    @ApiOperation("根据条件查询帘布大卷原线品牌列表")
    @RequiresPermissions("xwyy:bigRollOriginalBrand:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(XwyyBigRollOriginalBrand entity) {
        return iXwyyBigRollOriginalBrandService.list(entity);
    }

    /**
     * 修改或新增帘布大卷原线品牌
     */
    @ApiOperation("修改或新增帘布大卷原线品牌")
    @RequiresPermissions("xwyy:bigRollOriginalBrand:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        AjaxResult ajaxResult = null;
        if (xwyyBigRollOriginalBrand.getId() != null) {
            ajaxResult = iXwyyBigRollOriginalBrandService.edit(xwyyBigRollOriginalBrand);
        } else {
            ajaxResult = iXwyyBigRollOriginalBrandService.add(xwyyBigRollOriginalBrand);
        }
        return ajaxResult;
    }

    /**
     * 删除帘布大卷原线品牌
     */
    @ApiOperation("删除帘布大卷原线品牌（id不为空）")
    @RequiresPermissions("xwyy:bigRollOriginalBrand:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iXwyyBigRollOriginalBrandService.remove(arr);
    }

    /**
     * 校验帘布大卷原线品牌唯一性
     */
    @ApiOperation("校验帘布大卷原线品牌唯一性")
    @PostMapping("/checkXwyyBigRollOriginalBrandUnique")
    @ResponseBody
    public String checkXwyyBigRollOriginalBrandUnique(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        return iXwyyBigRollOriginalBrandService.checkXwyyBigRollOriginalBrandUnique(xwyyBigRollOriginalBrand);
    }

    /**
     * 导出帘布大卷原线品牌
     */
    @ApiOperation("导出帘布大卷原线品牌")
    @RequiresPermissions("xwyy:bigRollOriginalBrand:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.bigRollOriginalBrand.modelName");
        List<XwyyBigRollOriginalBrand> list = iXwyyBigRollOriginalBrandService.getList(xwyyBigRollOriginalBrand);
        ExcelUtil<XwyyBigRollOriginalBrand> util = new ExcelUtil<>(XwyyBigRollOriginalBrand.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, xwyyBigRollOriginalBrand.toString(), ApsConstant.PROCEDURE_CODE_XWYY);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.bigRollOriginalBrand.modelName");
        ExcelUtil<XwyyBigRollOriginalBrand> util = new ExcelUtil<>(XwyyBigRollOriginalBrand.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("xwyy:bigRollOriginalBrand:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_XWYY,
                I18nUtil.getMessage("ui.data.column.bigRollOriginalBrand.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<XwyyBigRollOriginalBrand> util = new ExcelUtil<>(XwyyBigRollOriginalBrand.class);
        InputStream in = new ByteArrayInputStream(data);
        List<XwyyBigRollOriginalBrand> list = util.importExcel(in);
        AjaxResult ajaxResult = iXwyyBigRollOriginalBrandService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
