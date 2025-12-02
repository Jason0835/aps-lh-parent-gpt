package com.zlt.aps.controller.cx;

import com.alibaba.nacos.common.utils.CollectionUtils;
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
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductStockLimit;
import com.zlt.aps.cxlh.cx.api.service.ICxProductStockLimitService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成型投产班次库存限定设置Controller
 * @author zlt
 * @date 2022-01-07
 */
@Api(tags = "成型投产班次库存限定设置")
@Controller
@RequestMapping("/cx/shiftLimit")
public class CxProductStockLimitController extends BaseController {

    @Autowired
    private ICxProductStockLimitService iCxProductStockLimitService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/shiftLimit";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:shiftLimit:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/shiftLimit";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxProductStockLimit", new CxProductStockLimit());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxProductStockLimit", iCxProductStockLimitService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型投产班次库存限定设置列表
     */
    @ApiOperation("根据条件查询成型投产班次库存限定设置列表")
    @RequiresPermissions("cx:shiftLimit:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxProductStockLimit entity) {
        return iCxProductStockLimitService.list(entity);
    }

    /**
     * 修改或新增成型投产班次库存限定设置
     */
    @ApiOperation("修改或新增成型投产班次库存限定设置")
    @RequiresPermissions("cx:shiftLimit:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxProductStockLimit cxProductStockLimit) {
        AjaxResult ajaxResult = null;
        //唯一性校验
        if( CollectionUtils.isNotEmpty(iCxProductStockLimitService.checkCxProductStockLimitUnique(cxProductStockLimit))){
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique"));
        }
        CxProductStockLimit query=new CxProductStockLimit();
        query.setId(cxProductStockLimit.getId());
        query.setType(cxProductStockLimit.getType());
        List<CxProductStockLimit> checkList=iCxProductStockLimitService.checkCxProductStockLimitUnique(query);
        if(CollectionUtils.isNotEmpty(checkList)){
            Map<String,Long> typeMap=checkList.stream().collect(Collectors.toMap(a->a.getLimitType(), a->a.getStockNum()));
            Long stockNum=cxProductStockLimit.getStockNum();
            //库存上限校验
            if ("1".equals(cxProductStockLimit.getLimitType())){
                if(typeMap.get("2")!=null && stockNum<typeMap.get("2")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique2"));
                }
                if(typeMap.get("3")!=null && stockNum<typeMap.get("3")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique3"));
                }
                if(typeMap.get("4")!=null && stockNum<typeMap.get("4")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique4"));
                }
            }
            //库存上限预警值校验
            if ("2".equals(cxProductStockLimit.getLimitType())){
                if(typeMap.get("1")!=null && stockNum>typeMap.get("1")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique5"));
                }
                if(typeMap.get("3")!=null && stockNum<typeMap.get("3")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique6"));
                }
                if(typeMap.get("4")!=null && stockNum<typeMap.get("4")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique7"));
                }
            }
            //库存下限校验
            if ("3".equals(cxProductStockLimit.getLimitType())){
                if(typeMap.get("1")!=null && stockNum>typeMap.get("1")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique8"));
                }
                if(typeMap.get("2")!=null && stockNum>typeMap.get("2")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique9"));
                }
                if(typeMap.get("4")!=null && stockNum>typeMap.get("4")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique10"));
                }
            }
            //库存下限预警值校验
            if ("4".equals(cxProductStockLimit.getLimitType())){
                if(typeMap.get("1")!=null && stockNum>typeMap.get("1")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique11"));
                }
                if(typeMap.get("2")!=null && stockNum>typeMap.get("2")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique12"));
                }
                if(typeMap.get("3")!=null && stockNum<typeMap.get("3")){
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.column.shiftLimit.checkUnique13"));
                }
            }
        }
        if (cxProductStockLimit.getId() != null){
            ajaxResult = iCxProductStockLimitService.edit(cxProductStockLimit);
        } else{
            ajaxResult = iCxProductStockLimitService.add(cxProductStockLimit);
        }
        return ajaxResult;
    }

    /**
     * 删除成型投产班次库存限定设置
     */
    @ApiOperation("删除成型投产班次库存限定设置（id不为空）")
    @RequiresPermissions("cx:shiftLimit:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxProductStockLimitService.remove(arr);
    }

    /**
     * 校验成型投产班次库存限定设置唯一性
     */
    @ApiOperation("校验成型投产班次库存限定设置唯一性")
    @PostMapping("/checkCxProductStockLimitUnique")
    @ResponseBody
    public String checkCxProductStockLimitUnique(CxProductStockLimit cxProductStockLimit) {
        return "";
    }

    /**
     * 导出成型投产班次库存限定设置
     */
    @ApiOperation("导出成型投产班次库存限定设置")
    @RequiresPermissions("cx:shiftLimit:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,CxProductStockLimit cxProductStockLimit) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.shiftLimit.modelName");
        List<CxProductStockLimit> list = iCxProductStockLimitService.getList(cxProductStockLimit);
        ExcelUtil<CxProductStockLimit> util = new ExcelUtil<>(CxProductStockLimit. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxProductStockLimit.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.shiftLimit.modelName");
        ExcelUtil<CxProductStockLimit> util = new ExcelUtil<>(CxProductStockLimit.class);
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
    @RequiresPermissions("cx:shiftLimit:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.shiftLimit.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxProductStockLimit> util = new ExcelUtil<>(CxProductStockLimit.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxProductStockLimit> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxProductStockLimitService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
