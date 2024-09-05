package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import com.zlt.aps.cx.api.service.ICxMachineGroupListService;

/**
 * 组别机台列Controller
 * @author zlt
 * @date 2021-12-16
 */
@Api(tags = "组别机台列")
@Controller
@RequestMapping("/cx/groupMachineList")
public class CxMachineGroupListController extends BaseController {

    @Autowired
    private ICxMachineGroupListService iCxMachineGroupListService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/groupMachineList";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:groupMachineList:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/groupMachineList";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add/{id}")
    public String add(@PathVariable("id") Long pid, ModelMap mmap) {
        CxMachineGroupList cxMachineGroupList=  new CxMachineGroupList();
        cxMachineGroupList.setGroupId(pid);
        mmap.put("cxMachineGroupList",cxMachineGroupList);
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxMachineGroupList", iCxMachineGroupListService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询组别机台列列表
     */
    @ApiOperation("根据条件查询组别机台列列表")
    @RequiresPermissions("cx:groupMachineList:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxMachineGroupList entity) {
        return iCxMachineGroupListService.list(entity);
    }

    /**
     * 修改或新增组别机台列
     */
    @ApiOperation("修改或新增组别机台列")
    @RequiresPermissions("cx:groupMachineList:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxMachineGroupList cxMachineGroupList) {
        AjaxResult ajaxResult = null;
        if (cxMachineGroupList.getId() != null){
            ajaxResult = iCxMachineGroupListService.edit(cxMachineGroupList);
        } else{
            ajaxResult = iCxMachineGroupListService.add(cxMachineGroupList);
        }
        return ajaxResult;
    }

    /**
     * 删除组别机台列
     */
    @ApiOperation("删除组别机台列（id不为空）")
    @RequiresPermissions("cx:groupMachineList:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxMachineGroupListService.remove(arr);
    }

    /**
     * 校验组别机台列唯一性
     */
    @ApiOperation("校验组别机台列唯一性")
    @PostMapping("/checkCxMachineGroupListUnique")
    @ResponseBody
    public List<CxMachineGroupList> checkCxMachineGroupListUnique(CxMachineGroupList cxMachineGroupList) {
        return iCxMachineGroupListService.checkCxMachineGroupListUnique(cxMachineGroupList);
    }

    /**
     * 导出组别机台列
     */
    @ApiOperation("导出组别机台列")
    @RequiresPermissions("cx:groupMachineList:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,CxMachineGroupList cxMachineGroupList) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.groupMachineList.modelName");
        List<CxMachineGroupList> list = iCxMachineGroupListService.getList(cxMachineGroupList);
        ExcelUtil<CxMachineGroupList> util = new ExcelUtil<>(CxMachineGroupList. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxMachineGroupList.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.groupMachineList.modelName");
        ExcelUtil<CxMachineGroupList> util = new ExcelUtil<>(CxMachineGroupList.class);
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
    @RequiresPermissions("cx:groupMachineList:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.groupMachineList.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxMachineGroupList> util = new ExcelUtil<>(CxMachineGroupList.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxMachineGroupList> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxMachineGroupListService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
