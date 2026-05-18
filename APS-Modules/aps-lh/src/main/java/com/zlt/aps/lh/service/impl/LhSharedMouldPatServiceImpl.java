package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import com.zlt.aps.lh.mapper.LhSharedMouldPatEntityMapper;
import com.zlt.aps.lh.service.ILhSharedMouldPatService;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 共用模具花纹配置服务实现类
 * <p>
 * 唯一性校验逻辑：工厂编号 + 物料编码 + 主花纹 + 模具号
 *
 * @author zlt
 * @date 2026-05-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhSharedMouldPatServiceImpl extends AbstractDocService<LhSharedMouldPat> implements ILhSharedMouldPatService {

    @Autowired
    private LhSharedMouldPatEntityMapper lhSharedMouldPatEntityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH_SHARED_MOULD_PAT";
    }

    @Override
    public int save(LhSharedMouldPat entity) {
        // 字段长度边界校验
        validateFieldLength(entity);

        if (entity.getId() != null) {
            entity.setBaseVale(entity.getId());
        } else {
            entity.setBaseVale(null);
            entity.setUpdateBy(SecurityUtils.getUsername());
            entity.setUpdateTime(new Date());
        }
        return super.save(entity);
    }

    /**
     * 字段长度边界校验
     * <p>
     * 校验规格、主花纹、模具号、花纹块字段是否超过最大长度限制
     *
     * @param entity 校验对象
     */
    private void validateFieldLength(LhSharedMouldPat entity) {
        if (entity == null) {
            return;
        }
        // 规格最大长度300
        if (StringUtil.isNotBlank(entity.getSpecifications()) && entity.getSpecifications().length() > 300) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.specificationsOverflow"));
        }
        // 主花纹最大长度50
        if (StringUtil.isNotBlank(entity.getMainPattern()) && entity.getMainPattern().length() > 50) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.mainPatternOverflow"));
        }
        // 模具号最大长度64
        if (StringUtil.isNotBlank(entity.getMouldNo()) && entity.getMouldNo().length() > 64) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.mouldNoOverflow"));
        }
        // 花纹块最大长度64
        if (StringUtil.isNotBlank(entity.getPatternBlock()) && entity.getPatternBlock().length() > 64) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.patternBlockOverflow"));
        }
    }

    /**
     * 查询列表
     *
     * @param queryWrapper 查询条件
     * @return 结果列表
     */
    @Override
    public List<LhSharedMouldPat> selectList(QueryWrapper<LhSharedMouldPat> queryWrapper) {
        return lhSharedMouldPatEntityMapper.selectList(queryWrapper);
    }

    /**
     * 导入数据
     * <p>
     * 校验逻辑：
     * 1. 工厂编号必填
     * 2. 物料编码必填
     * 3. 花纹块必填
     * 4. 模具号必填
     * 5. 唯一性校验：工厂编号 + 物料编码 + 主花纹 + 模具号
     */
    @Override
    public AjaxResult importData(List<LhSharedMouldPat> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<LhSharedMouldPat> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // Step1: 基础数据校验 + 设置行号
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhSharedMouldPat docEntity = list.get(i);
            docEntity.setRowNo(errorNum);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // Step2: 获取校验通过的数据列表
        List<LhSharedMouldPat> validList = list.stream()
                .filter(item -> item.getId() == null || item.getId() != -999L)
                .collect(Collectors.toList());

        // Step3: 批量预取物料主数据，用于反显物料描述
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(validList)) {
            List<String> materialCodeList = validList.stream()
                    .map(LhSharedMouldPat::getMaterialCode)
                    .filter(StringUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(materialCodeList)) {
                List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(materialCodeList, 900);
                List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
                for (List<String> codes : splitList) {
                    LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<MdmMaterialInfo>()
                            .in(MdmMaterialInfo::getMaterialCode, codes);
                    materialInfoList.addAll(mdmMaterialInfoEntityMapper.selectList(wrapper));
                }
                if (CollectionUtils.isNotEmpty(materialInfoList)) {
                    materialInfoMap = materialInfoList.stream()
                            .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (s1, s2) -> s1));
                }
            }
        }

        // Step4: Excel内数据重复校验

        if (CollectionUtils.isNotEmpty(validList)) {
            // 按工厂+物料编码+模具号+主花纹分组（主花纹可为空，用空字符串占位）
            Map<String, List<LhSharedMouldPat>> repeatMap = validList.stream()
                    .filter(item -> StringUtil.isNotBlank(item.getFactoryCode())
                            && StringUtil.isNotBlank(item.getMaterialCode())
                            && StringUtil.isNotBlank(item.getMouldNo()))
                    .collect(Collectors.groupingBy(item -> item.getFactoryCode() + "_"
                            + item.getMaterialCode() + "_"
                            + item.getMouldNo() + "_"
                            + (StringUtil.isNotBlank(item.getMainPattern()) ? item.getMainPattern() : "")));

            // 遍历进行校验
            List<LhSharedMouldPat> checkList = new ArrayList<>();
            for (LhSharedMouldPat docEntity : validList) {
                int errorNum = docEntity.getRowNo();
                boolean isCan = true;

                // 必填字段校验 - 工厂编号
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.factoryCodeRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 物料编码
                if (StringUtil.isBlank(docEntity.getMaterialCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.materialCodeRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 物料编码存在性校验 - 物料编码必须在物料主数据中存在
                if (StringUtil.isNotBlank(docEntity.getMaterialCode()) && !materialInfoMap.containsKey(docEntity.getMaterialCode())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.materialCodeNotExist"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 花纹块
                if (StringUtil.isBlank(docEntity.getPatternBlock())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.patternBlockRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // 必填字段校验 - 模具号
                if (StringUtil.isBlank(docEntity.getMouldNo())) {
                    isCan = false;
                    String message = String.format(I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.mouldNoRequired"), errorNum);
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                            errorNum, message, importErrorLogs);
                }

                // Excel内重复校验 - 工厂+物料编码+模具号+主花纹（主花纹可为空）
                if (StringUtil.isNotBlank(docEntity.getFactoryCode())
                        && StringUtil.isNotBlank(docEntity.getMaterialCode())
                        && StringUtil.isNotBlank(docEntity.getMouldNo())) {
                    String key = docEntity.getFactoryCode() + "_"
                            + docEntity.getMaterialCode() + "_"
                            + docEntity.getMouldNo() + "_"
                            + (StringUtil.isNotBlank(docEntity.getMainPattern()) ? docEntity.getMainPattern() : "");
                    List<LhSharedMouldPat> repeatList = repeatMap.get(key);
                    if (CollectionUtils.isNotEmpty(repeatList) && repeatList.size() > 1) {
                        isCan = false;
                        String message = String.format(I18nUtil.getMessage("import.validated.repeat"), errorNum,
                                repeatList.stream()
                                        .map(item -> String.valueOf(item.getRowNo()))
                                        .filter(row -> !row.equals(String.valueOf(errorNum)))
                                        .collect(Collectors.joining(", ")));
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, message, importErrorLogs);
                    }
                }

                // 数据库唯一性校验
                if (isCan) {
                    if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                        checkList.add(docEntity);
                    } else {
                        if (updateSupport) {
                            // 查询已存在记录进行更新
                            LambdaQueryWrapper<LhSharedMouldPat> queryWrapper = Wrappers.lambdaQuery();
                            queryWrapper.eq(LhSharedMouldPat::getFactoryCode, docEntity.getFactoryCode());
                            queryWrapper.eq(LhSharedMouldPat::getMaterialCode, docEntity.getMaterialCode());
                            // 主花纹可为空，为空时使用isNull条件
                            if (StringUtil.isNotBlank(docEntity.getMainPattern())) {
                                queryWrapper.eq(LhSharedMouldPat::getMainPattern, docEntity.getMainPattern());
                            } else {
                                queryWrapper.isNull(LhSharedMouldPat::getMainPattern);
                            }
                            queryWrapper.eq(LhSharedMouldPat::getMouldNo, docEntity.getMouldNo());
                            LhSharedMouldPat existEntity = lhSharedMouldPatEntityMapper.selectOne(queryWrapper);
                            if (existEntity != null) {
                                docEntity.setId(existEntity.getId());
                                checkList.add(docEntity);
                            }
                        } else {
                            isCan = false;
                            String notUniqueMsg = I18nUtil.getMessage("ui.data.alert.lhSharedMouldPat.notUnique.withRow");
                            String message = String.format(notUniqueMsg, errorNum);
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, message, importErrorLogs);
                        }
                    }
                }

                if (!isCan) {
                    failureNum++;
                    docEntity.setId(-999L);
                }
            }
            importList = checkList;
            successNum = importList.size();
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        // Step5: 批量导入 - 分离新增和更新数据
        List<LhSharedMouldPat> insertList = importList.stream()
                .filter(entity -> entity.getId() == null)
                .collect(Collectors.toList());
        List<LhSharedMouldPat> updateList = importList.stream()
                .filter(entity -> entity.getId() != null)
                .collect(Collectors.toList());

        // 设置物料描述（根据物料编码从物料主数据反显）
        String currentUser = SecurityUtils.getUsername();
        Date currentTime = new Date();
        for (LhSharedMouldPat entity : importList) {
            // 根据物料编码反显物料描述
            if (StringUtil.isNotBlank(entity.getMaterialCode()) && materialInfoMap.containsKey(entity.getMaterialCode())) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(entity.getMaterialCode());
                entity.setMaterialDesc(materialInfo.getMaterialDesc());
            }
        }

        // 批量插入 - 设置审计字段和逻辑删除标识
        if (CollectionUtils.isNotEmpty(insertList)) {
            for (LhSharedMouldPat entity : insertList) {
                entity.setIsDelete(Integer.valueOf(ApsConstant.DEL_FLAG_NORMAL));
                entity.setCreateBy(currentUser);
                entity.setCreateTime(currentTime);
                entity.setUpdateBy(currentUser);
                entity.setUpdateTime(currentTime);
            }
            lhSharedMouldPatEntityMapper.insertBatch(insertList);
        }

        // 批量更新 - 设置更新审计字段
        if (CollectionUtils.isNotEmpty(updateList)) {
            for (LhSharedMouldPat entity : updateList) {
                entity.setUpdateBy(currentUser);
                entity.setUpdateTime(currentTime);
            }
            lhSharedMouldPatEntityMapper.updateBatch(updateList);
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 校验唯一性
     * <p>
     * 唯一性规则：工厂编号 + 物料编码 + 主花纹 + 模具号
     *
     * @param entity 校验对象
     * @return 唯一性结果
     */
    @Override
    public String checkUnique(LhSharedMouldPat entity) {
        if (entity == null || StringUtil.isBlank(entity.getFactoryCode())) {
            return UserConstants.NOT_UNIQUE;
        }

        LambdaQueryWrapper<LhSharedMouldPat> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(entity.getId() != null, LhSharedMouldPat::getId, entity.getId());
        wrapper.eq(LhSharedMouldPat::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(LhSharedMouldPat::getMaterialCode, entity.getMaterialCode());
        // 主花纹可为空，为空时使用isNull条件
        if (StringUtil.isNotBlank(entity.getMainPattern())) {
            wrapper.eq(LhSharedMouldPat::getMainPattern, entity.getMainPattern());
        } else {
            wrapper.isNull(LhSharedMouldPat::getMainPattern);
        }
        wrapper.eq(LhSharedMouldPat::getMouldNo, entity.getMouldNo());

        Long count = lhSharedMouldPatEntityMapper.selectCount(wrapper);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }

        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "materialCode", "mainPattern", "mouldNo");
    }
}
