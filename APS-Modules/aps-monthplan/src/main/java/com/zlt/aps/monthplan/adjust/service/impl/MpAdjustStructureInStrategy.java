package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.monthplan.adjust.engine.MpWeekRollAdjustEngine;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.monthplan.api.annotation.WeekAdjustType;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.entity.MpTrialPlan;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 结构内调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_IN)
public class MpAdjustStructureInStrategy extends AbstractBaseWeekAdjustService {

    @Autowired
    private IMpAdjustStructureInService mpAdjustStructureInService;

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 1、设置版本号
        setVersion(contextDTO, BusiConstant.WeekRollAdjust.VERSION_PREFIX);
        // 2、构建结构内调整明细
        List<MpAdjustDetailVo> adjustDetailList = buildAdjustDetailList(contextDTO);
        // 3、构建结构内调整明细（试制量试计划）
        List<MpAdjustDetailVo> adjustDetailByTrialList = buildAdjustDetailByTrialList(contextDTO);
        // 结构内调整明细结果列表
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        resultList.addAll(adjustDetailByTrialList);
        resultList.addAll(adjustDetailList);
        // 未获取到调整记录，抛出异常
        Assert.isFalse(PubUtil.isEmpty(resultList), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindAdjustDetailList"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 4、按照结构、物料编码维度进行分组，并汇总订单量
        resultList = sumByStructureAndMaterial(resultList);
        contextDTO.setAdjustDetailList(resultList);
        // 5、设置净需求
        setCurrentNetQty(contextDTO);
        // 6、设置计划剩余排产量、计划已排产量、已生产量
        setMonthUnScheduledQty(contextDTO);
        // 7、筛选：|净需求 - 计划剩余排产量| > 0的数据
        filterAdjustList(contextDTO.getAdjustDetailList());
        // 筛选后数据为空，抛出异常
        Assert.isFalse(PubUtil.isEmpty(contextDTO.getAdjustDetailList()), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notMatchAdjustDetailList"), contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 8、设置其他字段
        setOtherField(contextDTO);
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {
        //注：结构内自动调整列表：关单直接排除，同时取订单列表与月计划最大并集；
        //结构内调整记录
        contextDTO.setMpAdjustStructureInList(mpAdjustStructureInService.selectMpAdjustStructureInList(contextDTO));
        //1.结构内订单调整记录空检查
        if (PubUtil.isEmpty(contextDTO.getMpAdjustStructureInList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.orderAdjustRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }
        //2.月计划定稿数据空检查
        if (PubUtil.isEmpty(contextDTO.getFactoryMonthPlanProdFinalList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }

        //规格挑选可用机台
        //4.按结构序列化分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpProdFinalMap = contextDTO.getFactoryMonthPlanProdFinalList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        Map<String, List<MpAdjustStructureIn>> adjustStructInMap = contextDTO.getMpAdjustStructureInList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        Date startTime,endTime;
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        for (Map.Entry<String, List<MpAdjustStructureIn>> entry : adjustStructInMap.entrySet()) {
            //结构内，按结构分别调整
            contextDTO.setStructureName(entry.getKey());
            List<MpStructureAllocation> structureAllocationList = contextDTO.getStructureAllocationList().stream().filter(x->x.getStructureName().equals(contextDTO.getStructureName())).collect(Collectors.toList());
            contextDTO.setOneStructureAllocationList(structureAllocationList);
            //初始锁定日
            contextDTO.setLockEndDay(mpAdjustStructureInService.getLockEndDay(contextDTO));
            //初始结构收尾日
            contextDTO.setStructureDeadLine(mpAdjustStructureInService.getStructureDeadline(contextDTO));
            //初始化日志
            contextDTO.setLogDetail(new StringBuilder());
            //规格挑选可用机台
            startTime = new Date();
            contextDTO.getLogDetail().append(String.format("结构:%s,自动调整,开始时间:%s",entry.getKey(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
            weekRollAdjustEngine.structureInAdjustForOne(contextDTO,entry.getValue(), MapUtils.getObject(mpProdFinalMap, entry.getKey(), new ArrayList<>()));
            endTime = new Date();
            contextDTO.getLogDetail().append(String.format("结构:%s,自动调整,结束时间:%s,总耗时:%s毫秒",entry.getKey(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);
            //保存调整日志
            saveMpAdjustLog(contextDTO);
        }
    }

    @Override
    public void doConfirmAdjust(MpRollAdjustContextDTO contextDTO) {

    }

    @Override
    public void specialInit(MpRollAdjustContextDTO contextDTO) {

    }

    @Override
    public void specialCheck(MpRollAdjustContextDTO contextDTO) {

    }

    /**
     * 构建结构内调整明细（试制量试计划）
     * @param contextDTO
     * @return
     */
    private List<MpAdjustDetailVo> buildAdjustDetailByTrialList(MpRollAdjustContextDTO contextDTO) {
        // 试制量试计划列表
        List<MpTrialPlan> trialPlanList = contextDTO.getMpTrialPlanList();
        // 月度生产计划列表
        List<FactoryMonthPlanFinalAdjustVo> monthPlanProdList = contextDTO.getFactoryMonthPlanProdFinalList();
        // 结果集初始化
        List<MpAdjustDetailVo> resultList = new ArrayList<>();
        // 任一列表为空则直接返回空结果
        if (PubUtil.isEmpty(trialPlanList) || PubUtil.isEmpty(monthPlanProdList)) {
            return resultList;
        }
        // 生产计划列表按照物料编码进行分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> monthPlanMap = monthPlanProdList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanFinalAdjustVo::getMaterialCode));
        // 遍历试制量试计划列表，匹配生产计划
        for (MpTrialPlan trialPlan : trialPlanList) {
            String materialCode = trialPlan.getMaterialCode();
            // 物料编码为空则跳过
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            matchMonthPlanList(contextDTO,resultList,materialCode,monthPlanMap,
                    Convert.toInt(trialPlan.getTrialQty(),0), ApsConstant.TRUE);
        }
        return resultList;
    }

    /**
     * 筛选：|净需求 - 计划剩余排产量| > 0的数据
     * @param adjustList
     */
    private void filterAdjustList(List<MpAdjustDetailVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(),0);
            Integer monthUnScheduledQty = Convert.toInt(adjust.getMonthUnScheduledQty(),0);
            return Math.abs(currentNetQty - monthUnScheduledQty) == 0;
        });
    }

    @Override
    public void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getAdjustDetailList())) {
            return;
        }
        List<MpAdjustDetailVo> resultList = baseDao.saveWithQuery(contextDTO.getAdjustDetailList());
        contextDTO.setAdjustDetailList(resultList);
    }

}
