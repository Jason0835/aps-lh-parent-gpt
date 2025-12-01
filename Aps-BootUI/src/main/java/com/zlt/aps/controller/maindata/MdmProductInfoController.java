package com.zlt.aps.controller.maindata;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.dto.ProductMouldRelationConfigurationParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import com.zlt.aps.monthplan.api.domain.vo.ConfigConstructionVo;
import com.zlt.aps.monthplan.api.domain.vo.TableProductInfoVo;
import com.zlt.aps.monthplan.api.service.IMdmProductInfoService;
import com.zlt.aps.monthplan.api.service.IMdmProductModelRelationRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 物料信息表Controller
 *
 * @author leo
 * @date 2021-08-24
 */
@Api(tags = "物料信息表")
@Controller
@RequestMapping("/lean/productinfo")
@RequiredArgsConstructor
public class MdmProductInfoController extends BaseUIController<MdmProductInfo> {

    private final IMdmProductInfoService iProductInfoService;

    private final IMdmProductModelRelationRemoteService productModelRelationRemoteService;

    private String prefix = "lean/productinfo";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lean:productinfo:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/list";
    }

    /**
     * 跳转物料查询ui界面
     */
    @RequiresPermissions("lean:productinfo:view")
    @GetMapping("/query")
    public String productinfoui() {
        return prefix + "/productinfo";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("productInfo", new MdmProductInfo());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("productInfo", iProductInfoService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询物料信息表列表
     */
    @ApiOperation("根据条件查询物料信息表列表")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TableProductInfoVo entity) {
        return iProductInfoService.list(entity);
    }

    /**
     * 根据条件查询物料信息表列表-关联模具配置、施工配置
     */
    @ApiOperation("根据条件查询物料信息表列表-关联模具配置、施工配置")
    @PostMapping("/tableList")
    @ResponseBody
    public TableDataInfo tableList(TableProductInfoVo entity) {
        return iProductInfoService.getTableList(entity);
    }

    /**
     * 修改或新增物料信息表
     */
    @ApiOperation("修改或新增物料信息表")
//    @RequiresPermissions("lean:productinfo:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmProductInfo productInfo) {
        AjaxResult ajaxResult = null;
        if (productInfo.getId() != null) {
            ajaxResult = iProductInfoService.edit(productInfo);
        } else {
            ajaxResult = iProductInfoService.add(productInfo);
        }
        return ajaxResult;
    }


    /**
     * 配置物料的模具信息
     */
    @ResponseBody
    @PostMapping("/configurationMould")
    @RequiresPermissions("lean:productinfo:configurationMould")
    @ApiOperation("配置物料的模具信息-->ZLT 20250912")
    public AjaxResult configurationMould(ProductMouldRelationConfigurationParam configuration) {
        return productModelRelationRemoteService.configurationMouldRelation(configuration);
    }

    /**
     * 删除物料信息表
     */
    @ApiOperation("删除物料信息表（id不为空）")
    @RequiresPermissions("lean:productinfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iProductInfoService.remove(arr);
    }


    @ApiOperation("校验物料信息表唯一性")
    @PostMapping("/checkProductInfoUnique")
    @ResponseBody
    public String checkProductInfoUnique(MdmProductInfo productInfo) {
        return iProductInfoService.checkProductInfoUnique(productInfo);
    }

    @ApiOperation("根据物料号获取物料信息")
    @PostMapping({"/getProductInfo"})
    @ResponseBody
    public AjaxResult getProductInfo(@RequestParam("productCode") String productCode) {
        return iProductInfoService.getProductInfo(productCode);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.mdmProductInfo.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmProductInfo> util = new ExcelUtil<>(MdmProductInfo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("物料数据导出")
    @GetMapping({"/exportData"})
    @ResponseBody
    public void export(HttpServletResponse response, TableProductInfoVo entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iProductInfoService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("物料数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iProductInfoService.importData(context, updateSupport);
    }

    @ApiOperation("物料毛利率数据导出")
    @GetMapping({"/exportGrossRate"})
    @ResponseBody
    public void exportGrossRate(HttpServletResponse response, TableProductInfoVo entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mdmProductInfo.grossRate.modelName");
        byte[] excelBytes = iProductInfoService.exportGrossRate(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping({"/importGrossRate"})
    @ResponseBody
    @ApiOperation("物料毛利率数据导入")
    public AjaxResult importGrossRate(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iProductInfoService.importGrossRate(context, false);
        return ajaxResult;
    }

    /**
     * 施工配置校验物料是否相同
     *
     * @param productConstruction 施工配置数据
     * @return 结果
     */
    @ApiOperation("施工配置校验物料是否相同")
    @PostMapping("/configurationConstructionCheck")
    @ResponseBody
    public AjaxResult configurationConstructionCheck(MdmProductConstruction productConstruction) {
        return iProductInfoService.configurationConstructionCheck(productConstruction);
    }

    /**
     * 施工配置
     *
     * @param productConstruction 施工配置数据
     * @return 结果
     */
    @RequiresPermissions("lean:productinfo:configurationConstruction")
    @ApiOperation("施工配置")
    @PostMapping("/configurationConstruction")
    @ResponseBody
    public AjaxResult configurationConstruction(MdmProductConstruction productConstruction) {
        return iProductInfoService.configurationConstruction(productConstruction);
    }

    /**
     * 查询物料对应施工列表
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("查询物料对应施工列表")
    @PostMapping("/selectConstructionCheckList")
    @ResponseBody
    public AjaxResult selectConstructionCheckList(MdmProductConstruction productConstruction) {
        return iProductInfoService.selectConstructionCheckList(productConstruction);
    }

    /**
     * 施工配置-新
     * @param configConstructionVo 配置数据
     * @return 结果
     */
    @ApiOperation("施工配置-新")
    @PostMapping("/configConstruction")
    @ResponseBody
    public AjaxResult configConstruction(ConfigConstructionVo configConstructionVo) {
        return iProductInfoService.configConstruction(configConstructionVo);
    }
}
