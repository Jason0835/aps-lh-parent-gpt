package com.zlt.aps.maindata.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.CommonTypeEnum;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.maindata.enums.SystemBaseEnums;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.vo.ConfigConstructionVo;
import com.zlt.aps.monthplan.api.domain.vo.MaterialInfoGrossRateJsonVo;
import com.zlt.aps.monthplan.api.domain.vo.MaterialInfoGrossRateVo;
import com.zlt.aps.monthplan.api.domain.vo.TableProductInfoVo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tlt.aps.constant.FactoryConstant.DEFAULT_FACTORY_CODE;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMaterialInfoServiceImpl.java
 * 描    述：MdmMaterialInfoServiceImpl物料信息业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-19
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMaterialInfoServiceImpl extends AbstractDocService<MdmMaterialInfo> implements IMdmMaterialInfoService {

    @Resource
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;

    @Override
    protected String getDocTypeCode() {
        return "0102";
    }

    /**
     * 根据编号查询物料信息
     */
    @Override
    public List<MdmMaterialInfo> selectListByProductCode(List<String> codeList) {
        if (CollectionUtils.isEmpty(codeList)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.in(MdmMaterialInfo::getMaterialCode, codeList);
        return mdmMaterialInfoEntityMapper.selectList(wrapper);
    }

    @Override
    public List<TableProductInfoVo> getList(TableProductInfoVo queryCondition) {
        List<TableProductInfoVo> resultData = mdmMaterialInfoEntityMapper.getMaterialInfoList(queryCondition);
        analysisGrossRate(resultData);
        return resultData;
    }

    /**
     * 查询物料信息表
     *
     * @param id 物料信息表主键
     * @return 物料信息表
     */
    @Override
    public MdmMaterialInfo selectMaterialInfoById(Long id) {
        return mdmMaterialInfoEntityMapper.selectById(id);
    }

    /**
     * 新增物料信息表
     *
     * @param productInfo 物料信息表
     * @return 结果
     */
    @Override
    public int insertMaterialInfo(MdmMaterialInfo productInfo) {
        return baseDao.insert(productInfo);
    }

    /**
     * 修改物料信息表
     *
     * @param productInfo 物料信息表
     * @return 结果
     */
    @Override
    public int updateMaterialInfo(MdmMaterialInfo productInfo) {
        return mdmMaterialInfoEntityMapper.updateById(productInfo);
    }

    /**
     * 批量删除物料信息表
     *
     * @param ids 需要删除的物料信息表主键
     * @return 结果
     */
    @Override
    public int deleteMaterialInfoByIds(Long[] ids) {
        return mdmMaterialInfoEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 删除物料信息表信息
     *
     * @param id 物料信息表主键
     * @return 结果
     */
    @Override
    public int deleteMaterialInfoById(Long id) {
        return mdmMaterialInfoEntityMapper.deleteById(id);
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkMaterialInfoUnique(MdmMaterialInfo productInfo) {
        if (productInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmMaterialInfo::getFactoryCode, productInfo.getFactoryCode());
        wrapper.eq(MdmMaterialInfo::getMaterialCode, productInfo.getMaterialCode());
//        wrapper.eq(MdmMaterialInfo::getCommonType, productInfo.getCommonType());
        if (productInfo.getId() != null) {
            wrapper.ne(MdmMaterialInfo::getId, productInfo.getId());
        }
        List<MdmMaterialInfo> list = mdmMaterialInfoEntityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询列表
     *
     * @param wrapper 查询条件
     * @return 结果
     */
    @Override
    public List<MdmMaterialInfo> selectList(QueryWrapper<MdmMaterialInfo> wrapper) {
        if (wrapper != null) {
            List<MdmMaterialInfo> productInfoList = mdmMaterialInfoEntityMapper.selectList(wrapper);
            transformJsonField(productInfoList);
            return productInfoList;
        }
        return Collections.emptyList();
    }

    /**
     * 将json字段转成前端展示字段
     *
     * @param productInfoList 要转换的物料信息
     */
    @Override
    public void transformJsonField(List<MdmMaterialInfo> productInfoList) {
        if (CollectionUtils.isNotEmpty(productInfoList)) {
            for (MdmMaterialInfo productInfo : productInfoList) {
                String grossRateJson = productInfo.getGrossRateJson();
                if (StringUtils.isNotBlank(grossRateJson)) {
                    List<MaterialInfoGrossRateJsonVo> materialInfoGrossRateJsonVoList = JSON.parseArray(grossRateJson, MaterialInfoGrossRateJsonVo.class);
                    for (MaterialInfoGrossRateJsonVo materialInfoGrossRateJsonVo : materialInfoGrossRateJsonVoList) {
                        Object fieldValue = ReflectUtils.getFieldValue(materialInfoGrossRateJsonVo, "grossRate");
                        String commonType = materialInfoGrossRateJsonVo.getCommonType();
                        String fieldNameByCommonType = CommonTypeEnum.getFieldNameByCommonType(Integer.valueOf(commonType));
                        ReflectUtils.setFieldValue(productInfo, fieldNameByCommonType, fieldValue);

                    }
                }
            }
        }
    }

    /**
     * 将前端的字段转换json字段存储
     *
     * @param productInfoList 要转换的物料信息
     */
    @Override
    public void transformToJsonField(List<MdmMaterialInfo> productInfoList) {
        if (CollectionUtils.isNotEmpty(productInfoList)) {
            for (MdmMaterialInfo productInfo : productInfoList) {
                List<MaterialInfoGrossRateJsonVo> materialInfoGrossRateJsonVoList = new ArrayList<>();
                List<Field> fieldList = Arrays.stream(MdmMaterialInfo.class.getDeclaredFields()).filter(item -> item.getName().contains("GrossRate")).collect(Collectors.toList());
                for (Field field : fieldList) {
                    Integer commonTypeByFieldName = CommonTypeEnum.getCommonTypeByFieldName(field.getName());
                    Object fieldValue = ReflectUtils.getFieldValue(productInfo, field.getName());
                    MaterialInfoGrossRateJsonVo jsonVo = new MaterialInfoGrossRateJsonVo();
                    jsonVo.setCommonType(commonTypeByFieldName.toString());
                    if (fieldValue != null) {
                        jsonVo.setGrossRate(new BigDecimal(fieldValue.toString()));
                        materialInfoGrossRateJsonVoList.add(jsonVo);
                    }
                }
                String jsonString = JSON.toJSONString(materialInfoGrossRateJsonVoList);
                productInfo.setGrossRateJson(jsonString);
            }
        }
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0102");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMaterialInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.productMinConfiguration.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "productCode"));
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmMaterialInfo importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (StringUtils.isBlank(importDocEntity.getProductTypeCode())) {
            importDocEntity.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        }
        if (StringUtils.isNotBlank(importDocEntity.getMaterialCode())) {
            ProductTypeEnum enumByValue = ProductTypeEnum.getEnumByValue(importDocEntity.getProductTypeCode());
            if (enumByValue !=  null) {
                importDocEntity.setProductTypeName(enumByValue.getName());
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 导入物料信息
     *
     * @param list          要导入的列表
     * @param updateSupport 是否更新
     * @param importLogId   导入记录id
     * @return 结果
     */
    @Override
    public AjaxResult importGrossRate(List<MaterialInfoGrossRateVo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        // 校验
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<MaterialInfoGrossRateVo> importList = new ArrayList<>();
        List<String> fieldNameList = Arrays.asList("outGrossRate", "inGrossRate", "oeGrossRate");

        // 提示信息
        String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
        String columnName1 = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.factoryCode");
        String columnName2 = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.productCode");
        String outGrossRateRequired = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.outGrossRate.required");
        String inGrossRateRequired = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.inGrossRate.required");
        String oeGrossRateRequired = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.oeGrossRate.required");
        String dbNotExist = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.dbNotExist");

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors
                .groupingBy(item -> String.join("|", item.getFactoryCode(), item.getMaterialCode()),
                        Collectors.counting()));

        // 查询数据库内对应数据，将毛利率字段更新
        // 数据库内不存在的数据暂不考虑
        List<MdmMaterialInfo> productInfoList = new ArrayList<>();
        List<String> uniqueKeyList = list.stream().map(productInfo ->
                String.join("|", productInfo.getFactoryCode(), productInfo.getMaterialCode())).collect(Collectors.toList());

        Map<String, MdmMaterialInfo> productInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(uniqueKeyList)) {
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(uniqueKeyList, 500);
            for (List<String> subList : splitList) {
                List<MdmMaterialInfo> productInfos = mdmMaterialInfoEntityMapper.selectByUniqueKeyList(subList);
                productInfoList.addAll(productInfos);
            }

            productInfoMap = productInfoList.stream().collect(Collectors
                    .toMap(item -> String.join("|", item.getFactoryCode(), item.getMaterialCode()),
                            Function.identity(), (v1, v2) -> v1));
        }

        for (int i = 0; i < list.size(); i++) {
            MaterialInfoGrossRateVo productInfo = list.get(i);

            //重复记录校验
            String commonType = productInfo.getCommonType();
            String uniqueKey = String.join("|", productInfo.getFactoryCode(), productInfo.getMaterialCode());
            Long hasValue = groupMap.get(uniqueKey);
            if (hasValue > 1) {
                message = String.format(message, String.join("+", columnName1, columnName2));
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, productInfo);
            // 校验公共类型，如果是外销，则外销毛利率必填，其他类型一样，如果是公用规格，没有填的话默认赋值0
            if (StringConstant.TWO.equals(commonType) && Objects.isNull(productInfo.getOutGrossRate())) {
                addImportErrorLog(importLogId, i + 2, outGrossRateRequired, validated);
            }
            if (StringConstant.THREE.equals(commonType) && Objects.isNull(productInfo.getInGrossRate())) {
                addImportErrorLog(importLogId, i + 2, inGrossRateRequired, validated);
            }
            if (StringConstant.FOUR.equals(commonType) && Objects.isNull(productInfo.getOeGrossRate())) {
                addImportErrorLog(importLogId, i + 2, oeGrossRateRequired, validated);
            }

            if (!productInfoMap.containsKey(uniqueKey)) {
                addImportErrorLog(importLogId, i + 2, dbNotExist, validated);
            }

            if (CollectionUtils.isEmpty(validated)) {
                // 公用规格，赋值默认值
                if (StringConstant.ONE.equals(commonType)) {
                    if (Objects.isNull(productInfo.getOutGrossRate())) {
                        productInfo.setOutGrossRate(BigDecimal.ZERO);
                    }
                    if (Objects.isNull(productInfo.getInGrossRate())) {
                        productInfo.setInGrossRate(BigDecimal.ZERO);
                    }
                    if (Objects.isNull(productInfo.getOeGrossRate())) {
                        productInfo.setOeGrossRate(BigDecimal.ZERO);
                    }
                }

                MdmMaterialInfo productInfoDb = productInfoMap.get(uniqueKey);
                Long id = productInfoDb.getId();
                productInfo.setId(id);
                List<MaterialInfoGrossRateJsonVo> materialInfoGrossRateJsonVos = new ArrayList<>();
                for (String fieldName : fieldNameList) {
                    // 如果规格共用类型不一样，且不是公用规格，就不做json转换保存
                    String fieldNameCommonType = CommonTypeEnum.getCommonTypeByFieldName(fieldName).toString();
                    if (!fieldNameCommonType.equals(commonType) && !StringConstant.ONE.equals(commonType)) {
                        continue;
                    }
                    MaterialInfoGrossRateJsonVo materialInfoGrossRateJsonVo = new MaterialInfoGrossRateJsonVo();
                    materialInfoGrossRateJsonVo.setCommonType(fieldNameCommonType);
                    materialInfoGrossRateJsonVo.setGrossRate(ReflectUtils.getFieldValue(productInfo, fieldName));
                    materialInfoGrossRateJsonVos.add(materialInfoGrossRateJsonVo);
                }
                productInfo.setGrossRateJson(JSON.toJSONString(materialInfoGrossRateJsonVos));

                productInfo.setBaseVale(null);
                importList.add(productInfo);
            } else {
                failureNum++;
                productInfo.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            try {
                List<MdmMaterialInfo> infoList = BeanCopyUtils.copyBeanList(importList, MdmMaterialInfo.class);
                successNum = baseDao.updateBatch(infoList);

                //勾选更新记录，调用merge即可
                /*if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                    successNum = importList.size();
                    cd15LossSettingMapper.mergeSql(importList);
                } else {
                    //查询数据库已存在对象
                    for (int i = 0; i < list.size(); i++) {
                        Cd15LossSettingDto excelItem = list.get(i);
                        if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                            continue;
                        }
                        // 唯一性校验
                        Cd15LossSetting tmLossSetting = new Cd15LossSetting();
                        BeanUtils.copyProperties(excelItem, tmLossSetting);
                        int unique = cd15LossSettingMapper.checkCd15LossSettingUnique(tmLossSetting);
                        if (unique == 0) {
                            //不存在插入
                            successNum++;
                            cd15LossSettingMapper.insertCd15LossSetting(tmLossSetting);
                        } else {
                            // 存在，插入错误详细日志
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2,
                                    I18nUtil.getMessage("ui.error.message.loss.unique"), importErrorLogs);
                        }
                    }
                }*/
            } catch (Exception e) {
                log.error(e.getMessage());
                // 执行sql失败，插入导入失败记录
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 查询对应物料列表+分厂列表的物料信息
     *
     * @param factoryCodeList 分厂列表（可以为空，只限制物料编号）
     * @param productCodeList 物料编号列表（不能为空）
     * @return 物料信息
     */
    @Override
    public List<MdmMaterialInfo> selectListByFactoryProductCode(List<String> factoryCodeList, List<String> productCodeList) {
        if (CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList);
        wrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
        return mdmMaterialInfoEntityMapper.selectList(wrapper);
    }

    /**
     * 根据分厂编号和物料号集合查询物料信息
     *
     * @param factoryCode  分厂编号
     * @param productCodes 物料编号集合
     * @return 对应的施工记录列表
     */
    @Override
    public List<MdmMaterialInfo> queryByFactoryCodeAndProductCodes(String factoryCode, Set<String> productCodes) {
        // 将 Set 转换为 List，便于切分批次
        List<String> productCodeList = new ArrayList<>(productCodes);
        //定义最终返回的List
        List<MdmMaterialInfo> finalList = new ArrayList<>();
        //判断集合的长度是多少 如果超过900条则进行切分查询
        if (productCodeList.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(productCodeList, SystemBaseEnums.SPLIT_LENGTH.getCode());
            //将多次查询的结果汇总到finalList中
            for (List<String> splitItemList : splitList) {
                List<MdmMaterialInfo> queryList = mdmMaterialInfoEntityMapper.queryByFactoryCodeAndProductCodes(factoryCode, splitItemList);
                finalList.addAll(queryList);
            }
        } else {
            finalList = mdmMaterialInfoEntityMapper.queryByFactoryCodeAndProductCodes(factoryCode, productCodeList);
        }
        return finalList;
    }

    @Override
    public MdmMaterialInfo selectOneByProductCodeAndSpecCode(String productCode, String factoryCode) {
        LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotBlank(factoryCode), MdmMaterialInfo::getFactoryCode, factoryCode);
        queryWrapper.eq(StringUtils.isNotBlank(productCode), MdmMaterialInfo::getMaterialCode, productCode);
        queryWrapper.eq(MdmMaterialInfo::getIsDelete, YesOrNoEnum.NO.getCode());
        return mdmMaterialInfoEntityMapper.selectOne(queryWrapper);
    }

    /**
     * 毛利率处理
     *
     * @param productInfoList
     */
    private void analysisGrossRate(List<TableProductInfoVo> productInfoList) {
        if (CollectionUtils.isEmpty(productInfoList)) {
            return;
        }
        for (TableProductInfoVo productInfo : productInfoList) {
            setGrossRate(productInfo);
        }
    }

    /**
     * 毛利率解析
     *
     * @param productInfo
     */
    private void setGrossRate(MdmMaterialInfo productInfo) {
        String grossRateJson = productInfo.getGrossRateJson();
        if (StringUtils.isBlank(grossRateJson)) {
            return;
        }
        List<MaterialInfoGrossRateJsonVo> productInfoGrossRateJsonList = JSON.parseArray(grossRateJson, MaterialInfoGrossRateJsonVo.class);
        if (CollectionUtils.isEmpty(productInfoGrossRateJsonList)) {
            return;
        }
        for (MaterialInfoGrossRateJsonVo productInfoGrossRateJson : productInfoGrossRateJsonList) {
            Object fieldValue = ReflectUtils.getFieldValue(productInfoGrossRateJson, "grossRate");
            String commonType = productInfoGrossRateJson.getCommonType();
            String fieldNameByCommonType = CommonTypeEnum.getFieldNameByCommonType(Integer.valueOf(commonType));
            ReflectUtils.setFieldValue(productInfo, fieldNameByCommonType, fieldValue);
        }
    }

    @Autowired
    private MdmProductConstructionEntityMapper productConstructionEntityMapper;

    /**
     * 配置施工记录
     *
     * @param productConstruction 物料信息ID、SAP代码(物料表的)、胎胚号、规格代码、施工代码、成型法
     * @return 结果
     */
    @Override
    public AjaxResult configurationConstructionCheck(MdmProductConstruction productConstruction) {
        String productCode = productConstruction.getProductCode();
        LambdaQueryWrapper<MdmProductConstruction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseEntity::getIsDelete, 0);
        queryWrapper.eq(MdmProductConstruction::getEmbryoCode, productConstruction.getEmbryoCode());
        queryWrapper.eq(MdmProductConstruction::getSpecCode, productConstruction.getSpecCode());
        List<MdmProductConstruction> productConstructionList = productConstructionEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(productConstructionList)) {
            for (MdmProductConstruction construction : productConstructionList) {
                String productCode1 = construction.getProductCode();
                if (StringUtils.isNotBlank(productCode1) && !productCode.equals(productCode1)) {
                    return new AjaxResult(301, I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.configurationConstructionCheck"), 1);
                }
            }
        } else {
            LambdaQueryWrapper<MdmProductConstruction> queryWrapperNew = new LambdaQueryWrapper<>();
            queryWrapperNew.eq(BaseEntity::getIsDelete, 0);
            queryWrapperNew.eq(MdmProductConstruction::getEmbryoCode, productConstruction.getEmbryoCode());
            productConstructionList = productConstructionEntityMapper.selectList(queryWrapperNew);
            if (CollectionUtils.isEmpty(productConstructionList)) {
                return new AjaxResult(301, I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.configurationConstruction.notExist"), 2);
            }
        }
        return AjaxResult.success();
    }

    /**
     * 配置施工记录
     *
     * @param productConstruction 物料信息ID、SAP代码(物料表的)、胎胚号、规格代码、施工代码、成型法
     * @return 结果
     */
    @Override
    public AjaxResult configurationConstruction(MdmProductConstruction productConstruction) {
        String productCode = productConstruction.getProductCode();
        LambdaQueryWrapper<MdmProductConstruction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseEntity::getIsDelete, 0);
        queryWrapper.eq(MdmProductConstruction::getEmbryoCode, productConstruction.getEmbryoCode());
        queryWrapper.eq(MdmProductConstruction::getSpecCode, productConstruction.getSpecCode());
        queryWrapper.eq(MdmProductConstruction::getProductionVersion, "");
        List<MdmProductConstruction> productConstructionList = productConstructionEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(productConstructionList)) {
            for (MdmProductConstruction construction : productConstructionList) {
                construction.setProductCode(productCode);
                construction.setConstructionCode(productConstruction.getConstructionCode());
                construction.setMouldMethod(productConstruction.getMouldMethod());
            }
            baseDao.updateBatch(productConstructionList);
        } else {
            LambdaQueryWrapper<MdmProductConstruction> queryWrapperNew = new LambdaQueryWrapper<>();
            queryWrapperNew.eq(BaseEntity::getIsDelete, 0);
            queryWrapperNew.eq(MdmProductConstruction::getEmbryoCode, productConstruction.getEmbryoCode());
            queryWrapperNew.eq(MdmProductConstruction::getProductionVersion, "");
            productConstructionList = productConstructionEntityMapper.selectList(queryWrapperNew);
            if (CollectionUtils.isNotEmpty(productConstructionList)) {
                // 如果有多条取第一条
                MdmProductConstruction mdmProductConstruction = productConstructionList.get(0);
                MdmProductConstruction newConstruction = new MdmProductConstruction();
                BeanUtils.copyProperties(mdmProductConstruction, newConstruction, "id");
                newConstruction.setSpecCode(productConstruction.getSpecCode());
                newConstruction.setMouldMethod(productConstruction.getMouldMethod());
                newConstruction.setConstructionCode(productConstruction.getConstructionCode());
                baseDao.insert(newConstruction);
            }
        }
        return AjaxResult.success();
    }

    /**
     * 根据物料号查询对应的SAP与施工关系
     *
     * @param productConstruction 物料号
     * @return 结果
     */
    @Override
    public AjaxResult selectConstructionCheckList(MdmProductConstruction productConstruction) {
        List<MdmProductConstruction> productConstructionList = mdmProductConstructionService.selectByProductCode(productConstruction.getProductCode());
        return AjaxResult.success(productConstructionList);
    }

    /**
     * 配置施工关系
     *
     * @param configConstructionVo 配置施工关系
     * @return 结果
     */
    @Override
    public AjaxResult configConstruction(ConfigConstructionVo configConstructionVo) {
        List<MdmProductConstruction> list = configConstructionVo.getList();

        for (int i = 0; i < list.size(); i++) {
            MdmProductConstruction construction = list.get(i);
            construction.setFactoryCode(DEFAULT_FACTORY_CODE);
            construction.setBaseVale(null);
            String embryoCode = construction.getEmbryoCode();
            String specCode = construction.getSpecCode();
            String constructionCode = construction.getConstructionCode();
            if (StringUtils.isAllBlank(embryoCode, specCode, constructionCode)) {
                // 根据当前物料号、成型法，删除对应施工关系
                mdmProductConstructionService.removeByProductCodeAndMouldMethod(construction);
                list.remove(construction);
                i--;
            }
        }
        mdmProductConstructionService.syncProductConstructionInfo(list, "");
        return AjaxResult.success();
    }
}

