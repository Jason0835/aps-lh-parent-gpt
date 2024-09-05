package com.zlt.aps.controller.lh;

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
import com.zlt.aps.lh.api.domain.entity.MixMasterMaterialStock;
import com.zlt.aps.lh.api.service.IMixMasterMaterialStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 母炼胶小料库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@Api(tags = "母炼胶小料库存")
@Controller
@RequestMapping("/lh/mix/masterMaterialStock")
public class MixMasterMaterialStockController extends BaseController {

    private final String prefix = "lh/mix/masterMaterialStock";
    @Autowired
    private IMixMasterMaterialStockService iMixMasterMaterialStockService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至主页面
     */
    @GetMapping()
    public String toIndex() {
        return prefix + "/masterMaterialStock";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mixMasterMaterialStock", new MixMasterMaterialStock());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mixMasterMaterialStock", iMixMasterMaterialStockService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询母炼胶小料库存列表
     */
    @ApiOperation("根据条件查询母炼胶小料库存列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MixMasterMaterialStock entity) {
        return iMixMasterMaterialStockService.list(entity);
    }

    /**
     * 修改或新增母炼胶小料库存
     */
    @ApiOperation("修改或新增母炼胶小料库存")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MixMasterMaterialStock mixMasterMaterialStock) {
        AjaxResult ajaxResult = null;
        if (mixMasterMaterialStock.getId() != null) {
            ajaxResult = iMixMasterMaterialStockService.edit(mixMasterMaterialStock);
        } else {
            ajaxResult = iMixMasterMaterialStockService.add(mixMasterMaterialStock);
        }
        return ajaxResult;
    }

    /**
     * 删除母炼胶小料库存
     */
    @ApiOperation("删除母炼胶小料库存（id不为空）")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMixMasterMaterialStockService.remove(arr);
    }

    /**
     * 校验母炼胶小料库存唯一性
     */
    @ApiOperation("校验母炼胶小料库存唯一性")
    @PostMapping("/checkMixMasterMaterialStockUnique")
    @ResponseBody
    public String checkMixMasterMaterialStockUnique(MixMasterMaterialStock mixMasterMaterialStock) {
        return iMixMasterMaterialStockService.checkMixMasterMaterialStockUnique(mixMasterMaterialStock);
    }

    /**
     * 导出母炼胶小料库存
     */
    @ApiOperation("导出母炼胶小料库存")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MixMasterMaterialStock mixMasterMaterialStock) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.masterMaterialStock.export.fileName");
        List<MixMasterMaterialStock> list = iMixMasterMaterialStockService.getList(mixMasterMaterialStock);
        ExcelUtil<MixMasterMaterialStock> util = new ExcelUtil<>(MixMasterMaterialStock.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mixMasterMaterialStock.toString(), ApsConstant.PROCEDURE_CODE_LH);
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
        String fileName = I18nUtil.getMessage("ui.lh.masterMaterialStock.modelName");
        ExcelUtil<MixMasterMaterialStock> util = new ExcelUtil<>(MixMasterMaterialStock.class);
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
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(file, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.lh.masterMaterialStock.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<MixMasterMaterialStock> util = new ExcelUtil<>(MixMasterMaterialStock.class);
        List<MixMasterMaterialStock> list = util.importExcel(file.getInputStream());
        AjaxResult ajaxResult = iMixMasterMaterialStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
