package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.mdm.api.domain.dto.MdmProductConstructionDto;
import com.zlt.aps.mdm.enums.SystemBaseEnums;
import com.zlt.aps.mdm.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.mdm.service.IMdmProductConstructionService;
import com.zlt.aps.mdm.utils.ScmListUtils;
import com.zlt.aps.mdm.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.mdm.api.domain.vo.MdmProductConstructionImportVo;
import com.zlt.aps.mdm.api.domain.vo.MdmProductConstructionVO;
import com.zlt.aps.mdm.api.domain.vo.ProductSpecInfoVo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductConstructionServiceImpl.java
 * 描    述：MdmProductConstructionServiceImplSAP与施工对照业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmProductConstructionServiceImpl extends AbstractDocService<MdmProductConstruction> implements IMdmProductConstructionService {

    @Autowired
    private MdmProductConstructionEntityMapper mdmProductConstructionEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0108";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0108");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmProductConstruction docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmProductConstruction.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "productCode", "specCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmProductConstruction> list, List<MdmProductConstruction> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(item -> String.join("|", item.getFactoryCode(), item.getProductCode(), item.getSpecCode()), Collectors.counting()));
        serviceCheckParams.put("groupMap", groupMap);
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmProductConstruction importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Map<String, Long> groupMap = (Map<String, Long>) serviceCheckParams.getOrDefault("groupMap", new HashMap<>());
        String mapKey = String.join("|", importDocEntity.getFactoryCode(), importDocEntity.getProductCode(), importDocEntity.getSpecCode());
        if (groupMap.containsKey(mapKey)) {
            if (groupMap.get(mapKey) > 1) {
                String message = I18nUtil.getMessage("ui.data.alert.mdmProductConstruction.excelRepeat");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, String.format(message, errorRowNum), importErrorLogs);
                return Boolean.FALSE;
            }
        }
        //todo 机械液压先同步
        importDocEntity.setHydraulicPressureCuringTime2(importDocEntity.getCuringTime2());
        importDocEntity.setHydraulicPressureCuringTime(importDocEntity.getCuringTime());
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 根据分厂编号和产品、规格组合查询 SAP 与施工关系数据
     *
     * @param factoryCode 分厂编号
     * @param specCodes   产品与规格组合列表，每个对象包含 productCode 和 specCode
     *                    示例：["物料编码_规格代码", "P002_S002"]
     * @return 对应的施工记录列表
     */
    @Override
    public List<MdmProductConstructionVO> queryByFactoryCodeAndSpecCodes(String factoryCode, Set<String> specCodes) {
        // 将 Set 转换为 List，便于切分批次
        List<String> codeList = new ArrayList<>(specCodes);
        //定义最终返回的List
        List<MdmProductConstructionVO> finalList = new ArrayList<>();
        //判断集合的长度是多少 如果超过900条则进行切分查询
        if (codeList.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(codeList, SystemBaseEnums.SPLIT_LENGTH.getCode());
            //将多次查询的结果汇总到finalList中
            for (List<String> splitItemList : splitList) {
                List<MdmProductConstructionVO> queryList = mdmProductConstructionEntityMapper.queryByFactoryCodeAndSpecCodes(factoryCode, splitItemList);
                finalList.addAll(queryList);
            }
        } else {
            finalList = mdmProductConstructionEntityMapper.queryByFactoryCodeAndSpecCodes(factoryCode, codeList);
        }
        return finalList;
    }

    /**
     * 根据规格号查询物料List
     *
     * @param factoryCode
     * @param specCode
     * @return
     */
    @Override
    public List<MdmProductConstruction> selectListByFactoryCodeAndSpecCode(String factoryCode, List<String> specCode) {
        //定义最终返回的List
        List<MdmProductConstruction> finalList = new ArrayList<>();
        if (specCode.size() > SystemBaseEnums.SPLIT_LENGTH.getCode()) {
            List<List<String>> splitList = ScmListUtils.getSplitList(specCode, SystemBaseEnums.SPLIT_LENGTH.getCode());
            for (List<String> splitItemList : splitList) {
                LambdaQueryWrapper<MdmProductConstruction> queryWrapper = Wrappers.lambdaQuery();
                queryWrapper.eq(StringUtils.isNotBlank(factoryCode), MdmProductConstruction::getFactoryCode, factoryCode);
                queryWrapper.in(MdmProductConstruction::getSpecCode, splitItemList);
                queryWrapper.eq(MdmProductConstruction::getIsDelete, YesOrNoEnum.NO.getCode());
                List<MdmProductConstruction> queryWrapperList = mdmProductConstructionEntityMapper.selectList(queryWrapper);
                finalList.addAll(queryWrapperList);
            }
        } else {
            LambdaQueryWrapper<MdmProductConstruction> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(StringUtils.isNotBlank(factoryCode), MdmProductConstruction::getFactoryCode, factoryCode);
            queryWrapper.in(MdmProductConstruction::getSpecCode, specCode);
            queryWrapper.eq(MdmProductConstruction::getIsDelete, YesOrNoEnum.NO.getCode());
            finalList =  mdmProductConstructionEntityMapper.selectList(queryWrapper);
        }
       return finalList;
    }

    @Override
    public MdmProductConstructionDto getCuringTime(String factoryCode, String productCode, String specCode) {
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(factoryCode) || StringUtils.isBlank(specCode)) {
            return null;
        }
        QueryWrapper<MdmProductConstruction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("PRODUCT_CODE", productCode);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        List<MdmProductConstruction> constructionList = mdmProductConstructionEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(constructionList)) {
            return null;
        }
        MdmProductConstructionDto productConstruction = new MdmProductConstructionDto();
        List<ProductSpecInfoVo> productSpecCodeInfoList = new ArrayList<>();
        constructionList.forEach(constructionConfiguration -> {
            String configurationSpecCode = constructionConfiguration.getSpecCode();
            ProductSpecInfoVo productSpecInfo = new ProductSpecInfoVo();
            productSpecInfo.setConstructionCode(constructionConfiguration.getConstructionCode());
            productSpecInfo.setSpecCode(configurationSpecCode);
            productSpecInfo.setEmbryoCode(constructionConfiguration.getEmbryoCode());
            productSpecInfo.setMouldMethod(constructionConfiguration.getMouldMethod());
            productSpecCodeInfoList.add(productSpecInfo);
            if (specCode.equals(configurationSpecCode)) {
                BeanUtils.copyProperties(constructionConfiguration, productConstruction);
            }
        });
        productConstruction.setProductSpecCodeInfoList(productSpecCodeInfoList);
        return productConstruction;
    }

    @Override
    public AjaxResult importData(List<MdmProductConstruction> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<MdmProductConstruction> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        Map<Object, Object> serviceCheckParams = this.getServiceCheckParams(list, importList);

        for(int i = 0; i < list.size(); ++i) {
            int errorNum = i + 2;
            MdmProductConstruction docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (!this.serviceCheckAndDataHandle(docEntity, validated, importLogId, i + 2, serviceCheckParams)) {
                this.logger.debug("第{}行,业务校验不通过", errorNum);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                ++failureNum;
                importErrorLogs.addAll(validated);
            } else {
                docEntity.setBaseVale(null);
                importList.add(docEntity);
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {

            this.syncProductConstructionInfo(importList, "");
            successNum = importList.size();
            if (failureNum > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
            } else {
                return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
            }
        }
    }

    /**
     * 导入客户格式施工信息
     *
     * @param importList    导入列表
     * @param updateSupport 是否更新已存在的数据
     * @param importLogId   导入日志ID
     * @return 结果
     */
    @Override
    public AjaxResult importOfflineData(List<MdmProductConstructionImportVo> importList, boolean updateSupport, Long importLogId) {
        List<MdmProductConstructionImportVo> resultList = new ArrayList<>();
        // 将多个规格代码拆成多条数据
        for (MdmProductConstructionImportVo importVo : importList) {
            // 解析硫化时间
            importVo.setCuringTimeByTimeStr();
            String specCode = importVo.getSpecCode();
            if (specCode.contains("/")) {
                String[] split = specCode.split("/");
                for (String code : split) {
                    MdmProductConstructionImportVo vo = new MdmProductConstructionImportVo();
                    BeanUtils.copyProperties(importVo, vo);
                    vo.setSpecCode(code);
                    resultList.add(vo);
                }
            } else {
                resultList.add(importVo);
            }
        }
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        // 校验excel内是否有重复的数据
        Map<String, MdmProductConstructionImportVo> importVoMap = resultList.stream().collect(Collectors
                .toMap(item -> GenerageMapKeyUtils.createMapKey(item.getEmbryoCode(), item.getSpecCode()),
                        Function.identity(), (s1, s2) -> {
                            if (s1.getCuringTime() != null && s1.getCuringTime() != 0) {
                                return s1;
                            }
                            if (s2.getCuringTime() != null && s2.getCuringTime() != 0) {
                                return s2;
                            }
                            return s1;
                        }));
        List<MdmProductConstructionImportVo> list = new ArrayList<>(importVoMap.values());
        /*for (MdmProductConstructionImportVo importVo : resultList) {
            Long hasValue = groupMap.get(GenerageMapKeyUtils.createMapKey(importVo.getEmbryoCode(), importVo.getSpecCode()));
            if (hasValue > 1) {
                failureNum++;
                importVo.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.mdmProductConstruction.importVo.embryoCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.mdmProductConstruction.importVo.specCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, null, message, importErrorLogs);
                continue;
            }
            list.add(importVo);
        }*/

        List<List<MdmProductConstructionImportVo>> splitList = ScmListUtils.getSplitList(list, 1000);
        for (List<MdmProductConstructionImportVo> importVos : splitList) {
            // 查询已存在的数据
            List<MdmProductConstruction> existList = mdmProductConstructionEntityMapper.selectExistData(importVos);
            baseDao.updateBatch(existList);
            List<String> existKeyList = existList.stream().map(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getSpecCode(), item.getEmbryoCode())).collect(Collectors.toList());
            List<MdmProductConstruction> insertList = new ArrayList<>();
            for (MdmProductConstructionImportVo importVo : importVos) {
                String mapKey = GenerageMapKeyUtils.createMapKey(importVo.getFactoryCode(), importVo.getSpecCode(), importVo.getEmbryoCode());
                if (!existKeyList.contains(mapKey)) {
                    importVo.setProductionVersion("");
                    importVo.setBomVersion("01");
                    MdmProductConstruction mdmProductConstruction = new MdmProductConstruction();
                    BeanUtils.copyProperties(importVo, mdmProductConstruction);
                    mdmProductConstruction.setProductCode("");
                    mdmProductConstruction.setConstructionCode("");
                    mdmProductConstruction.setMouldMethod("");
                    mdmProductConstruction.setProductionVersion("");
                    mdmProductConstruction.setBomVersion("");
                    insertList.add(mdmProductConstruction);
                }
            }
            baseDao.insertBatch(insertList);
        }

        // 全表更新生胎对应的硫化时间
        mdmProductConstructionEntityMapper.batchUpdateCuringTime(SecurityUtils.getUsername());

        successNum = list.size();
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 同步数据
     * 1、Find(Sap代码 成形法)
     * 1.1、IF(存在)， update(规格代号、施工代码、版本号)
     * 1.1.2、IF(旧生胎代码 == 新生胎代码)
     * 1.1.2.1、IF(旧硫化时间有值)，结束
     * 1.1.2.2、IF(旧硫化时间没值)，update(硫化时间、模具型腔、合模压力) = Find(新生胎代码，Sap不能T开头，硫化时间有值，更新时间最晚).(硫化时间、模具型腔、合模压力)
     * 1.1.3、IF(旧生胎代码 != 新生胎代码)
     * update(硫化时间、模具型腔、合模压力) = Find(新生胎代码，Sap不能T开头，硫化时间有值，更新时间最晚).(硫化时间、模具型腔、合模压力)
     * ---
     * 1.2、IF(不存在)，Find(规格代号)
     * 1.2.1、IF(存在)，Update(SAP代码，成型法，施工代号、版本号、生胎代码)
     * 1.2.1.1、IF(旧Sap代码为空或是T开头)，update(硫化时间、模具型腔、合模压力) = Find(新生胎代码，Sap不能T开头，硫化时间有值，更新时间最晚).(硫化时间、模具型腔、合模压力)
     * 1.2.1.2、IF(旧Sap代码不为空且不是T开头)，IF(旧生胎代码 == 新生胎代码，硫化时间不为空)，结束
     * 1.2.1.3、IF(旧Sap代码不为空且不是T开头)，IF(旧生胎代码 == 新生胎代码，硫化时间为空)，update(硫化时间、模具型腔、合模压力) = Find(新生胎代码，Sap不能T开头，硫化时间有值，更新时间最晚).(硫化时间、模具型腔、合模压力)
     * 1.2.1.4、IF(旧Sap代码不为空且不是T开头)，IF(旧生胎代码 != 新生胎代码)，update(硫化时间、模具型腔、合模压力) = Find(新生胎代码，Sap不能T开头，硫化时间有值，更新时间最晚).(硫化时间、模具型腔、合模压力)
     * ---Update(SAP代码，成型法，施工代号、版本号、生胎代码)的代码在此处更新，避免上面的判断有误
     * 1.2.2、IF(不存在)，新增记录
     * insert(硫化时间、模具型腔、合模压力) = Find(生胎代码，Sap不能T开头，硫化时间有值，更新时间最晚).(硫化时间、模具型腔、合模压力)
     *
     * @param syncData    同步数据
     * @param dataVersion 数据版本
     */
    @Override
    public void syncProductConstructionInfo(List<MdmProductConstruction> syncData, String dataVersion) {
        // 根据物料+成型法查询
        List<MdmProductConstruction> productCodeAndMethodList = mdmProductConstructionEntityMapper.selectByProductCodeAndMethod(syncData);
        Map<String, MdmProductConstruction> productCodeMap = productCodeAndMethodList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getProductCode(), item.getMouldMethod()), Function.identity(), (s1, s2) -> s1));
        // 根据规格代号查询
        List<MdmProductConstruction> specCodeList = mdmProductConstructionEntityMapper.selectBySpecCode(syncData);
        Map<String, MdmProductConstruction> specCodeMap = specCodeList.stream().collect(Collectors.toMap(MdmProductConstruction::getSpecCode, Function.identity(), (s1, s2) -> s1));
        // 查询接口生胎代码、物料号不能T开头、硫化时间有值、更新时间最晚的查询
        List<MdmProductConstruction> embryoCodeList = mdmProductConstructionEntityMapper.selectByEmbryoCode(syncData);
        Map<String, MdmProductConstruction> embryoCodeMap = embryoCodeList.stream().collect(Collectors.toMap(MdmProductConstruction::getEmbryoCode, Function.identity(), (s1, s2) -> s1.getUpdateTime().compareTo(s2.getUpdateTime()) >= 0 ? s1 : s2));

        List<MdmProductConstruction> productCodeAndMethodUpdateList = new ArrayList<>();
        List<MdmProductConstruction> curingTimeUpdateList = new ArrayList<>();
        List<MdmProductConstruction> productCodeUpdateList = new ArrayList<>();
        List<MdmProductConstruction> insertList = new ArrayList<>();

        for (MdmProductConstruction productConstruction : syncData) {
            String productCodeMapKey = GenerageMapKeyUtils.createMapKey(productConstruction.getProductCode(), productConstruction.getMouldMethod());
            String newEmbryoCode = productConstruction.getEmbryoCode();

            if (productCodeMap.containsKey(productCodeMapKey)) {
                MdmProductConstruction construction = productCodeMap.get(productCodeMapKey);
                construction.setSpecCode(productConstruction.getSpecCode());
                construction.setConstructionCode(productConstruction.getConstructionCode());
                construction.setProductionVersion(productConstruction.getProductionVersion());
                construction.setBomVersion(productConstruction.getBomVersion());
                productCodeAndMethodUpdateList.add(construction);
                if (newEmbryoCode.equals(construction.getEmbryoCode())) {
                    if (construction.getCuringTime() == null) {
                        setNewEmbryoCodeCuringTime(embryoCodeMap, newEmbryoCode, construction, curingTimeUpdateList);
                    }
                } else {
                    setNewEmbryoCodeCuringTime(embryoCodeMap, newEmbryoCode, construction, curingTimeUpdateList);
                    construction.setEmbryoCode(productConstruction.getEmbryoCode());
                    productCodeUpdateList.add(construction);
                }
            } else {
                String newSpecCode = productConstruction.getSpecCode();
                if (specCodeMap.containsKey(newSpecCode)) {
                    MdmProductConstruction construction = specCodeMap.get(newSpecCode);

                    if (StringUtils.isBlank(construction.getProductCode()) || construction.getProductCode().startsWith("T")) {
                        setNewEmbryoCodeCuringTime(embryoCodeMap, newEmbryoCode, construction, curingTimeUpdateList);
                    }
                    if (StringUtils.isNotBlank(construction.getProductCode()) && !construction.getProductCode().startsWith("T")) {
                        if (newEmbryoCode.equals(construction.getEmbryoCode())) {
                            if (construction.getCuringTime() == null) {
                                setNewEmbryoCodeCuringTime(embryoCodeMap, newEmbryoCode, construction, curingTimeUpdateList);
                            }
                        } else {
                            setNewEmbryoCodeCuringTime(embryoCodeMap, newEmbryoCode, construction, curingTimeUpdateList);
                        }
                    }

                    // 更新物料
                    construction.setProductCode(productConstruction.getProductCode());
                    construction.setMouldMethod(productConstruction.getMouldMethod());
                    construction.setConstructionCode(productConstruction.getConstructionCode());
                    construction.setBomVersion(productConstruction.getBomVersion());
                    construction.setProductionVersion(productConstruction.getProductionVersion());
                    construction.setEmbryoCode(productConstruction.getEmbryoCode());
                    productCodeUpdateList.add(construction);
                } else {
                    // 新增
                    setNewEmbryoCodeCuringTime(embryoCodeMap, newEmbryoCode, productConstruction, insertList);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(insertList)) {
            mdmProductConstructionEntityMapper.insertBatch(insertList);
        }
        if (CollectionUtils.isNotEmpty(productCodeUpdateList)) {
            mdmProductConstructionEntityMapper.updateProductCodeById(productCodeUpdateList);
        }
        if (CollectionUtils.isNotEmpty(productCodeAndMethodUpdateList)) {
            mdmProductConstructionEntityMapper.updateSpecConsVersionByProductCodeAndMethod(productCodeAndMethodUpdateList);
        }
        if (CollectionUtils.isNotEmpty(curingTimeUpdateList)) {
            mdmProductConstructionEntityMapper.updateCuringTimeByEmbryoCodeList(curingTimeUpdateList);
        }
        String updateBy = "";
        if (CollectionUtils.isNotEmpty(syncData)) {
            MdmProductConstruction construction = syncData.get(0);
            updateBy = construction.getUpdateBy();
        }
        mdmProductConstructionEntityMapper.batchUpdateCuringTime(updateBy);

    }

    /**
     * 赋值新生胎代码对应的硫化时间、模具型腔、合模压力给旧的数据
     * @param embryoCodeMap 新生胎代码对应的数据
     * @param newEmbryoCode 新生胎代码
     * @param construction 旧数据
     * @param curingTimeUpdateList 硫化时间、模具型腔、合模压力更新列表
     */
    private static void setNewEmbryoCodeCuringTime(Map<String, MdmProductConstruction> embryoCodeMap, String newEmbryoCode, MdmProductConstruction construction, List<MdmProductConstruction> curingTimeUpdateList) {
        MdmProductConstruction newEmbryoCodeConstruction = embryoCodeMap.get(newEmbryoCode);
        if (embryoCodeMap.containsKey(newEmbryoCode)) {
            construction.setCuringTime(newEmbryoCodeConstruction.getCuringTime());
            construction.setCuringTime2(newEmbryoCodeConstruction.getCuringTime2());
            construction.setHydraulicPressureCuringTime(newEmbryoCodeConstruction.getHydraulicPressureCuringTime());
            construction.setHydraulicPressureCuringTime2(newEmbryoCodeConstruction.getHydraulicPressureCuringTime2());
            construction.setMoldCavity(newEmbryoCodeConstruction.getMoldCavity());
            construction.setMouldClampingPressure(newEmbryoCodeConstruction.getMouldClampingPressure());
        }
        curingTimeUpdateList.add(construction);
    }

    /**
     * 根据物料编码查询
     *
     * @param productCode 物料编码
     * @return 结果
     */
    @Override
    public List<MdmProductConstruction> selectByProductCode(String productCode) {
        LambdaQueryWrapper<MdmProductConstruction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmProductConstruction::getProductCode, productCode);
        return mdmProductConstructionEntityMapper.selectList(queryWrapper);
    }

    /**
     * 根据物料编码和成型法删除
     *
     * @param construction 物料号、成型法
     * @return 结果
     */
    @Override
    public AjaxResult removeByProductCodeAndMouldMethod(MdmProductConstruction construction) {
        LambdaUpdateWrapper<MdmProductConstruction> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MdmProductConstruction::getProductCode, construction.getProductCode());
        updateWrapper.eq(MdmProductConstruction::getMouldMethod, construction.getMouldMethod());
        mdmProductConstructionEntityMapper.delete(updateWrapper);
        return AjaxResult.success();
    }
}
