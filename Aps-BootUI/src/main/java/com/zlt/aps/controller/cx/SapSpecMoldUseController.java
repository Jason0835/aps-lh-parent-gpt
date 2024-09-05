package com.zlt.aps.controller.cx;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;
import com.zlt.aps.cx.api.service.ISapSpecMoldUseService;

/**
 * 规格使用模数Controller
 * @author zlt
 * @date 2022-01-18
 */
@Api(tags = "规格使用模数")
@Controller
@RequestMapping("/cx/sapSpecMoldUse")
public class SapSpecMoldUseController extends BaseController {

    @Autowired
    private ISapSpecMoldUseService iSapSpecMoldUseService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/sapSpecMoldUse";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:sapSpecMoldUse:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/sapSpecMoldUse";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("sapSpecMoldUse", new SapSpecMoldUse());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("sapSpecMoldUse", iSapSpecMoldUseService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询规格使用模数列表
     */
    @ApiOperation("根据条件查询规格使用模数列表")
    @RequiresPermissions("cx:sapSpecMoldUse:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SapSpecMoldUse entity) {
        return iSapSpecMoldUseService.list(entity);
    }

    /**
     * 修改或新增规格使用模数
     */
    @ApiOperation("修改或新增规格使用模数")
    @RequiresPermissions("cx:sapSpecMoldUse:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SapSpecMoldUse sapSpecMoldUse) {
        AjaxResult ajaxResult = null;
        if(UserConstants.NOT_UNIQUE.equals(iSapSpecMoldUseService.checkSapSpecMoldUseUnique(sapSpecMoldUse))){
           return  AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        if (sapSpecMoldUse.getId() != null){
            ajaxResult = iSapSpecMoldUseService.edit(sapSpecMoldUse);
        } else{
            ajaxResult = iSapSpecMoldUseService.add(sapSpecMoldUse);
        }
        return ajaxResult;
    }

    /**
     * 删除规格使用模数
     */
    @ApiOperation("删除规格使用模数（id不为空）")
    @RequiresPermissions("cx:sapSpecMoldUse:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSapSpecMoldUseService.remove(arr);
    }

    /**
     * 校验规格使用模数唯一性
     */
    @ApiOperation("校验规格使用模数唯一性")
    @PostMapping("/checkSapSpecMoldUseUnique")
    @ResponseBody
    public String checkSapSpecMoldUseUnique(SapSpecMoldUse sapSpecMoldUse) {
        return iSapSpecMoldUseService.checkSapSpecMoldUseUnique(sapSpecMoldUse);
    }

    /**
     * 导出规格使用模数
     */
    @ApiOperation("导出规格使用模数")
    @RequiresPermissions("cx:sapSpecMoldUse:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,SapSpecMoldUse sapSpecMoldUse) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.modelName");
        List<SapSpecMoldUse> list = iSapSpecMoldUseService.getList(sapSpecMoldUse);
        ExcelUtil<SapSpecMoldUse> util = new ExcelUtil<>(SapSpecMoldUse. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, sapSpecMoldUse.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.modelName");
        ExcelUtil<SapSpecMoldUse> util = new ExcelUtil<>(SapSpecMoldUse.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:sapSpecMoldUse:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<SapSpecMoldUse> util = new ExcelUtil<>(SapSpecMoldUse.class);
        InputStream in = new ByteArrayInputStream(data);
        List<SapSpecMoldUse> list = util.importExcel(in);
        AjaxResult ajaxResult = iSapSpecMoldUseService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据胎胚代码获取规格尺寸
     */
    @ApiOperation("根据胎胚代码获取规格尺寸")
    @PostMapping("/getSpecDesc")
    @ResponseBody
    public AjaxResult getSpecDesc(SapSpecMoldUse sapSpecMoldUse) {
        if (StringUtils.isBlank(sapSpecMoldUse.getSapCode())) {
            return AjaxResult.error();
        }
        List<SapSpecMoldUse> list = iSapSpecMoldUseService.getSpecDesc(sapSpecMoldUse);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.success(list.get(0));
        } else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.notFoundSpecDesc"));
        }
    }

}
