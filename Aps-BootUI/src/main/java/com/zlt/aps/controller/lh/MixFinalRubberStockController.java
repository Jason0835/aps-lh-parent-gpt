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
import com.zlt.aps.lh.api.domain.entity.MixFinalRubberStock;
import com.zlt.aps.lh.api.service.IMixFinalRubberStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 终炼胶库存Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@Api(tags = "终炼胶库存")
@Controller
@RequestMapping("/lh/mix/finalRubberStock")
public class MixFinalRubberStockController extends BaseController {

    private final String prefix = "lh/mix/finalRubberStock";
    @Autowired
    private IMixFinalRubberStockService iMixFinalRubberStockService;
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
        return prefix + "/finalRubberStock";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mixFinalRubberStock", new MixFinalRubberStock());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mixFinalRubberStock", iMixFinalRubberStockService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询终炼胶库存列表
     */
    @ApiOperation("根据条件查询终炼胶库存列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MixFinalRubberStock entity) {
        return iMixFinalRubberStockService.list(entity);
    }

    /**
     * 修改或新增终炼胶库存
     */
    @ApiOperation("修改或新增终炼胶库存")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MixFinalRubberStock mixFinalRubberStock) {
        AjaxResult ajaxResult = null;
        if (mixFinalRubberStock.getId() != null) {
            ajaxResult = iMixFinalRubberStockService.edit(mixFinalRubberStock);
        } else {
            ajaxResult = iMixFinalRubberStockService.add(mixFinalRubberStock);
        }
        return ajaxResult;
    }

    /**
     * 删除终炼胶库存
     */
    @ApiOperation("删除终炼胶库存（id不为空）")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMixFinalRubberStockService.remove(arr);
    }

    /**
     * 校验终炼胶库存唯一性
     */
    @ApiOperation("校验终炼胶库存唯一性")
    @PostMapping("/checkMixFinalRubberStockUnique")
    @ResponseBody
    public String checkMixFinalRubberStockUnique(MixFinalRubberStock mixFinalRubberStock) {
        return iMixFinalRubberStockService.checkMixFinalRubberStockUnique(mixFinalRubberStock);
    }

    /**
     * 导出终炼胶库存
     */
    @ApiOperation("导出终炼胶库存")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MixFinalRubberStock mixFinalRubberStock) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.finalRubberStock.export.fileName");
        List<MixFinalRubberStock> list = iMixFinalRubberStockService.getList(mixFinalRubberStock);
        ExcelUtil<MixFinalRubberStock> util = new ExcelUtil<>(MixFinalRubberStock.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mixFinalRubberStock.toString(), ApsConstant.PROCEDURE_CODE_LH);
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
        String fileName = I18nUtil.getMessage("ui.lh.finalRubberStock.modelName");
        ExcelUtil<MixFinalRubberStock> util = new ExcelUtil<>(MixFinalRubberStock.class);
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
                I18nUtil.getMessage("ui.lh.finalRubberStock.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<MixFinalRubberStock> util = new ExcelUtil<>(MixFinalRubberStock.class);
        List<MixFinalRubberStock> list = util.importExcel(file.getInputStream());
        AjaxResult ajaxResult = iMixFinalRubberStockService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
