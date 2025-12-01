package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.mapper.MdmModelInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductModelRelationEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductModelRelationService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.dto.ProductMouldConfigurationParam;
import com.zlt.aps.monthplan.api.domain.dto.ProductMouldRelationConfigurationParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductModelRelation;
import com.zlt.aps.monthplan.api.domain.vo.ProductMouldInfoVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductModelRelationController.java
 * 描    述：SAP与模具关系 控制层类：....
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
@Api(tags = "SAP与模具关系")
@RestController
@RequestMapping("/relation")
public class MdmProductModelRelationController extends AbstractDocBizController<MdmProductModelRelation> {

    @Autowired
    private IMdmProductModelRelationService mdmProductModelRelationService;

    @Autowired
    private MdmProductModelRelationEntityMapper entityMapper;

    @Autowired
    private MdmProductConstructionEntityMapper mdmProductConstructionEntityMapper;

    @Autowired
    private MdmModelInfoEntityMapper mdmModelInfoEntityMapper;

    /**
     * 查询SAP与模具关系列表
     */
//    @RequiresPermissions("maindata:relation:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmProductModelRelation queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmProductModelRelation> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmProductModelRelation> list = entityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            // 根据物料号、规格代码查询施工表，取成型法
            Map<String, String> productConstructionMap = new HashMap<>(16);
            Map<String, MdmModelInfo> modelMap = new HashMap<>(16);
            List<MdmProductConstruction> productConstructionList = new ArrayList<>();
            List<MdmModelInfo> modelList = new ArrayList<>();
            List<List<MdmProductModelRelation>> splitList = ScmListUtils.getSplitList(list, 500);
            for (List<MdmProductModelRelation> relationList : splitList) {
                List<String> uniqueKeyList = relationList.stream().map(item -> String.join("|", item.getProductCode(), item.getSpecCode())).collect(Collectors.toList());
                List<String> mouldCodeList = relationList.stream().map(MdmProductModelRelation::getMouldCode).collect(Collectors.toList());
                LambdaQueryWrapper<MdmProductConstruction> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.apply(" CONCAT(PRODUCT_CODE, '|', SPEC_CODE) IN('{0}')", String.join("','", uniqueKeyList));
                productConstructionList.addAll(mdmProductConstructionEntityMapper.queryByProductCodeAndSpecCodes(queryVO.getFactoryCode(), uniqueKeyList));

                LambdaQueryWrapper<MdmModelInfo> mouldQueryWrapper = new LambdaQueryWrapper<>();
                mouldQueryWrapper.eq(MdmModelInfo::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);
                mouldQueryWrapper.in(MdmModelInfo::getMouldCode, mouldCodeList);
                modelList.addAll(mdmModelInfoEntityMapper.selectList(mouldQueryWrapper));
            }
            if (CollectionUtils.isNotEmpty(productConstructionList)) {
                productConstructionMap = productConstructionList.stream().collect(Collectors
                        .toMap(item -> String.join("|", StringUtils.defaultIfBlank(item.getProductCode(), ""),
                                        StringUtils.defaultIfBlank(item.getSpecCode(), "")),
                                item -> StringUtils.defaultIfBlank(item.getMouldMethod(), ""), (v1, v2) -> v1));
            }
            if (CollectionUtils.isNotEmpty(modelList)) {
                modelMap = modelList.stream().collect(Collectors.toMap(MdmModelInfo::getMouldCode, Function.identity(), (v1, v2) -> v1));
            }
            for (MdmProductModelRelation mdmProductModelRelation : list) {
                String mapKey = String.join("|", StringUtils.defaultIfBlank(mdmProductModelRelation.getProductCode(), "")
                        , StringUtils.defaultIfBlank(mdmProductModelRelation.getSpecCode(), ""));
                if (productConstructionMap.containsKey(mapKey)) {
                    String mouldMethod = productConstructionMap.get(mapKey);
                    mdmProductModelRelation.setMouldMethod(mouldMethod);
                }
                String mouldCode = mdmProductModelRelation.getMouldCode();
                if (modelMap.containsKey(mouldCode)) {
                    MdmModelInfo mdmModelInfo = modelMap.get(mouldCode);
                    mdmProductModelRelation.setMouldNo(mdmModelInfo.getMouldNo());
                }
            }
        }
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
    public AjaxResult save(@RequestBody MdmProductModelRelation billVO) {
        return super.save(billVO);
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
     * 获取SAP与模具关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmProductModelRelation getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SAP与模具关系数据
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
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "SAP与模具关系", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmProductModelRelation queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmProductModelRelation> listExportData(MdmProductModelRelation obj) {
        QueryWrapper<MdmProductModelRelation> wrapper = new QueryWrapper<>();
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
    protected void builderCondition(QueryWrapper<MdmProductModelRelation> queryWrapper, MdmProductModelRelation queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCode")), "MOULD_CODE", queryVO.getFieldValueByFieldName("mouldCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCategory")), "MOULD_CATEGORY", queryVO.getFieldValueByFieldName("mouldCategory"));
        queryWrapper.exists(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), " SELECT 1 FROM t_mdm_model_info WHERE" +
                " t_mdm_model_info.MOULD_CODE = T_MDM_PRODUCT_MODEL_RELATION.MOULD_CODE AND MOULD_NO = {0}", queryVO.getFieldValueByFieldName("mouldNo"));
    }

    @Override
    protected String getTypeCode() {
        return "0114-1";
    }


}
