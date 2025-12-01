package com.zlt.mix.controller.setting;

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
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;
import com.zlt.mix.setting.api.service.IGlueSafeStockService;
import com.zlt.mix.template.setting.GlueSafeStockTemp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 安全库存Controller
 * @author Gim
 * @date 2022-03-21
 */
@Api(tags = "安全库存")
@Controller
@RequestMapping("/setting/safeStock")
public class GlueSafeStockController extends BaseController {

    @Resource
    private IGlueSafeStockService iGlueSafeStockService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/safeStock";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:safeStock:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/safeStock";
    }

    @ApiOperation("根据条件查询安全库存列表")
    @RequiresPermissions("setting:safeStock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueSafeStock(GlueSafeStock entity) {
        return iGlueSafeStockService.listGlueSafeStock(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("glueSafeStock", new GlueSafeStock());
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueSafeStock", iGlueSafeStockService.getGlueSafeStockInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增安全库存")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueSafeStock(GlueSafeStock glueSafeStock) {
        return iGlueSafeStockService.saveGlueSafeStock(glueSafeStock);
    }

    @ApiOperation("删除安全库存（id不为空）")
    @RequiresPermissions("setting:safeStock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueSafeStock(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueSafeStockService.deleteGlueSafeStock(arr);
    }

    @ApiOperation("校验安全库存唯一性")
    @PostMapping("/checkGlueSafeStockUnique")
    @ResponseBody
    public String checkGlueSafeStockUnique(GlueSafeStock glueSafeStock) {
        return iGlueSafeStockService.checkGlueSafeStockUnique(glueSafeStock);
    }

    /**
     * 导出安全库存
     */
    @ApiOperation("导出安全库存")
    @RequiresPermissions("setting:safeStock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,GlueSafeStock glueSafeStock) throws IOException {
        String fileName = I18nUtil.getMessage("setting.safeStock.modelName");
        List<GlueSafeStock> list = iGlueSafeStockService.exportData(glueSafeStock);
        ExcelUtil<GlueSafeStock> util = new ExcelUtil<>(GlueSafeStock. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, glueSafeStock.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.safeStock.modelName");
        ExcelUtil<GlueSafeStockTemp> util = new ExcelUtil<>(GlueSafeStockTemp.class);
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
    @RequiresPermissions("setting:safeStock:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.safeStock.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<GlueSafeStock> util = new ExcelUtil<>(GlueSafeStock.class);
        List<GlueSafeStock> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iGlueSafeStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    @ApiOperation("修改安全库存")
    @PostMapping("/updateSafeStock")
    @ResponseBody
    public AjaxResult updateSafeStock(GlueSafeStock glueSafeStock) {
        return iGlueSafeStockService.updateSafeStockByMixAreaAndGlue(glueSafeStock);
    }
}
