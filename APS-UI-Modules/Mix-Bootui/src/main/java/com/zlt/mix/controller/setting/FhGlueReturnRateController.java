package com.zlt.mix.controller.setting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.FhGlueReturnRate;
import com.zlt.mix.setting.api.service.IFhGlueReturnRateService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 返回胶日返回率Controller
 * @author zlt
 * @date 2022-11-28
 */
@Api(tags = "返回胶日返回率")
@Controller
@RequestMapping("/setting/fhGlueRate")
public class FhGlueReturnRateController extends BaseController {

    @Resource
    private IFhGlueReturnRateService iFhGlueReturnRateService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/fhGlueRate";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:fhGlueRate:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/fhGlueRate";
    }

    @ApiOperation("根据条件查询返回胶日返回率列表")
    @RequiresPermissions("setting:fhGlueRate:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listFhGlueReturnRate(FhGlueReturnRate entity) {
        return iFhGlueReturnRateService.listFhGlueReturnRate(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("fhGlueReturnRate", new FhGlueReturnRate());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("fhGlueReturnRate", iFhGlueReturnRateService.getFhGlueReturnRateInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增返回胶日返回率")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveFhGlueReturnRate(FhGlueReturnRate fhGlueReturnRate) {
        return iFhGlueReturnRateService.saveFhGlueReturnRate(fhGlueReturnRate);
    }

    @ApiOperation("删除返回胶日返回率（id不为空）")
    @RequiresPermissions("setting:fhGlueRate:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeFhGlueReturnRate(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iFhGlueReturnRateService.deleteFhGlueReturnRate(arr);
    }

    @ApiOperation("校验返回胶日返回率唯一性")
    @PostMapping("/checkFhGlueReturnRateUnique")
    @ResponseBody
    public String checkFhGlueReturnRateUnique(FhGlueReturnRate fhGlueReturnRate) {
        return iFhGlueReturnRateService.checkFhGlueReturnRateUnique(fhGlueReturnRate);
    }

    /**
     * 导出返回胶日返回率
     */
    @ApiOperation("导出返回胶日返回率")
    @RequiresPermissions("setting:fhGlueRate:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,FhGlueReturnRate fhGlueReturnRate) throws IOException {
        String fileName = I18nUtil.getMessage("setting.fhGlueRate.modelName");
        List<FhGlueReturnRate> list = iFhGlueReturnRateService.exportData(fhGlueReturnRate);
        ExcelUtil<FhGlueReturnRate> util = new ExcelUtil<>(FhGlueReturnRate. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, fhGlueReturnRate.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.fhGlueRate.modelName");
        ExcelUtil<FhGlueReturnRate> util = new ExcelUtil<>(FhGlueReturnRate.class);
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
    @RequiresPermissions("setting:fhGlueRate:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.fhGlueRate.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<FhGlueReturnRate> util = new ExcelUtil<>(FhGlueReturnRate.class);
        List<FhGlueReturnRate> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iFhGlueReturnRateService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
