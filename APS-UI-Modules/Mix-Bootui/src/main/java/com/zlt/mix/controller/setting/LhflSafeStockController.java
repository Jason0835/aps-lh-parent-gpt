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
import com.zlt.mix.setting.api.domain.entity.LhflSafeStock;
import com.zlt.mix.setting.api.service.ILhflSafeStockService;
import com.zlt.mix.template.setting.LhflSafeStockTemp;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 硫磺辅料安全库存Controller
 * @author hakimryan
 *
 */
@Api(tags = "硫磺辅料安全库存")
@Controller
@RequestMapping("/setting/lhflSafeStock")
public class LhflSafeStockController extends BaseController {

    @Resource
    private ILhflSafeStockService iLhflSafeStockService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/lhflSafeStock";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:lhflSafeStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhflSafeStock";
    }

    @ApiOperation("根据条件查询安全库存列表")
    @RequiresPermissions("setting:lhflSafeStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listLhflSafeStock(LhflSafeStock entity) {
        return iLhflSafeStockService.listLhflSafeStock(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("lhflSafeStock", new LhflSafeStock());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhflSafeStock", iLhflSafeStockService.getLhflSafeStockInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增安全库存")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveLhflSafeStock(LhflSafeStock lhflSafeStock) {
        return iLhflSafeStockService.saveLhflSafeStock(lhflSafeStock);
    }

    @ApiOperation("删除安全库存（id不为空）")
    @RequiresPermissions("setting:lhflSafeStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeLhflSafeStock(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhflSafeStockService.deleteLhflSafeStock(arr);
    }

    @ApiOperation("校验安全库存唯一性")
    @PostMapping("/checkLhflSafeStockUnique")
    @ResponseBody
    public String checkLhflSafeStockUnique(LhflSafeStock lhflSafeStock) {
        return iLhflSafeStockService.checkLhflSafeStockUnique(lhflSafeStock);
    }

    /**
     * 导出安全库存
     */
    @ApiOperation("导出安全库存")
    @RequiresPermissions("setting:lhflSafeStock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,LhflSafeStock lhflSafeStock) throws IOException {
        String fileName = I18nUtil.getMessage("setting.lhfl.safeStock.modelName");
        List<LhflSafeStock> list = iLhflSafeStockService.exportData(lhflSafeStock);
        ExcelUtil<LhflSafeStock> util = new ExcelUtil<>(LhflSafeStock. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhflSafeStock.toString(),ZltConstant.PROCEDURE_CODE_FL_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.lhfl.safeStock.modelName");
        ExcelUtil<LhflSafeStockTemp> util = new ExcelUtil<>(LhflSafeStockTemp.class);
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
    @RequiresPermissions("setting:lhflSafeStock:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_FL_SETTING,
                I18nUtil.getMessage("setting.lhfl.safeStock.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<LhflSafeStock> util = new ExcelUtil<>(LhflSafeStock.class);
        List<LhflSafeStock> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iLhflSafeStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

//    @ApiOperation("修改安全库存")
//    @PostMapping("/updateSafeStock")
//    @ResponseBody
//    public AjaxResult updateSafeStock(LhflSafeStock lhflSafeStock) {
//        return iLhflSafeStockService.updateSafeStockByMixAreaAndLhfl(lhflSafeStock);
//    }
}
