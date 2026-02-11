package com.zlt.aps.monthplan.adjust.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.ProductionProcessesTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.maindata.service.IRawSpecialMaterialRecordService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.monthplan.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmStructureLhRatioEntityMapper;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.monthplan.factory.service.impl.MoldCavityInsertMaxValueCalculatorImpl;
import com.zlt.aps.monthplan.mdm.dto.DataDTO;
import com.zlt.aps.monthplan.mdm.handler.DataManager;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.util.StopWatch;

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
    private ProductionMdmDataService productionSchedulingDataService;

    @Autowired
    private MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;

    @Autowired
    private MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;

    @Autowired
    private MdmStructureLhRatioEntityMapper mdmStructureLhRatioEntityMapper;

    @Autowired
    private IRawSpecialMaterialRecordService rawSpecialMaterialRecordService;

    @Autowired
    private DataManager dataManager;



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
        for (FactoryMonthPlanFinalAdjustVo mpFinalVo:mpFinalAdjustList){
            mpFinalVo.setAdjustDetail(new StringBuilder());
        }
        // 设置是否特殊材料
        setSpecialMaterial(contextDTO.getFactoryCode(), mpFinalAdjustList);
        return mpFinalAdjustList;
    }


    /**
     * 设置是否特殊材料
     * @param factoryCode
     * @param mpFinalAdjustList
     */
    public void setSpecialMaterial(String factoryCode, List<FactoryMonthPlanFinalAdjustVo> mpFinalAdjustList) {
        if (PubUtil.isEmpty(mpFinalAdjustList)) {
            return;
        }

        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 查询BOM物料消耗明细
        CompletableFuture<List<MdmMaterialConsumeDetail>> materialConsumeDetailFuture = CompletableFuture.supplyAsync(
                () -> queryMaterialConsumeDetailList(factoryCode)
        );
        // 查询特殊材料记录
        CompletableFuture<List<RawSpecialMaterialRecord>> rawSpecialMaterialRecordFuture = CompletableFuture.supplyAsync(
                () -> querySpecialMaterialRecordList(factoryCode)
        );

        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    materialConsumeDetailFuture,
                    rawSpecialMaterialRecordFuture
            ).join();

            log.info("设置是否特殊材料 ==> 并行查询数据执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("查询数据失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
        }

        List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList = materialConsumeDetailFuture.join();
        List<RawSpecialMaterialRecord> specialMaterialList = rawSpecialMaterialRecordFuture.join();

        for (FactoryMonthPlanFinalAdjustVo monthPlan : mpFinalAdjustList) {
            // 设置是否含有特殊材料
            boolean isHasSpecialMaterial = rawSpecialMaterialRecordService.hasSpecialMaterial(monthPlan.getEmbryoCode(), mdmMaterialConsumeDetailList, specialMaterialList);
            monthPlan.setHasSpecialMaterial(isHasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);
        }
    }



    /**
     * 查询特殊材料记录
     *
     * @param factoryCode
     */
    private List<RawSpecialMaterialRecord> querySpecialMaterialRecordList(String factoryCode) {
        RawSpecialMaterialRecord queryVO = new RawSpecialMaterialRecord();
        queryVO.setFactoryCode(factoryCode);

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        return dataManager.listSpecialMaterials(dataDTO);
    }


    /**
     * 查询BOM物料消耗明细
     *
     * @param factoryCode
     */
    private List<MdmMaterialConsumeDetail> queryMaterialConsumeDetailList(String factoryCode) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(factoryCode);

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        return dataManager.listMaterialConsumeDetails(dataDTO);
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
        paramCodeList.add(MonthPlanEnums.WEEK_ROLL_ADJUST_DATE.getCode());
        return productionSchedulingDataService.getFactoryParamByCondition(context,paramCodeList);
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
    public Map<Integer, DailyMouldAvailabilityResult> getCavityAndBlockQtyMap(MpRollAdjustContextDTO contextDTO) {
        //1.按年月获取型腔及活块数据
        List<DailyMouldAvailabilityResult> cavity2BlockList = moldCavityInsertMaxValueCalculator.moldCavityInsertMaxValueCalculator(contextDTO.getMpYear(),contextDTO.getMpMonth(),contextDTO.getFactoryCode(),null,null);
        if (PubUtil.isEmpty(cavity2BlockList)){
            throw new BusinessException(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.noGetCavityAndBlock"));
        }
        //2.按日进行序列化
        Map<Integer, DailyMouldAvailabilityResult> cavity2BlockMap = cavity2BlockList.stream().collect(Collectors.groupingBy(item->item.getDayOfCycle(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));
        return cavity2BlockMap;
    }

    @Override
    public Map<Integer, MdmWorkCalendar> getWorkCalendarMap(MpRollAdjustContextDTO contextDTO) {
        QueryWrapper<MdmWorkCalendar> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", contextDTO.getFactoryCode());
        queryWrapper.eq("PROC_CODE", ProductionProcessesTypeEnum.MONTH_PLAN.getProcCode());
        queryWrapper.eq("YEAR", contextDTO.getMpYear());
        queryWrapper.eq("MONTH", contextDTO.getMpMonth());
        List<MdmWorkCalendar> workCalendarList = mdmWorkCalendarEntityMapper.selectList(queryWrapper);
        Map<Integer, MdmWorkCalendar> workCalendarMap = workCalendarList.stream().collect(Collectors.groupingBy(item->item.getDay(),
                Collectors.collectingAndThen(Collectors.toList(),m-> {
                    return m.get(0);
                })));
        return workCalendarMap;
    }

    @Override
    public List<MdmStructureLhRatio> getStructureLhRatio(MpRollAdjustContextDTO contextDTO) {
        LambdaQueryWrapper<MdmStructureLhRatio> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmStructureLhRatio::getFactoryCode, contextDTO.getFactoryCode());
        return mdmStructureLhRatioEntityMapper.selectList(queryWrapper);
    }
}
