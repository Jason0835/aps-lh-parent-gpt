package com.zlt.aps.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.mdm.api.domain.dto.ProductMouldConfigurationParam;
import com.zlt.aps.mdm.api.domain.dto.ProductMouldRelationConfigurationParam;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mdm.api.domain.vo.ProductMouldInfoVo;
import com.zlt.aps.mdm.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mdm.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.mdm.service.IMdmProductModelRelationService;
import com.zlt.aps.mdm.utils.RemoteImportExcelUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductModelRelationController.java
 * 描    述：SKU与模具关系 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
@Slf4j
@Api(tags = "SKU与模具关系")
@RestController
@RequestMapping("/relation")
public class MdmProductModelRelationController extends AbstractDocBizController<MdmSkuMouldRel> {

    @Autowired
    private IMdmProductModelRelationService mdmProductModelRelationService;

    @Autowired
    private MdmProductModelRelationEntityMapper entityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 查询SKU与模具关系列表
     */
//    @RequiresPermissions("mdm:relation:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmSkuMouldRel queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmSkuMouldRel> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmSkuMouldRel> list = entityMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 根据物料获取分厂年月的匹配模具
     *
     * @param queryParam
     * @return
     */
    @ApiOperation("物料匹配的模具")
    @PostMapping("/matchMouldConfiguration")
    public ProductMouldInfoVo getProductMouldConfiguration(@RequestBody ProductMouldConfigurationParam queryParam) {
        if (null == queryParam) {
            return null;
        }
        return mdmProductModelRelationService.getProductMatchMould(queryParam);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.relation.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmSkuMouldRel billVO) {
        AjaxResult save = super.save(billVO);
        String factoryCode = billVO.getFactoryCode();
        String mainPattern = billVO.getMainPattern();
        String materialCode = billVO.getMaterialCode();
        // 把主花纹回写到物料信息表
        LambdaUpdateWrapper<MdmMaterialInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(MdmMaterialInfo::getFactoryCode, factoryCode)
                .eq(MdmMaterialInfo::getMaterialCode, materialCode)
                .set(MdmMaterialInfo::getMainPattern, mainPattern)
                .set(BaseEntity::getUpdateTime, new Date())
                .set(BaseEntity::getUpdateBy, SecurityUtils.getUsername());
        materialInfoEntityMapper.update(null, updateWrapper);
        return save;
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.relation.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 配置物料与模具关系
     *
     * @param configuration
     * @return
     */
    @ApiOperation("配置物料的模具信息")
    @PostMapping("/configurationMouldRelation")
    public AjaxResult configurationMouldRelation(@RequestBody ProductMouldRelationConfigurationParam configuration) {
        return mdmProductModelRelationService.configurationMouldRelation(configuration);
    }

    /**
     * 获取SKU与模具关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmSkuMouldRel getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SKU与模具关系数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.relation.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MdmSkuMouldRel> util = new ExcelUtil<>(this.getTClass());
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<MdmSkuMouldRel> list = util.importExcel(is);
        return this.doImportDataAsync(list, updateSupport, importLog.getId(), importLog, beginTime);
    }

    public AjaxResult doImportDataAsync(List<MdmSkuMouldRel> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        if (list.size() > 500) {
            // 传递请求头信息（主要是语言包），避免主线程执行后清空request，拷贝一个虚拟的request
            ServletRequestAttributes virtualAttr = RemoteImportExcelUtils.copyRequestHeaderAttribute();
            mdmProductModelRelationService.importDataAsync(list, updateSupport, importLogId, importLog, beginTime, virtualAttr);
            return AjaxResult.success(I18nUtil.getMessage("ui.data.column.common.importTimeOut"));
        }
        AjaxResult result = getDocService().importData(list, updateSupport, importLogId);
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(result, this.iImportErrorLogService);
        return result;
    }

    /**
     * 导出列表
     */
    @Log(title = "SKU与模具关系", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmSkuMouldRel queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmSkuMouldRel> listExportData(MdmSkuMouldRel obj) {
        QueryWrapper<MdmSkuMouldRel> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmProductModelRelationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmSkuMouldRel> queryWrapper, MdmSkuMouldRel queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCode")), "MOULD_CODE", queryVO.getFieldValueByFieldName("mouldCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isSamePatterPanel")), "IS_SAME_PATTER_PANEL", queryVO.getFieldValueByFieldName("isSamePatterPanel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCategory")), "MOULD_CATEGORY", queryVO.getFieldValueByFieldName("mouldCategory"));
        queryWrapper.exists(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), " SELECT 1 FROM T_MDM_MOULD_INFO WHERE" +
                " T_MDM_MOULD_INFO.MOULD_CODE = T_MDM_PRODUCT_MODEL_RELATION.MOULD_CODE AND MOULD_NO = {0}", queryVO.getFieldValueByFieldName("mouldNo"));
    }

    @Override
    protected String getTypeCode() {
        return "0114-1";
    }

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    public AjaxResult mesCapture() {
        return mdmProductModelRelationService.mesCapture();
    }

    /**
     * 更新主花纹到物料表
     *
     * @param queryVO 参数
     * @return 结果
     */
    @ApiOperation("更新主花纹到物料表")
    @PostMapping("/updateMainPatternToMaterial")
    public AjaxResult updateMainPatternToMaterial(@RequestBody MdmSkuMouldRel queryVO) {
        return mdmProductModelRelationService.updateMainPatternToMaterial(queryVO);
    }
}
