package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MpMouldDeliveryPlanEntityMapper;
import com.zlt.aps.maindata.service.IMpMouldDeliveryPlanService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpMouldDeliveryPlan;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMouldDeliveryPlanServiceImpl.java
 * 描    述：MpMouldDeliveryPlanServiceImpl模具到货计划业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MpMouldDeliveryPlanServiceImpl extends AbstractDocService<MpMouldDeliveryPlan> implements IMpMouldDeliveryPlanService {

    @Autowired
    private MpMouldDeliveryPlanEntityMapper mouldDeliveryPlanEntityMapper;

    @Autowired
    private FactoryParamMapper factoryParamMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MP0203";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0203");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpMouldDeliveryPlan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpMouldDeliveryPlan.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "mouldCode", "materialCode"));
    }

    /**
     * 根据计划发货日期获取计划上机日期
     *
     * @param entity 计划发货日期
     * @return 结果
     */
    @Override
    public AjaxResult getBoardingDate(MpMouldDeliveryPlan entity) {
        Date shipmentDate = entity.getShipmentDate();
        if (Objects.isNull(shipmentDate)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mpMouldDeliveryPlan.getBoardingDate.shipmentDateNull"));
        }
        LambdaQueryWrapper<FactoryParam> wrapper = new LambdaQueryWrapper<>();
        String code = MonthPlanEnums.MODULE_ARRIVAL_DAYS.getCode();
        wrapper.eq(FactoryParam::getParamCode, code);
        FactoryParam factoryParam = factoryParamMapper.selectOne(wrapper);
        if (Objects.isNull(factoryParam)) {
            return AjaxResult.success(String.format(I18nUtil.getMessage("ui.data.alert.mpMouldDeliveryPlan.getBoardingDate.paramsNull"), code), DateUtils.parseDateToStr("yyyy-MM-dd", shipmentDate));
        }
        String defaultValue = factoryParam.getDefauleValue();
        String paramValue = StringUtils.defaultIfBlank(factoryParam.getParamValue(), defaultValue);
        Date date = DateUtils.addDays(shipmentDate, Integer.parseInt(paramValue));
        return AjaxResult.success(DateUtils.parseDateToStr("yyyy-MM-dd", date));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MpMouldDeliveryPlan> list, List<MpMouldDeliveryPlan> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 关联物料表赋值规格、花纹、品牌
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(16);
        List<String> materialCodeList = list.stream().map(MpMouldDeliveryPlan::getMaterialCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(materialCodeList)) {
            List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(materialCodeList, 100);
            List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
            for (List<String> codeList : splitList) {
                LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<MdmMaterialInfo>()
                        .in(MdmMaterialInfo::getMaterialCode, codeList)
                        .eq(MdmMaterialInfo::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);
                materialInfoList.addAll(materialInfoEntityMapper.selectList(wrapper));
            }

            if (CollectionUtils.isNotEmpty(materialInfoList)) {
                materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(item -> String.join("|", item.getFactoryCode(), item.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
            }
            serviceCheckParams.put("materialInfoMap", materialInfoMap);
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MpMouldDeliveryPlan importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Date shipmentDate = importDocEntity.getShipmentDate();
        Date boardingDate = getBoardingDate(shipmentDate);
        importDocEntity.setBoardingDate(boardingDate);
        if (serviceCheckParams.containsKey("materialInfoMap")) {
            Map<String, MdmMaterialInfo> materialInfoMap = (Map<String, MdmMaterialInfo>) serviceCheckParams.get("materialInfoMap");
            String materialCode = FactoryConstant.DEFAULT_FACTORY_CODE + "|" + importDocEntity.getMaterialCode();
            if (materialInfoMap.containsKey(materialCode)) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                importDocEntity.setMaterialDesc(materialInfo.getMaterialDesc());
            } else {
                String message = I18nUtil.getMessage("ui.data.column.mdmMaterialInfo.dbNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 获取计划上机日期
     *
     * @param shipmentDate 计划发货日期
     * @return 结果
     */
    private Date getBoardingDate(Date shipmentDate) {
        if (Objects.isNull(shipmentDate)) {
            return shipmentDate;
        }
        LambdaQueryWrapper<FactoryParam> wrapper = new LambdaQueryWrapper<>();
        String code = MonthPlanEnums.MODULE_ARRIVAL_DAYS.getCode();
        wrapper.eq(FactoryParam::getParamCode, code);
        FactoryParam factoryParam = factoryParamMapper.selectOne(wrapper);
        if (Objects.isNull(factoryParam)) {
            return shipmentDate;
        }
        String defaultValue = factoryParam.getDefauleValue();
        String paramValue = StringUtils.defaultIfBlank(factoryParam.getParamValue(), defaultValue);
        return DateUtils.addDays(shipmentDate, Integer.parseInt(paramValue));
    }

    /**
     * 更新主花纹到物料
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult updateMainPatternToMaterial(MpMouldDeliveryPlan queryVO) {
        queryVO.setBaseVale(null);
        mouldDeliveryPlanEntityMapper.updateMainPatternToMaterial(queryVO);
        return AjaxResult.success();
    }
}
