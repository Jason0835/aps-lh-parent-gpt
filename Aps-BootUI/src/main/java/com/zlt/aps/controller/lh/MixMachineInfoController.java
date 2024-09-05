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
import com.zlt.aps.lh.api.domain.entity.MixMachineInfo;
import com.zlt.aps.lh.api.service.IMixMachineInfoService;
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
 * 密炼机台信息Controller
 *
 * @author zlt
 * @date 2021-11-09
 */
@Api(tags = "密炼机台信息")
@Controller
@RequestMapping("/lh/mix/mixMachine")
public class MixMachineInfoController extends BaseController {

    private final String prefix = "lh/mix/mixMachine";
    @Autowired
    private IMixMachineInfoService iMixMachineInfoService;
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
        return prefix + "/mixMachine";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mixMachineInfo", new MixMachineInfo());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mixMachineInfo", iMixMachineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询密炼机台信息列表
     */
    @ApiOperation("根据条件查询密炼机台信息列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MixMachineInfo entity) {
        return iMixMachineInfoService.list(entity);
    }

    /**
     * 修改或新增密炼机台信息
     */
    @ApiOperation("修改或新增密炼机台信息")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MixMachineInfo mixMachineInfo) {
        AjaxResult ajaxResult = null;
        if (mixMachineInfo.getId() != null) {
            ajaxResult = iMixMachineInfoService.edit(mixMachineInfo);
        } else {
            ajaxResult = iMixMachineInfoService.add(mixMachineInfo);
        }
        return ajaxResult;
    }

    /**
     * 删除密炼机台信息
     */
    @ApiOperation("删除密炼机台信息（id不为空）")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMixMachineInfoService.remove(arr);
    }

    /**
     * 校验密炼机台信息唯一性
     */
    @ApiOperation("校验密炼机台信息唯一性")
    @PostMapping("/checkMixMachineInfoUnique")
    @ResponseBody
    public String checkMixMachineInfoUnique(MixMachineInfo mixMachineInfo) {
        return iMixMachineInfoService.checkMixMachineInfoUnique(mixMachineInfo);
    }

    /**
     * 导出密炼机台信息
     */
    @ApiOperation("导出密炼机台信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MixMachineInfo mixMachineInfo) throws IOException {
        String fileName = I18nUtil.getMessage("ui.lh.mixMachine.export.fileName");
        List<MixMachineInfo> list = iMixMachineInfoService.getList(mixMachineInfo);
        ExcelUtil<MixMachineInfo> util = new ExcelUtil<>(MixMachineInfo.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mixMachineInfo.toString(), ApsConstant.PROCEDURE_CODE_LH);
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
        String fileName = I18nUtil.getMessage("ui.lh.mixMachine.modelName");
        ExcelUtil<MixMachineInfo> util = new ExcelUtil<>(MixMachineInfo.class);
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
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(file,  ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.lh.mixMachine.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<MixMachineInfo> util = new ExcelUtil<>(MixMachineInfo.class);
        List<MixMachineInfo> list = util.importExcel(file.getInputStream());
        AjaxResult ajaxResult = iMixMachineInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
