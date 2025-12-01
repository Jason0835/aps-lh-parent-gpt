package com.zlt.aps.controller.gsq;

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
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.api.service.IGsqStockService;
import com.zlt.aps.template.gsq.GsqStockTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * 钢丝圈库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/gsq/stock")
@Api(tags = {"钢丝圈库存信息维护接口"})
public class GsqStockController extends BaseController {
    private String prefix = "gsq/stock";

    @Autowired
    private IGsqStockService stockService;

    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至钢丝圈库存列表页面
     */
    @RequiresPermissions("gsq:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增钢丝圈库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("stock", new GsqStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改钢丝圈库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至钢丝圈库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("stock", stockService.selectStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 钢丝圈库存信息列表
     */
    @ApiOperation("查询钢丝圈库存信息列表")
    @RequiresPermissions("gsq:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GsqStock stock) {
        return stockService.list(stock);
    }

    /**
     * 删除钢丝圈库存信息
     */
    @ApiOperation("删除钢丝圈库存信息")
    @RequiresPermissions("gsq:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return stockService.remove(arr);
    }

    /**
     * 新增钢丝圈库存信息
     */
    @ApiOperation("新增钢丝圈库存信息")
    @RequiresPermissions("gsq:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(GsqStock stock) {
        return stockService.add(stock);
    }

    /**
     * 修改钢丝圈库存信息
     */
    @ApiOperation("修改钢丝圈库存信息")
    @RequiresPermissions("gsq:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GsqStock stock) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (stock.getId() != null) {
            ajaxResult = stockService.edit(stock);
        } else {
            ajaxResult = stockService.add(stock);
        }
        return ajaxResult;
    }

    @ApiOperation("导出钢丝圈库存信息")
    @RequiresPermissions("gsq:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GsqStock stock) throws IOException {
        List<GsqStock> list = stockService.exportList(stock);
        ExcelUtil<GsqStock> util = new ExcelUtil<>(GsqStock.class);
        String fileName = I18nUtil.getMessage("ui.gsq.stock.export.sheetName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_GSQ);
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
        String fileName = I18nUtil.getMessage("ui.gsq.stock.export.sheetName");
        ExcelUtil<GsqStockTemp> util = new ExcelUtil<>(GsqStockTemp.class);
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
    @RequiresPermissions("gsq:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GSQ,
                I18nUtil.getMessage("ui.gsq.stock.export.sheetName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<GsqStock> util = new ExcelUtil<>(GsqStock.class);
        List<GsqStock> list = util.importExcel(in);
        AjaxResult ajaxResult = stockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
