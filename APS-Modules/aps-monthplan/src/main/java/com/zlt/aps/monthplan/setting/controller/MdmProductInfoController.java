package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MdmProductInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductInfo;
import com.zlt.aps.monthplan.api.domain.vo.ConfigConstructionVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductInfoGrossRateVo;
import com.zlt.aps.monthplan.api.domain.vo.TableProductInfoVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ExcelReadUtils;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 物料信息表Controller
 *
 * @author leo
 * @date 2021-08-24
 */
@RestController
@Api(tags = "基础数据-物料信息")
@RequestMapping("/productinfo")
public class MdmProductInfoController extends AbstractDocBizController<MdmProductInfo> {
    @Autowired
    private IMdmProductInfoService iproductInfoService;
    @Resource
    private MdmProductInfoEntityMapper mdmProductInfoEntityMapper;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 查询物料信息表列表
     */
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmProductInfo productInfo) {
        startPage("create_time desc");
        QueryWrapper<MdmProductInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, productInfo);
        List<MdmProductInfo> list = iproductInfoService.selectList(wrapper);
        return getDataTable(list);
    }

    /**
     * 查询物料信息表列表
     * 关联查询配置模具，配置施工
     *
     * @param productInfo
     */
    @PostMapping("/getTableList")
    @ApiOperation("根据条件查询物料信息-关联查询配置模具，配置施工")
    public TableDataInfo getTableList(@RequestBody TableProductInfoVo productInfo) {
        startPage("create_time desc");
        List<TableProductInfoVo> list = iproductInfoService.getList(productInfo);
        return getDataTable(list);
    }

    /**
     * 导出物料信息表列表
     */
    @Log(title = "ui.data.column.productinfo.title", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TableProductInfoVo productInfo) throws IOException {
        List<TableProductInfoVo> list = iproductInfoService.getList(productInfo);
        List<MdmProductInfo> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            result = BeanCopyUtils.copyBeanList(list, MdmProductInfo.class);
        }
        ExcelUtil<MdmProductInfo> util = new ExcelUtil<>(MdmProductInfo.class);
        util.exportExcel(response, result, "物料信息表数据");
    }

    /**
     * 获取物料信息表详细信息
     */
//    @PreAuthorize(hasPermi = "lean:productinfo:query")
    @Override
    @GetMapping(value = "/{id}")
    public MdmProductInfo getInfo(@PathVariable("id") Long id) {
        return baseDao.selectById(MdmProductInfo.class, id);
    }

    /**
     * 新增物料信息表
     */
//    @PreAuthorize(hasPermi = "lean:productinfo:add")
    @PostMapping("/add")
    @Log(title = "ui.data.column.productinfo.title", businessType = BusinessType.INSERT)
    public AjaxResult add(@RequestBody MdmProductInfo productInfo) {
        String unique = iproductInfoService.checkProductInfoUnique(productInfo);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmProductInfo.exist"));
        }
        iproductInfoService.transformToJsonField(Collections.singletonList(productInfo));
        return toAjax(baseDao.insert(productInfo));
    }

    /**
     * 修改物料信息表
     */
//    @PreAuthorize(hasPermi = "lean:productinfo:edit")
    @PostMapping("/edit")
    @Log(title = "ui.data.column.productinfo.title", businessType = BusinessType.UPDATE)
    public AjaxResult edit(@RequestBody MdmProductInfo productInfo) {
        String unique = iproductInfoService.checkProductInfoUnique(productInfo);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmProductInfo.exist"));
        }
        iproductInfoService.transformToJsonField(Collections.singletonList(productInfo));
        return toAjax(baseDao.update(productInfo));
    }

    /**
     * 删除物料信息表
     */
//    @PreAuthorize(hasPermi = "lean:productinfo:remove")
    @DeleteMapping("/{ids}")
    @Log(title = "ui.data.column.productinfo.title", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(baseDao.deleteByIds(MdmProductInfo.class, Arrays.asList(ids)));
    }

    /**
     * 物料信息表列表
     */
    @PostMapping("/getList")
    @Log(title = "ui.data.column.productinfo.title", businessType = BusinessType.EXPORT)
    @Override
    public List<MdmProductInfo> getList(@RequestBody MdmProductInfo productInfo) {
        startPage("create_time desc");
        QueryWrapper<MdmProductInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, productInfo);
        List<MdmProductInfo> list = iproductInfoService.selectList(wrapper);
        return list;
    }

    /**
     * 校验物料信息表唯一性
     */
    @ApiOperation("校验物料信息表唯一性")
    @PostMapping("/checkProductInfoUnique")
    public String checkProductInfoUnique(@RequestBody MdmProductInfo productInfo) {
        return iproductInfoService.checkProductInfoUnique(productInfo);
    }

    @ApiOperation("获取物料信息")
    @PostMapping("/getProductInfo")
    public AjaxResult getProductInfo(@RequestParam("productCode") String productCode) {
        // 查询物料信息
        QueryWrapper<MdmProductInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("PRODUCT_CODE", productCode);
        MdmProductInfo productInfo = mdmProductInfoEntityMapper.selectOne(wrapper);
        if (productInfo != null) {
            return AjaxResult.success(productInfo);
        }
        return AjaxResult.success();
    }

    /**
     * 根据集合导入物料信息数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmProductInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 根据集合导入物料信息数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmProductInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入物料毛利率数据")
    @PostMapping("/importGrossRate")
    public AjaxResult importGrossRate(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<ProductInfoGrossRateVo> util = new ExcelUtil<>(ProductInfoGrossRateVo.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<ProductInfoGrossRateVo> list = util.importExcel(is);
        AjaxResult ajaxResult = iproductInfoService.importGrossRate(list, updateSupport, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 导出列表
     */
    @Log(title = "物料信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData2/{fileName}")
    public byte[] exportData(@RequestBody TableProductInfoVo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<TableProductInfoVo> list = iproductInfoService.getList(queryVO);
        List<MdmProductInfo> result = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            result = BeanCopyUtils.copyBeanList(list, MdmProductInfo.class);
        }
        ExcelUtil<MdmProductInfo> util = new ExcelUtil(MdmProductInfo.class);
        Workbook workbook = util.exportExcel2(response, result, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    /**
     * 导出毛利率列表
     */
    @Log(title = "物料信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出毛利率列表")
    @PostMapping("/exportGrossRate/{fileName}")
    public byte[] exportGrossRate(@RequestBody TableProductInfoVo queryVO, @PathVariable("fileName") String fileName,
                                  HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<TableProductInfoVo> list = iproductInfoService.getList(queryVO);
        List<ProductInfoGrossRateVo> rateVoList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list)) {
            rateVoList = BeanCopyUtils.copyBeanList(list, ProductInfoGrossRateVo.class);
        }
//        List<ProductInfoGrossRateVo> rateVoList = BeanCopyUtils.copyBeanList(list, ProductInfoGrossRateVo.class);
        ExcelUtil<ProductInfoGrossRateVo> util = new ExcelUtil<>(ProductInfoGrossRateVo.class);
        Workbook workbook = util.exportExcel2(response, rateVoList, fileName);
        byte[] resultBytes = ExcelReadUtils.writeExcel(workbook);
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
        return resultBytes;
    }

    @Override
    protected List<MdmProductInfo> listExportData(MdmProductInfo obj) {
        QueryWrapper<MdmProductInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return iproductInfoService.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return iproductInfoService;
    }

    @Override
    protected String getTypeCode() {
        return "0102";
    }

    @Override
    protected void builderCondition(QueryWrapper<MdmProductInfo> queryWrapper, MdmProductInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesProductCode")), "MES_PRODUCT_CODE", queryVO.getFieldValueByFieldName("mesProductCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCategory")), "MOULD_CATEGORY", queryVO.getFieldValueByFieldName("mouldCategory"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireType")), "TIRE_TYPE", queryVO.getFieldValueByFieldName("tireType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("commonType")), "COMMON_TYPE", queryVO.getFieldValueByFieldName("commonType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("replaceGroup")), "REPLACE_GROUP", queryVO.getFieldValueByFieldName("replaceGroup"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cantProduce")), "CANT_PRODUCE", queryVO.getFieldValueByFieldName("cantProduce"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("noDelivery")), "NO_DELIVERY", queryVO.getFieldValueByFieldName("noDelivery"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("speed")), "SPEED", queryVO.getFieldValueByFieldName("speed"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("ability")), "ABILITY", queryVO.getFieldValueByFieldName("ability"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("environmentProtection")), "ENVIRONMENT_PROTECTION", queryVO.getFieldValueByFieldName("environmentProtection"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("authentication")), "AUTHENTICATION", queryVO.getFieldValueByFieldName("authentication"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productGroupCode")), "PRODUCT_GROUP_CODE", queryVO.getFieldValueByFieldName("productGroupCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("forbidTag")), "FORBID_TAG", queryVO.getFieldValueByFieldName("forbidTag"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("singleTireWeight")), "SINGLE_TIRE_WEIGHT", queryVO.getFieldValueByFieldName("singleTireWeight"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldClampingPressure")), "MOULD_CLAMPING_PRESSURE", queryVO.getFieldValueByFieldName("mouldClampingPressure"));

        boolean isTireTypeNullData = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isTireTypeNullData"));
        queryWrapper.apply(isTireTypeNullData && ApsConstant.APS_STRING_1.equals(queryVO.getIsTireTypeNullData()), " TIRE_TYPE IS NULL OR TIRE_TYPE = ''  ");
        boolean isCommonTypeNullData = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isCommonTypeNullData"));
        queryWrapper.apply(isCommonTypeNullData && ApsConstant.APS_STRING_1.equals(queryVO.getIsCommonTypeNullData()), " COMMON_TYPE IS NULL OR COMMON_TYPE = '' ");
        boolean isBrandNullData = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isBrandNullData"));
        queryWrapper.apply(isBrandNullData && ApsConstant.APS_STRING_1.equals(queryVO.getIsBrandNullData()), " BRAND IS NULL OR BRAND = '' ");
    }

    /**
     * 查询物料对应施工列表
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("查询物料对应施工列表")
    @PostMapping("/selectConstructionCheckList")
    public AjaxResult selectConstructionCheckList(@RequestBody MdmProductConstruction productConstruction) {
        return iproductInfoService.selectConstructionCheckList(productConstruction);
    }

    /**
     * 施工配置-新
     * @param configConstructionVo 配置数据
     * @return 结果
     */
    @ApiOperation("施工配置-新")
    @PostMapping("/configConstruction")
    public AjaxResult configConstruction(@RequestBody ConfigConstructionVo configConstructionVo) {
        return iproductInfoService.configConstruction(configConstructionVo);
    }

    /**
     * 施工配置校验物料是否相同
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("施工配置校验物料是否相同")
    @PostMapping("/configurationConstructionCheck")
    public AjaxResult configurationConstructionCheck(@RequestBody MdmProductConstruction productConstruction) {
        return iproductInfoService.configurationConstructionCheck(productConstruction);
    }

    /**
     * 施工配置
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("施工配置")
    @PostMapping("/configurationConstruction")
    public AjaxResult configurationConstruction(@RequestBody MdmProductConstruction productConstruction) {
        return iproductInfoService.configurationConstruction(productConstruction);
    }
}
