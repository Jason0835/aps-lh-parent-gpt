package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.aps.mp.factory.mapper.MpAdjustPlanRequireInfoEntityMapper;
import com.zlt.aps.mp.factory.service.IMpAdjustPlanRequireInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 计划调整信息业务服务实现
 *
 * @author ZLT
 * 20260716
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MpAdjustPlanRequireInfoServiceImpl extends AbstractDocService<MpAdjustPlanRequireInfo> implements IMpAdjustPlanRequireInfoService {

    private final MpAdjustPlanRequireInfoEntityMapper adjustPlanInfoMapper;

    private final MdmSkuStructureRefEntityMapper mdmSkuStructureRefMapper;

    @Override
    public List<MpAdjustPlanRequireInfo> getListByCondition(QueryWrapper<MpAdjustPlanRequireInfo> wrapper) {
        if (null == wrapper) {
            return Collections.emptyList();
        }
        return adjustPlanInfoMapper.selectList(wrapper);
    }

    @Override
    public String[] getQueryFormulas() {
        return new String[]{
                "materialDesc -> getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, materialCode)"
        };
    }

    @Override
    public AjaxResult importData(List<MpAdjustPlanRequireInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<MpAdjustPlanRequireInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // 第一轮：基本校验（必填、格式、重复行）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpAdjustPlanRequireInfo docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 第二轮：业务校验与写入（业务键：FACTORY_CODE + MATERIAL_CODE）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpAdjustPlanRequireInfo docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            // 调整后计划量统一由导入值计算；本月计划产量或调整数量为空时按 0 处理。
            this.calculateAdjustFinalQty(docEntity);
            // 调整原因与调整类型一致性校验：原因编码前两位必须等于调整类型编码（如类型 01 追加计划 -> 原因 0101~0109）
            if (StringUtils.isNotBlank(docEntity.getPlanAdjustType())
                    && StringUtils.isNotBlank(docEntity.getAdjustReason())
                    && !docEntity.getAdjustReason().startsWith(docEntity.getPlanAdjustType())) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(I18nUtil.getMessage("ui.message.mpAdjustPlanInfo.typeReasonMismatch"), errorNum), importErrorLogs);
                continue;
            }
            // 产品结构与物料编码必须在 SKU 结构关系基础数据中存在有效对应关系。
            if (!this.existsSkuStructureRef(docEntity)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(I18nUtil.getMessage("ui.message.mpAdjustPlanInfo.skuStructureRefNotExist"),
                                errorNum, docEntity.getFactoryCode(), docEntity.getStructureName(), docEntity.getMaterialCode()),
                        importErrorLogs);
                continue;
            }
            MpAdjustPlanRequireInfo exist = getExistByFactoryAndMaterial(docEntity);
            if (exist == null) {
                docEntity.setIsImport(YesOrNoEnum.YES.getCode());
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                copyBusinessFields(exist, docEntity);
                adjustPlanInfoMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            successNum += baseDao.saveBatch(importList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 计算调整后计划量：本月计划产量 - 调整数量，空值按 0 处理。
     */
    private void calculateAdjustFinalQty(MpAdjustPlanRequireInfo entity) {
        int monthPlanQty = entity.getMonthPlanQty() == null ? 0 : entity.getMonthPlanQty();
        int adjustPlanQty = entity.getAdjustPlanQty() == null ? 0 : entity.getAdjustPlanQty();
        entity.setAdjustFinalQty(monthPlanQty - adjustPlanQty);
    }

    /**
     * 校验分厂、产品结构、物料编码在 SKU 结构关系基础数据中存在精确对应关系。
     */
    private boolean existsSkuStructureRef(MpAdjustPlanRequireInfo entity) {
        if (StringUtils.isAnyBlank(entity.getFactoryCode(), entity.getStructureName(), entity.getMaterialCode())) {
            return false;
        }
        LambdaQueryWrapper<MdmSkuStructureRef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmSkuStructureRef::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(MdmSkuStructureRef::getStructureName, entity.getStructureName());
        wrapper.eq(MdmSkuStructureRef::getMaterialCode, entity.getMaterialCode());
        return mdmSkuStructureRefMapper.selectCount(wrapper) > 0;
    }

    /**
     * 按业务键（分厂 + 物料编码）查询已存在记录（未删除数据），最多取一条。
     * 业务上同一分厂同一物料可存在多条不同调整日期/类型的记录，导入更新只处理第一条。
     */
    private MpAdjustPlanRequireInfo getExistByFactoryAndMaterial(MpAdjustPlanRequireInfo entity) {
        LambdaQueryWrapper<MpAdjustPlanRequireInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpAdjustPlanRequireInfo::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(MpAdjustPlanRequireInfo::getMaterialCode, entity.getMaterialCode());
        wrapper.last("LIMIT 1");
        return adjustPlanInfoMapper.selectOne(wrapper);
    }

    /**
     * 将导入行的业务字段复制到已存在记录（保留主键与审计字段）
     */
    private void copyBusinessFields(MpAdjustPlanRequireInfo exist, MpAdjustPlanRequireInfo source) {
        exist.setLocationType(source.getLocationType());
        exist.setAdjustDate(source.getAdjustDate());
        exist.setArea(source.getArea());
        exist.setPlanAdjustType(source.getPlanAdjustType());
        exist.setAdjustReason(source.getAdjustReason());
        exist.setStructureName(source.getStructureName());
        exist.setStructureType(source.getStructureType());
        exist.setMesMaterialCode(source.getMesMaterialCode());
        exist.setMaterialDesc(source.getMaterialDesc());
        exist.setMonthPlanQty(source.getMonthPlanQty());
        exist.setAdjustPlanQty(source.getAdjustPlanQty());
        exist.setAdjustFinalQty(source.getAdjustFinalQty());
        exist.setRealAdjustQty(source.getRealAdjustQty());
        exist.setRemark(source.getRemark());
    }

    @Override
    protected String getDocTypeCode() {
        return "S2-0801";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode(getDocTypeCode());
        return sysDocType;
    }
}
