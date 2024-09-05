package com.zlt.aps.controller.tc;

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
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.service.ITcStockService;
import com.zlt.aps.template.tc.TcStockTemp;
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
 * 胎侧库存信息Controller
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/tc/stock")
@Api(tags = {"胎侧胶库存信息维护接口"})
public class TcStockController extends BaseController {
    private String prefix = "tc/stock";

    @Autowired
    private ITcStockService tcStockService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至胎侧库存列表页面
     */
    @RequiresPermissions("tc:stock:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/stock";
    }

    /**
     * 跳转至新增胎侧库存页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tcStock", new TcStock());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    /**
     * 跳转至修改胎侧库存页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcStock", tcStockService.selectTcStockById(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    /**
     * 跳转至胎侧库存修正页面
     */
    @GetMapping("/modifyStock/{id}")
    public String modifyStock(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tcStock", tcStockService.selectTcStockById(id));
        mmap.put("editType", "2");
        return prefix + "/edit";
    }

    /**
     * 胎侧库存信息列表
     */
    @ApiOperation("查询胎侧胶库存信息列表")
    @RequiresPermissions("tc:stock:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcStock tcStock) {
        return tcStockService.list(tcStock);
    }

    /**
     * 删除胎侧库存信息
     */
    @ApiOperation("删除胎侧胶库存信息")
    @RequiresPermissions("tc:stock:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return tcStockService.remove(arr);
    }

    /**
     * 新增胎侧库存信息
     */
    @ApiOperation("新增胎侧胶库存信息")
    @RequiresPermissions("tc:stock:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(TcStock tcStock) {
        return tcStockService.add(tcStock);
    }

    /**
     * 修改胎侧库存信息
     */
    @ApiOperation("修改胎侧胶库存信息")
    @RequiresPermissions("tc:stock:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(TcStock tcStock) {
        return tcStockService.edit(tcStock);
    }

    @ApiOperation("导出胎侧库存信息")
    @RequiresPermissions("tc:stock:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TcStock stock) throws IOException {
        List<TcStock> list = tcStockService.exportList(stock);
        ExcelUtil<TcStock> util = new ExcelUtil(TcStock.class);
        String fileName = I18nUtil.getMessage("ui.tc.stock.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(), ApsConstant.PROCEDURE_CODE_TC);
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
        String fileName = I18nUtil.getMessage("ui.tc.stock.export.fileName");
        ExcelUtil<TcStockTemp> util = new ExcelUtil<>(TcStockTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @throws Exception
     */
    @RequiresPermissions("tc:stock:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC,
                I18nUtil.getMessage("ui.tc.stock.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TcStock> util = new ExcelUtil<>(TcStock.class);
        List<TcStock> list = util.importExcel(in);
        AjaxResult ajaxResult = tcStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
