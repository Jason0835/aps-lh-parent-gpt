package com.zlt.aps.controller.tm;

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
import com.zlt.aps.template.tm.TmStockTemp;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.api.service.ITmStockService;
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
 * 胎面库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/tm/stock")
@Api(tags = {"胎面胶库存信息维护接口"})
public class TmStockController extends BaseController {
    private String prefix = "tm/stock";

    @Autowired
    private ITmStockService tTmStockService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;


    @RequiresPermissions("tm:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增胎侧库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tmStock", new TmStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改胎侧库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmStock", tTmStockService.selectTmStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至胎侧库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tmStock", tTmStockService.selectTmStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 胎面库存信息列表
     */
    @ApiOperation("查询胎面胶库存信息列表")
    @RequiresPermissions("tm:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmStock tTmStock) {
        return tTmStockService.list(tTmStock);
    }

    /**
     * 删除胎面库存信息
     */
    @ApiOperation("删除胎面胶库存信息")
    @RequiresPermissions("tm:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return tTmStockService.remove(arr);
    }

    /**
     * 新增胎面库存信息
     */
    @ApiOperation("新增胎面胶库存信息")
    @RequiresPermissions("tm:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TmStock stock) {
        return tTmStockService.add(stock);
    }

    /**
     * 修改胎面库存信息
     */
    @ApiOperation("修改胎面胶库存信息")
    @RequiresPermissions("tm:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TmStock stock) {
        return tTmStockService.edit(stock);
    }

    @ApiOperation("导出胎面库存信息")
    @RequiresPermissions("tm:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmStock stock) throws Exception {
        String fileName = I18nUtil.getMessage("ui.tm.stock.export.fileName");
        List<TmStock> list = tTmStockService.exportList(stock);
        ExcelUtil<TmStock> util = new ExcelUtil(TmStock.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_TM);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tm.stock.export.fileName");
        ExcelUtil<TmStockTemp> util = new ExcelUtil<>(TmStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("tm:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TM,
                I18nUtil.getMessage("ui.tm.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TmStock> util = new ExcelUtil<>(TmStock.class);
        List<TmStock> list = util.importExcel(in);
        AjaxResult ajaxResult = tTmStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
