package com.zlt.aps.controller.tq;

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
import com.zlt.aps.template.tq.TqStockTemp;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.api.service.ITqStockService;
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
 * 胎圈库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/tq/stock")
@Api(tags = {"胎圈库存信息维护接口"})
public class TqStockController extends BaseController {
    private String prefix = "tq/stock";

    @Autowired
    private ITqStockService stockService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至胎圈库存列表页面
     */
    @RequiresPermissions("tq:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增胎圈库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new TqStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改胎圈库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至胎圈库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 胎圈库存信息列表
     */
    @ApiOperation("查询胎圈库存信息列表")
    @RequiresPermissions("tq:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TqStock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除胎圈库存信息
     */
    @ApiOperation("删除胎圈库存信息")
    @RequiresPermissions("tq:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增胎圈库存信息
     */
    @ApiOperation("新增胎圈库存信息")
    @RequiresPermissions("tq:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TqStock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改胎圈库存信息
     */
    @ApiOperation("修改胎圈库存信息")
    @RequiresPermissions("tq:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TqStock stock) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (stock.getId() != null) {
            ajaxResult = stockService.edit(stock);
        } else {
            ajaxResult = stockService.add(stock);
        }
        return ajaxResult;
    }

    @ApiOperation("导出胎圈库存信息")
    @RequiresPermissions("tq:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TqStock stock) throws IOException {
        List<TqStock> list = stockService.exportList(stock);
        ExcelUtil<TqStock> util = new ExcelUtil<>(TqStock.class);
        String fileName = I18nUtil.getMessage("ui.tq.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_TQ);
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
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tq.stock.export.fileName");
        ExcelUtil<TqStockTemp> util = new ExcelUtil<>(TqStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("tq:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TQ,
                I18nUtil.getMessage("ui.tq.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TqStock> util = new ExcelUtil<>(TqStock.class);
        List<TqStock> list = util.importExcel(in);
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
