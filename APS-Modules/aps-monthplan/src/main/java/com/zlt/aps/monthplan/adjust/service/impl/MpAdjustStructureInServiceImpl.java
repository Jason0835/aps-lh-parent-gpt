package com.zlt.aps.monthplan.adjust.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionProcessesTypeEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.monthplan.common.utils.StringUtil;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.service.impl.MoldCavityInsertMaxValueCalculatorImpl;
import com.zlt.sysdef.domain.SysDocType;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureInServiceImpl.java
 * 描    述：MpAdjustStructureInServiceImpl调整-结构内调整记录业务层处理
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MpAdjustStructureInServiceImpl extends AbstractDocService<MpAdjustStructureIn>  implements IMpAdjustStructureInService {

    @Autowired
    private FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    @Autowired
    private MpStructureAllocationEntityMapper structureAllocationEntityMapper;

    @Autowired
    private MpAdjustStructureInEntityMapper structureInEntityMapper;

    @Autowired
    private ProductionSchedulingDataService productionSchedulingDataService;

    @Autowired
    private MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;

    @Autowired
    private MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;



    @Override
    protected String getDocTypeCode() {
        return "MP0802";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0802");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustStructureIn docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustStructureIn.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<MpAdjustStructureIn> selectMpAdjustStructureInList(MpRollAdjustContextDTO contextDTO) {
        QueryWrapper<MpAdjustStructureIn> structureInQueryWrapper = new QueryWrapper<>();
        structureInQueryWrapper.eq("FACTORY_CODE", contextDTO.getFactoryCode());
        structureInQueryWrapper.eq("YEAR", contextDTO.getMpYear());
        structureInQueryWrapper.eq("MONTH", contextDTO.getMpMonth());
        structureInQueryWrapper.eq("VERSION", contextDTO.getVersion());
        return structureInEntityMapper.selectList(structureInQueryWrapper);
    }

    @Override
    public List<FactoryMonthPlanFinalAdjustVo> selectMpFinalList(MpRollAdjustContextDTO contextDTO) {
        List<FactoryMonthPlanFinalAdjustVo> mpFinalAdjustList = factoryMonthPlanProdFinalMapper.selectMpFinalList(contextDTO.getMpYear(),contextDTO.getMpMonth(),contextDTO.getFactoryCode());
       /* if (PubUtil.isNotEmpty(mpFinalAdjustList) && !StringUtil.isEmptyWithTrim(contextDTO.getStructureName())){
            mpFinalAdjustList = mpFinalAdjustList.stream().filter(x->x.getStructureName().equals(contextDTO.getStructureName())).collect(Collectors.toList());
        }*/
        return mpFinalAdjustList;
    }

    @Override
    public Map<String, Object> getMpWeekAdjustParam(String factoryCode,String productType) {
        Context context = new Context();
        context.setFactoryCode(factoryCode);
        context.setProductType(ProductTypeEnum.getEnumByValue(productType));
        List<String> paramCodeList = new ArrayList<>();
        paramCodeList.add(MonthPlanEnums.SINGLE_CX_MACHINE_LOCK_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.MULTI_CX_MACHINE_LOCK_DAYS.getCode());
        paramCodeList.add(MonthPlanEnums.TRIAL_SKU_SINGLE_DAY_QTY_UP_LIMIT.getCode());
        paramCodeList.add(MonthPlanEnums.TRIAL_SKU_STRUCT_START_DAY_IS_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.TRIAL_SKU_SUNDAY_IS_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode());
        paramCodeList.add(MonthPlanEnums.SKU_SECOND_PRODUCTION.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MAX_CAPACITY.getCode());
        paramCodeList.add(MonthPlanEnums.MAX_BOOST_DAY.getCode());
        paramCodeList.add(MonthPlanEnums.BOOST_PRODUCTION_TYPE_VALUE.getCode());
        return  productionSchedulingDataService.getFactoryParamByCondition(context,paramCodeList);
    }

    @Override
    public List<MpStructureAllocation> selectMpStructureAllocationList(MpRollAdjustContextDTO contextDTO) {
        QueryWrapper<MpStructureAllocation> wrapper = new QueryWrapper<>();
        wrapper.eq( "FACTORY_CODE", contextDTO.getFactoryCode());
        wrapper.eq("PRODUCTION_VERSION", contextDTO.getProductionVersion());
        return structureAllocationEntityMapper.selectList(wrapper);
    }

    @Override
    public Integer getLockEndDay(MpRollAdjustContextDTO contextDTO) {
        int lockDays = (Integer) contextDTO.getParamMap().get(MonthPlanEnums.SINGLE_CX_MACHINE_LOCK_DAYS.getCode());
        List<MpStructureAllocation> structureAllocationList = contextDTO.getOneStructureAllocationList();
        if (PubUtil.isEmpty(structureAllocationList)){
            return lockDays;
        }
        //1、统计调整日成型机台数
        int iCount = 0;
        for (MpStructureAllocation allocation:structureAllocationList){
            if (contextDTO.getAdjustDay()>=allocation.getBeginDay() &&
                    contextDTO.getAdjustDay()<= allocation.getEndDay()){
                iCount +=1;
            }
        }
        //2、按成型机数，取月度生产计划锁定期天数
        if (iCount > 1){
            lockDays = (Integer)contextDTO.getParamMap().get(MonthPlanEnums.MULTI_CX_MACHINE_LOCK_DAYS.getCode());
        }
        // 今天算在内，故-1;
        lockDays = contextDTO.getAdjustDay() + lockDays -1;
        return lockDays > FactoryConstant.MONTH_MAX_DAY ? FactoryConstant.MONTH_MAX_DAY:lockDays;
    }

    @Override
    public void initStructureStartAndEndDay(MpRollAdjustContextDTO contextDTO) {
        int beginDay = FactoryConstant.MONTH_MAX_DAY;
        int endDay = 0;
        List<MpStructureAllocation> structureAllocationList = contextDTO.getOneStructureAllocationList();
        if (PubUtil.isNotEmpty(structureAllocationList)){
            // 取最大的成型机收尾日作为结构的收尾日
            for (MpStructureAllocation allocation:structureAllocationList){
                if (beginDay > allocation.getBeginDay()){
                    beginDay = allocation.getBeginDay();
                }
                if (endDay < allocation.getEndDay()){
                    endDay = allocation.getEndDay();
                }
            }
        }

        contextDTO.setStartDay(beginDay);
        contextDTO.setEndDay(endDay);
        contextDTO.setStructureStartDay(beginDay);
        contextDTO.setStructureDeadLine(endDay);
    }

    @Override
    public void initCavityAndBlockQty(MpRollAdjustContextDTO contextDTO) {
        //1.按年月获取型腔及活块数据
        Map<String, Object> cavity2BlockMap;
        try{
            cavity2BlockMap = moldCavityInsertMaxValueCalculator.moldCavityInsertMaxValueCalculator(contextDTO.getMpYear(),contextDTO.getMpMonth(),contextDTO.getFactoryCode(),null,null);
            if (PubUtil.isEmpty(cavity2BlockMap)){
                throw new BusinessException(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.noGetCavityAndBlock"));
            }
        }catch (Exception ex){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.getCavityAndBlockExcept"),
                    ex.getMessage()));
        }
        Map<Integer, Map<String, Object>> dailyDetailMap = (Map<Integer, Map<String, Object>>)cavity2BlockMap.get("dailyDetails");
        if (PubUtil.isEmpty(dailyDetailMap)){
            throw new BusinessException(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.noGetCavityAndBlock"));
        }
        //2.按日初始上下文：型腔及活块数据
        Map<Integer, Map<String, Set<String>>> dailyCavityMap = new HashMap<>();
        Map<Integer, Map<String, Set<String>>> dailyBlockMap = new HashMap<>();
        Map<String, Object> dayResult;

        /*dailyCavityMap = dayResult.get("cavityDetail");
        for (Map.Entry<Integer, Map<String, Object>> entry : dailyDetailMap.entrySet()) {
            dayResult = entry.getValue();
            dailyCavityMap.put(entry.getKey(),);
        }*/
    }

    @Override
    public List<MdmWorkCalendar> getWorkCalendarList(MpRollAdjustContextDTO contextDTO) {
        QueryWrapper<MdmWorkCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", contextDTO.getFactoryCode());
        queryWrapper.eq("PROC_CODE", ProductionProcessesTypeEnum.MONTH_PLAN.getProcCode());
        queryWrapper.eq("YEAR", contextDTO.getMpYear());
        queryWrapper.eq("MONTH", contextDTO.getMpMonth());
        return mdmWorkCalendarEntityMapper.selectList(queryWrapper);
    }
}
