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
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.api.service.ICxProductConstructionInfoService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 投产施工信息Controller
 *
 * @author zlt
 * @date 2021-12-02
 */
@Api(tags = "投产施工信息")
@Slf4j
@Controller
@RequestMapping("/cx/productConstruction")
public class CxProductConstructionInfoController extends BaseController {

    private final String prefix = "cx/productConstruction";
    @Autowired
    private ICxProductConstructionInfoService iCxProductConstructionInfoService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:productConstruction:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/productConstruction";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxProductConstructionInfo", new CxProductConstructionInfo());
        List<CxProductConstructionInfo> pcList = new ArrayList<>();
        mmap.put("embryoVersions", pcList);
        return prefix + "/add";
    }


    /**
     * 获取胎胚版本列表
     */
    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/getEmbryoVersions")
    @ResponseBody
    public AjaxResult getEmbryoVersions(CxProductConstructionInfo cxProductConstructionInfo) {
        cxProductConstructionInfo.setDelFlag("0");
        List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getEmbryoVersions(cxProductConstructionInfo);
        return AjaxResult.success(pcList);
    }


    /**
     * 获取胎胚版本列表
     */
    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/getVersionsByEmbryoCode")
    @ResponseBody
    public AjaxResult getVersionsByEmbryoCode(CxProductConstructionInfo cxProductConstructionInfo) {
        List<CxProductConstructionInfo> list = iCxProductConstructionInfoService.getList(cxProductConstructionInfo);
        return AjaxResult.success(list);
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxProductConstructionInfo", iCxProductConstructionInfoService.getInfo(id));
        return prefix + "/edit1";
    }


    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit1/{idAndQueryType}")
    public String edit1(@PathVariable("idAndQueryType") String idAndQueryType, ModelMap mmap) {
        String idStr = idAndQueryType.split("&")[0];
        String queryType = idAndQueryType.split("&")[1];
        Long id = Long.valueOf(idStr);
        CxProductConstructionInfo cxProductConstructionInfo = iCxProductConstructionInfoService.getInfo(id);
        cxProductConstructionInfo.setQueryType(queryType);
        mmap.put("cxProductConstructionInfo", cxProductConstructionInfo);
        List<CxProductConstructionInfo> pcList = null;
        try {
            pcList = iCxProductConstructionInfoService.getPartVersions(cxProductConstructionInfo);
        } catch (Exception e) {
            log.info("Error:get part product constructionInfo error.");
        }

        if (CollectionUtils.isEmpty(pcList)) {
            pcList = new ArrayList<CxProductConstructionInfo>();
        }
        mmap.put("versions", pcList);
        return prefix + "/edit";
    }

    /**
     * 根据条件查询投产施工信息列表
     */
    @ApiOperation("根据条件查询投产施工信息列表")
    @RequiresPermissions("cx:productConstruction:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxProductConstructionInfo entity) {
        return iCxProductConstructionInfoService.list(entity);
    }

    /**
     * 修改或新增投产施工信息
     */
    @ApiOperation("修改或新增投产施工信息")
    @RequiresPermissions("cx:productConstruction:edit")
    @PostMapping("/edit1")
    @ResponseBody
    public AjaxResult editSave1(CxProductConstructionInfo cxProductConstructionInfo) {
        AjaxResult ajaxResult = null;
        if (cxProductConstructionInfo.getId() != null) {
            ajaxResult = iCxProductConstructionInfoService.edit1(cxProductConstructionInfo);
        } else {
            ajaxResult = iCxProductConstructionInfoService.add(cxProductConstructionInfo);
        }
        return ajaxResult;
    }

    /**
     * 修改或新增投产施工信息
     */
    @ApiOperation("修改或新增投产施工信息")
    @RequiresPermissions("cx:productConstruction:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxProductConstructionInfo cxProductConstructionInfo) {
        AjaxResult ajaxResult = null;
        //唯一性校验
        if (UserConstants.NOT_UNIQUE.equals(iCxProductConstructionInfoService.checkCxProductConstructionInfoUnique(cxProductConstructionInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.construction.isEmbryoCodeExist"));
        }
        if (cxProductConstructionInfo.getId() != null) {
            ajaxResult = iCxProductConstructionInfoService.edit(cxProductConstructionInfo);
        } else {
            List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getEmbryoVersions(cxProductConstructionInfo);
            CxProductConstructionInfo pc=pcList.get(0);
            pc.setId(null);
            pc.setBaseVale(null);
            ajaxResult = iCxProductConstructionInfoService.add(pc);
        }
        return ajaxResult;
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/updateProductionStage/{id}")
    public String updateStage(@PathVariable("id") Long id, ModelMap mmap) {
        CxProductConstructionInfo cxProductConstructionInfo = iCxProductConstructionInfoService.getInfo(id);
        mmap.put("cxProductConstructionInfo", cxProductConstructionInfo);
        return prefix + "/updateStage";
    }

    /**
     * 修改或新增投产施工信息
     */
    @ApiOperation("修改生产阶段")
    @RequiresPermissions("cx:productConstruction:edit")
    @PostMapping("/updateProductionStage")
    @ResponseBody
    public AjaxResult updateProductionStage(CxProductConstructionInfo cxProductConstructionInfo) {
        AjaxResult ajaxResult = iCxProductConstructionInfoService.updateProductionStage(cxProductConstructionInfo);
        return ajaxResult;
    }

    /**
     * 删除投产施工信息
     */
    @ApiOperation("删除投产施工信息（id不为空）")
    @RequiresPermissions("cx:productConstruction:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxProductConstructionInfoService.remove(arr);
    }

    /**
     * 校验投产施工信息唯一性
     */
    @ApiOperation("校验投产施工信息唯一性")
    @PostMapping("/checkCxProductConstructionInfoUnique")
    @ResponseBody
    public String checkCxProductConstructionInfoUnique(CxProductConstructionInfo cxProductConstructionInfo) {
        return iCxProductConstructionInfoService.checkCxProductConstructionInfoUnique(cxProductConstructionInfo);
    }

    /**
     * 导出投产施工信息
     */
    @ApiOperation("导出投产施工信息")
    @RequiresPermissions("cx:productConstruction:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxProductConstructionInfo cxProductConstructionInfo) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.productConstruction.modelName");
        List<CxProductConstructionInfo> list = iCxProductConstructionInfoService.getList(cxProductConstructionInfo);
        ExcelUtil<CxProductConstructionInfo> util = new ExcelUtil<>(CxProductConstructionInfo.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxProductConstructionInfo.toString(), "ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.productConstruction.modelName");
        ExcelUtil<CxProductConstructionInfo> util = new ExcelUtil<>(CxProductConstructionInfo.class);
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
    @RequiresPermissions("cx:productConstruction:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.productConstruction.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxProductConstructionInfo> util = new ExcelUtil<>(CxProductConstructionInfo.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxProductConstructionInfo> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxProductConstructionInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
