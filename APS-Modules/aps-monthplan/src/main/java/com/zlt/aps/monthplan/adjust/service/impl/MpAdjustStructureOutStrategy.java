package com.zlt.aps.monthplan.adjust.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.monthplan.adjust.engine.MpWeekRollAdjustEngine;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureOutService;
import com.zlt.aps.monthplan.api.annotation.WeekAdjustType;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureIn;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureOut;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.api.domain.vo.MpAdjustDetailVo;
import com.zlt.aps.monthplan.api.enums.WeekAdjustTypeEnum;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 结构外调整策略
 * @author wengpc
 */
@Slf4j
@Service
@WeekAdjustType(adjustType = WeekAdjustTypeEnum.STRUCTURE_OUT)
public class MpAdjustStructureOutStrategy extends AbstractBaseWeekAdjustService {

    @Autowired
    private IMpAdjustStructureOutService mpAdjustStructureOutService;

    @Override
    public void doGenerateAdjust(MpRollAdjustContextDTO contextDTO) throws BusinessException {
        // 1、设置版本号
        setVersion(contextDTO, BusiConstant.WeekRollAdjust.VERSION_PREFIX);
        // 2、构建结构外调整明细
        List<MpAdjustDetailVo> adjustDetailList = buildAdjustDetailList(contextDTO);
        // 3、通过排产机台、结构筛选结构外调整明细
        List<MpAdjustDetailVo> matchAdjustList = filterAdjustDetailList(contextDTO,adjustDetailList);
        // 未获取到调整记录，抛出异常
        Assert.isFalse(PubUtil.isEmpty(matchAdjustList), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notFindAdjustDetailList"),
                    contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 4、按照结构、物料编码维度进行分组，并汇总订单量
        List<MpAdjustDetailVo> resultList = sumByStructureAndMaterial(matchAdjustList);
        contextDTO.setAdjustDetailList(resultList);
        // 5、设置净需求
        setCurrentNetQty(contextDTO);
        // 6、设置计划剩余排产量、计划已排产量、已生产量
        setMonthUnScheduledQty(contextDTO);
        // 7、筛选：|净需求 - 计划已排产量| > 0的数据
        filterAdjustList(contextDTO.getAdjustDetailList());
        // 筛选后数据为空，抛出异常
        Assert.isFalse(PubUtil.isEmpty(contextDTO.getAdjustDetailList()), () -> {
            String msg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.notMatchAdjustDetailList"), contextDTO.getYearMonth());
            return new BusinessException(msg);
        });
        // 8、设置其他字段
        setOtherField(contextDTO);
    }


    /**
     * 通过排产机台、结构筛选结构外调整明细
     * @param contextDTO
     * @param adjustDetailList
     * @return
     */
    private List<MpAdjustDetailVo> filterAdjustDetailList(MpRollAdjustContextDTO contextDTO,
                                                                     List<MpAdjustDetailVo> adjustDetailList) {
        if (PubUtil.isEmpty(adjustDetailList)) {
            return Collections.emptyList();
        }

        return adjustDetailList.stream()
                .filter(vo -> StringUtils.equals(vo.getStructureName(), contextDTO.getStructureName())
                        && (StringUtils.isEmpty(vo.getScheduledMachines())
                        || StringUtils.contains(vo.getScheduledMachines(), contextDTO.getScheduledMachines())))
                .collect(Collectors.toList());
    }

    /**
     * 筛选：|净需求 - 计划已排产量| > 0的数据
     * @param adjustList
     */
    private void filterAdjustList(List<MpAdjustDetailVo> adjustList) {
        if (PubUtil.isEmpty(adjustList)) {
            return;
        }
        adjustList.removeIf(adjust -> {
            Integer currentNetQty = Convert.toInt(adjust.getCurrentNetQty(),0);
            Integer monthScheduledQty = Convert.toInt(adjust.getMonthScheduledQty(),0);
            return Math.abs(currentNetQty - monthScheduledQty) == 0;
        });
    }

    @Override
    public void doAutoAdjust(MpRollAdjustContextDTO contextDTO) {
        //结构外调整记录
        contextDTO.setMpAdjustStructureOutList(mpAdjustStructureOutService.selectMpAdjustStructureOutList(contextDTO));
        //1.结构外订单调整记录空检查
        if (PubUtil.isEmpty(contextDTO.getMpAdjustStructureOutList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.orderAdjustRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }
        //2.月计划定稿数据空检查
        if (PubUtil.isEmpty(contextDTO.getFactoryMonthPlanProdFinalList())){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.monthPlanFinalRecordNotFound"),
                    contextDTO.getMpYear(),contextDTO.getMpMonth()));
        }

        //4.按结构序列化分组
        Map<String, List<FactoryMonthPlanFinalAdjustVo>> mpProdFinalMap = contextDTO.getFactoryMonthPlanProdFinalList().stream().collect(Collectors.groupingBy(item->item.getStructureName()));
        Date startTime,endTime;
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        //结构内，按结构分别调整
        contextDTO.setStructureName(contextDTO.getStructureName());
        List<MpStructureAllocation> structureAllocationList = contextDTO.getStructureAllocationList().stream().filter(x->x.getStructureName().equals(contextDTO.getStructureName())).collect(Collectors.toList());
        //更新结构转产表对应成型机台的调整开始日、结束日
        updateStructureAdjustDayByMachine(structureAllocationList,contextDTO);
        contextDTO.setOneStructureAllocationList(structureAllocationList);
        //初始锁定日
        contextDTO.setLockEndDay(getLockEndDay(contextDTO));
        //初始结构收尾日
        initStructureStartAndEndDay(contextDTO);
        //初始化日志
        contextDTO.setLogDetail(new StringBuilder());
        //规格挑选可用机台
        startTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,自动调整,开始时间:%s",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        weekRollAdjustEngine.structureOutAdjustForOne(contextDTO,contextDTO.getMpAdjustStructureOutList(), MapUtils.getObject(mpProdFinalMap, contextDTO.getStructureName(), new ArrayList<>()));
        endTime = new Date();
        contextDTO.getLogDetail().append(String.format("结构:%s,自动调整,结束时间:%s,总耗时:%s毫秒",contextDTO.getStructureName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,endTime),DateUtils.getDiffMillTime(startTime,endTime))).append(ApsConstant.DIVISION);
        //保存调整日志
        saveMpAdjustLog(contextDTO);
    }

    /**
     * 更新结构转产表对应成型机台的调整开始日、结束日
     * @param structureAllocationList
     * @param contextDTO
     */
    private void updateStructureAdjustDayByMachine(List<MpStructureAllocation> structureAllocationList,MpRollAdjustContextDTO contextDTO){
        if (PubUtil.isEmpty(structureAllocationList)){
            return;
        }
        for (MpStructureAllocation structureAllocation:structureAllocationList){
            if (structureAllocation.getCxMachineCode().equals(contextDTO.getScheduledMachines())){
                //更新结构转产表对应成型机台的调整开始日、结束日
                structureAllocation.setBeginDay(contextDTO.getAdjustStartDay());
                structureAllocation.setEndDay(contextDTO.getAdjustEndDay());
                return;
            }
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
        // 判断机台是否为空
        Assert.isFalse(StringUtils.isEmpty(contextDTO.getScheduledMachines()), I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.scheduledMachinesEmpty"));
        // 判断结构是否为空
        Assert.isFalse(StringUtils.isEmpty(contextDTO.getStructureName()), I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.structureNameEmpty"));
    }

    @Override
    public void saveAdjustDetailList(MpRollAdjustContextDTO contextDTO) {
        if (PubUtil.isEmpty(contextDTO.getAdjustDetailList())) {
            return;
        }
        List<MpAdjustStructureOut> adjustStructureOutList = baseDao.saveWithQuery(BeanUtil.copyToList(contextDTO.getAdjustDetailList(), MpAdjustStructureOut.class));
        List<MpAdjustDetailVo> resultList = BeanUtil.copyToList(adjustStructureOutList, MpAdjustDetailVo.class);
        contextDTO.setAdjustDetailList(resultList);
    }

    /**
     * 初始结构开始日\收尾日
     * @param contextDTO 周程滚动调整上下文对象
     */
    @Override
    protected void initStructureStartAndEndDay(MpRollAdjustContextDTO contextDTO){
        int endDay = 0;
        List<MpStructureAllocation> structureAllocationList = contextDTO.getOneStructureAllocationList();
        if (PubUtil.isNotEmpty(structureAllocationList)){
            // 取最大的成型机收尾日作为结构的收尾日
            for (MpStructureAllocation allocation:structureAllocationList){
                if (endDay < allocation.getEndDay()){
                    endDay = allocation.getEndDay();
                }
            }
        }

        contextDTO.setStructureDeadLine(endDay);
        //若结构收尾日小于锁定日，提示
        if (contextDTO.getStructureDeadLine() <= contextDTO.getLockEndDay()){
            throw new BusinessException(String.format(I18nUtil.getMessage("alg.data.mp.weekRollAdjust.adjustDayLtLockEndDay"),
                    contextDTO.getStructureDeadLine(),contextDTO.getLockEndDay()));
        }
    }
}
