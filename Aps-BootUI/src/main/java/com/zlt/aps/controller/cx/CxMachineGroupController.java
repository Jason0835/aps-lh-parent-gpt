package com.zlt.aps.controller.cx;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroup;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupForExcel;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import com.zlt.aps.cx.api.service.ICxMachineGroupListService;
import com.zlt.aps.cx.api.service.ICxMachineGroupService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
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
import java.util.stream.Collectors;

/**
 * 成型机组Controller
 *
 * @author zlt
 * @date 2021-12-16
 */
@Api(tags = "成型机组")
@Controller
@RequestMapping("/cx/machineGroup")
public class CxMachineGroupController extends BaseController {

    @Autowired
    private ICxMachineGroupService iCxMachineGroupService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private ICxMachineGroupListService iCxMachineGroupListService;

    private final String prefix = "cx/machineGroup";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:machineGroup:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machineGroup";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxMachineGroup", new CxMachineGroup());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        CxMachineGroup cxMachineGroup=iCxMachineGroupService.getInfo(id);
        mmap.put("cxMachineGroup", cxMachineGroup);
        String selectedDataStr ="";
        if(CollectionUtils.isNotEmpty(cxMachineGroup.getCxMachineGroupListList())){
            List<String> cxMachineCodes = cxMachineGroup.getCxMachineGroupListList().stream().map(CxMachineGroupList::getCxMachineCode).collect(Collectors.toList());
            selectedDataStr= String.join(",", cxMachineCodes);
        }
        mmap.put("selectedDataStr", selectedDataStr);
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型机组列表
     */
    @ApiOperation("根据条件查询成型机组列表")
    @RequiresPermissions("cx:machineGroup:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxMachineGroup entity) {
        return iCxMachineGroupService.list(entity);
    }

    /**
     * 修改或新增成型机组
     */
    @ApiOperation("修改或新增成型机组")
    @RequiresPermissions("cx:machineGroup:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxMachineGroup cxMachineGroup) {
        AjaxResult ajaxResult = null;
        if (cxMachineGroup.getId() != null) {
            ajaxResult = iCxMachineGroupService.edit(cxMachineGroup);
        } else {
            ajaxResult = iCxMachineGroupService.add(cxMachineGroup);
        }
        return ajaxResult;
    }

    /**
     * 删除成型机组
     */
    @ApiOperation("删除成型机组（id不为空）")
    @RequiresPermissions("cx:machineGroup:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxMachineGroupService.remove(arr);
    }

    /**
     * 导出成型机组
     */
    @ApiOperation("导出成型机组")
    @RequiresPermissions("cx:machineGroup:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxMachineGroup cxMachineGroup) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.machineGroup.modelName");
        List<CxMachineGroupForExcel> list = iCxMachineGroupService.selectCxMachineGroup4Excel(cxMachineGroup);
        ExcelUtil<CxMachineGroupForExcel> util = new ExcelUtil<>(CxMachineGroupForExcel.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxMachineGroup.toString(), "ApsConstant.PROCEDURE_CODE_XXX");
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
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.machineGroup.modelName");
        ExcelUtil<CxMachineGroupForExcel> util = new ExcelUtil<>(CxMachineGroupForExcel.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:machineGroup:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.machineGroup.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxMachineGroupForExcel> util = new ExcelUtil<>(CxMachineGroupForExcel.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxMachineGroupForExcel> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxMachineGroupService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    @PostMapping("/getDetailById")
    @ResponseBody
    public TableDataInfo getDetailById(CxMachineGroup cxMachineGroup) {
        return iCxMachineGroupService.getDetailById(cxMachineGroup);
    }

    /**
     * 配置列表页面
     */
    @GetMapping("/detail/{idAndGroupName}")
    public String detail(@PathVariable("idAndGroupName") String idAndGroupName, ModelMap mmap) {
        Long id=Long.valueOf(idAndGroupName.split("&")[0]);
        String groupName=idAndGroupName.split("&")[1];
        mmap.put("groupId", id);
        mmap.put("groupName", groupName);
        return "cx/groupMachineList/groupMachineList";
    }

    @ApiOperation("校验组名唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public AjaxResult checkUnique(CxMachineGroup cxMachineGroup) {

        //校验机台组名称唯一性
        CxMachineGroup query=new CxMachineGroup();
        query.setId(cxMachineGroup.getId());
        query.setGroupName(cxMachineGroup.getGroupName());
        String isUnique=iCxMachineGroupService.checkCxMachineGroupUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(isUnique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machineGroup.groupNameUnique"));
        }

        //校验机台在子表的唯一新
        String[] machineCodes=cxMachineGroup.getMachineCodes();
        if(machineCodes!=null && machineCodes.length>0){
            String machineNames="";
            for(String machineCode:machineCodes){
                CxMachineGroupList mgl=new CxMachineGroupList();
                if(cxMachineGroup.getId()!=null){
                    mgl.setGroupId(cxMachineGroup.getId());
                }
                mgl.setCxMachineCode(machineCode);
                List<CxMachineGroupList> existList=iCxMachineGroupListService.checkCxMachineGroupListUnique(mgl);
                if(CollectionUtils.isNotEmpty(existList)){
                    if(StringUtils.isNotBlank(machineNames)){
                        machineNames=machineNames+"，"+existList.get(0).getCxMachineName();
                    }else{
                        machineNames=existList.get(0).getCxMachineName();
                    }
                }
            }
            if(StringUtils.isNotBlank(machineNames)){
                String errorMsg=String.format(I18nUtil.getMessage("ui.data.column.machineGroup.conflictRecord"),machineNames);
                return AjaxResult.error(errorMsg);
            }
        }
        return AjaxResult.success();
    }

}
