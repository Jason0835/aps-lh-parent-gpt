package com.zlt.aps.controller.lh;

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
import com.zlt.aps.lh.api.domain.entity.LhTireConstructionInfo;
import com.zlt.aps.lh.api.service.ILhTireConstructionInfoService;
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
 * 硫化外胎施工信息Controller
 * @author zlt
 * @date 2021-11-15
 */
@Api(tags = "硫化外胎施工信息")
@Controller
@RequestMapping("/lh/lhTireConstructionInfo")
public class LhTireConstructionInfoController extends BaseController {

    @Autowired
    private ILhTireConstructionInfoService iLhTireConstructionInfoService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "lh/lhTireConstructionInfo";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:lhTireConstructionInfo:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/lhTireConstructionInfo";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("lhTireConstructionInfo", new LhTireConstructionInfo());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhTireConstructionInfo", iLhTireConstructionInfoService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询硫化外胎施工信息列表
     */
    @ApiOperation("根据条件查询硫化外胎施工信息列表")
    @RequiresPermissions("lh:lhTireConstructionInfo:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhTireConstructionInfo entity) {
        return iLhTireConstructionInfoService.list(entity);
    }

    /**
     * 修改或新增硫化外胎施工信息
     */
    @ApiOperation("修改或新增硫化外胎施工信息")
    @RequiresPermissions("lh:lhTireConstructionInfo:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhTireConstructionInfo lhTireConstructionInfo) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(iLhTireConstructionInfoService.checkLhTireConstructionInfoUnique(lhTireConstructionInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mouthPlate.alreadyExists"));
        }
        if (lhTireConstructionInfo.getId() != null){
            ajaxResult = iLhTireConstructionInfoService.edit(lhTireConstructionInfo);
        } else{
            ajaxResult = iLhTireConstructionInfoService.add(lhTireConstructionInfo);
        }
        return ajaxResult;
    }

    /**
     * 删除硫化外胎施工信息
     */
    @ApiOperation("删除硫化外胎施工信息（id不为空）")
    @RequiresPermissions("lh:lhTireConstructionInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhTireConstructionInfoService.remove(arr);
    }

    /**
     * 校验硫化外胎施工信息唯一性
     */
    @ApiOperation("校验硫化外胎施工信息唯一性")
    @PostMapping("/checkLhTireConstructionInfoUnique")
    @ResponseBody
    public String checkLhTireConstructionInfoUnique(LhTireConstructionInfo lhTireConstructionInfo) {
        return iLhTireConstructionInfoService.checkLhTireConstructionInfoUnique(lhTireConstructionInfo);
    }

    /**
     * 导出硫化外胎施工信息
     */
    @ApiOperation("导出硫化外胎施工信息")
    @RequiresPermissions("lh:lhTireConstructionInfo:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,LhTireConstructionInfo lhTireConstructionInfo) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lhTireConstructionInfo.modelName");
        List<LhTireConstructionInfo> list = iLhTireConstructionInfoService.getList(lhTireConstructionInfo);
        ExcelUtil<LhTireConstructionInfo> util = new ExcelUtil<>(LhTireConstructionInfo. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhTireConstructionInfo.toString(),ApsConstant.PROCEDURE_CODE_LH);
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
        String fileName = I18nUtil.getMessage("ui.data.column.lhTireConstructionInfo.modelName");
        ExcelUtil<LhTireConstructionInfo> util = new ExcelUtil<>(LhTireConstructionInfo.class);
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
    @RequiresPermissions("lh:lhTireConstructionInfo:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.data.column.lhTireConstructionInfo.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<LhTireConstructionInfo> util = new ExcelUtil<>(LhTireConstructionInfo.class);
        InputStream in = new ByteArrayInputStream(data);
        List<LhTireConstructionInfo> list = util.importExcel(in);
        AjaxResult ajaxResult = iLhTireConstructionInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据sap查询对应的胎胚代码
     * @param lhTireConstructionInfo sap品号
     * @return 查询到的胎胚代码
     */
    @ApiOperation("根据sap查询对应的胎胚代码")
    @PostMapping("/getEmbryoCodeListBySapCode")
    @ResponseBody
    public AjaxResult getEmbryoCodeListBySapCode(LhTireConstructionInfo lhTireConstructionInfo) {
        List<LhTireConstructionInfo> list = iLhTireConstructionInfoService.getEmbryoCodeListBySapCode(lhTireConstructionInfo);
        return AjaxResult.success(list);
    }
}
