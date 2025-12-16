package com.zlt.aps.factory.scheduling.cxcapacity;

import com.ruoyi.common.core.utils.DateUtils;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ContinueProductInfo;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.ProductionCycleUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工厂TBR业务轮胎成型产能分配
 * 主要完成按结构进行成型产能分配
 * 1、按结构汇总净需求量，粗算结构所需成型机台数
 * 2、从上个月的月度排产计划，获取在产结构-即续作结构
 * 3、如果续作结构需求量减少，导致需要提前释放续作结构的成型机台数，则优先释放配比机台数多的
 * 4、续作结构排产完毕后，对续作结构中收尾的成型机台，进行反向查找下个结构(需成型机台剩余产能能满足结构净需求)
 * 5、对剩余还有需求量的结构，按结构优先级，挑选优先级最高的结构，匹配能分配的成型机台
 * 5.1、固定机台优先，是否零度结构 = 零度供料架
 * 5.2、成型机当前排产结构是否与挑选结构含有同规格(结构向下SKU只要有一个同规格则认为同规格)
 * 5.3、成型机当前排产结构是否与挑选结构含有同英寸(结构向下SKU只要有一个同英寸则认为同英寸)
 * 5.4、成型机当前排产结构是否与挑选结构的断面宽±10优先
 * 5.5、近1个月历史结构在期日期近的优先(最后一个排产日)
 * 5.6、近3个月历史结构在机次数多的优先(一个月算一次)
 *
 * @author
 */
@Slf4j
@Service(value = "tbrCxCapacityAllocationService")
public class TbrCxCapacityAllocationService extends AbstractProductionBusinessService {


    public TbrCxCapacityAllocationService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {
        //创建排产上下文
        TbrProductionContext productionContext = (TbrProductionContext) buildProductionContext(context);
        //todo 记录日志-开始进行成型产能分配-结构排产

        //获取排产初始化信息
        List<MonthPlanProductionRequirePlanVo> requirePlanList = getDataService().getFactoryMonthPlanManufacturing(productionContext);
        //获取周期内的生产日历信息
        setMonthProductionDays(context);
        //获取结构的最小日硫化量
        Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap = getMinLhRatioConfiguration(productionContext, requirePlanList);
        //todo 记录日志-粗算成型机台数
        //按结构分组，汇总结构净需求量，粗算需要的机台数
        Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap = ProductionPlanGroupInfo.statisticsAndEstimateCxAllocationByGroup(context, requirePlanList, minLhRatioMap);
        productionContext.setGroupProductionInfo(estimateGroupCxAllocationMap);
        //获取上个月度的月度定稿排产计划，得到在产结构及对应的成型机及在产SKU
        Map<String, CxContinueInfoHelper> cxContinueInfoMap = getContinueInfo(context);
        //获取成型机台信息--日产信息
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = getDataService().getCxMachineBaseInfo(productionContext);

        productionContext.setCxMachineBaseInfo(cxMachineBaseInfo);
        //todo 记录日志 续作结构排产分配
        //先对续作结构进行成型机台分配
        CxCapacityAllocationHandler.continueGroupPlanAllocation(productionContext, estimateGroupCxAllocationMap, cxContinueInfoMap);
        //对成型机台进行模拟模具排产

        //对收尾成型机台，反向匹配待排结构

        //对还需排产结构，获取优先级最高的结构

        //对挑选出的机构，匹配还有排产量的成型机台

        //得到结构成型排程结果
    }

    /**
     * 构建业务排产上下文
     *
     * @param context
     * @return
     */
    @Override
    protected Context buildProductionContext(Context context) {
        //全钢业务
        if (ProductTypeEnum.WHOLE_STEEL == context.getProductType()) {
            return buildTbrProductionContext(context);
        }
        //主要为-半钢业务
        return buildDefaultProductionContext(context);
    }

    /**
     * 设置工厂的排产日信息
     * 包含 停产日及开停产的产能比例
     *
     * @param context
     */
    private void setMonthProductionDays(Context context) {
        List<ProductionDayInfoVo> productionDayInfoList = getDataService().getProductCalendar(context);
        if (CollectionUtils.isEmpty(productionDayInfoList)) {
            context.setStopDays(Collections.emptySet());
            return;
        }
        //排产开始日
        Date productionStartDate = context.getProductionStartDate();
        //开产比例设置
        Map<Integer, Integer> startProductionRatioMap = new HashMap<>(context.getMonthDays());
        List<ProductionDayInfoVo> startProductionDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.YES.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(startProductionDays)) {
            startProductionDays.forEach(startProductionInfo -> {
                Date startProduction = startProductionInfo.getProductionDate();
                Integer startDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, startProduction);
                startProductionRatioMap.put(startDay, startProductionInfo.getRate());
            });
        }
        context.setCapacityRatioMap(startProductionRatioMap);
        //停产设置
        List<ProductionDayInfoVo> stopDays = productionDayInfoList.stream().filter(productionDayInfo -> YesOrNoEnum.NO.getCode().equals(productionDayInfo.getDayFlag())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(stopDays)) {
            context.setStopDays(Collections.emptySet());
            return;
        }
        Set<Integer> stopDaySet = new HashSet<>(context.getMonthDays());
        stopDays.forEach(stopProductionInfo -> {
            Date stopProduction = stopProductionInfo.getProductionDate();
            Integer stopDay = com.zlt.aps.factory.utils.DateUtils.getIntervalDays(productionStartDate, stopProduction);
            stopDaySet.add(stopDay);
        });
        context.setStopDays(stopDaySet);
    }

    /**
     * 获取计划对应结构的最小日硫化量信息
     *
     * @param context         排产上下文
     * @param requirePlanList 需求计划信息
     * @return
     */
    private Map<String, MonthPlanStructureLhRatioVo> getMinLhRatioConfiguration(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        Set<String> structureNameMap = requirePlanList.stream().map(MonthPlanProductionRequirePlanVo::getStructureName).collect(Collectors.toSet());
        List<String> structureNameList = new ArrayList<>(structureNameMap);
        return getDataService().getMinLhRatioInfo(context, structureNameList);
    }

    /**
     * 获取续作排产信息
     * 续作的分组信息(结构)，对应的成型产能和续作的SKU，使用模具-硫化机台数
     *
     * @param context 排产上下文
     * @return
     */
    private Map<String, CxContinueInfoHelper> getContinueInfo(Context context) {
        //获取前一个月的排产版本信息
        String factoryCode = context.getFactoryCode();
        LocalDate previousMonth = context.getPreviousMonth();
        Integer year = previousMonth.getYear();
        Integer month = previousMonth.getMonthValue();
        FactoryProductionVersion previousVersion = getDataService().getFinalVersion(factoryCode, year, month);
        if (null == previousVersion) {
            return Collections.emptyMap();
        }
        Context previousContext = new Context();
        previousContext.setFactoryCode(factoryCode);
        previousContext.setYear(year);
        previousContext.setMonth(month);
        List<ProductionDayInfoVo> previousProductionDayInfo = getDataService().getProductCalendar(previousContext);
        Integer lastDay = ProductionCycleUtils.getLastProductionDay(previousVersion, previousProductionDayInfo);
        if (lastDay <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyMap();
        }
        List<ContinueProductInfo> continueProductionInfoList = getDataService().getContinueProductionInfo(factoryCode, year, month, lastDay);
        return CxContinueInfoHelper.createGroupInfo(continueProductionInfoList);
    }

    /**
     * 设置成型机台基础信息
     * 包含 成型可排产日信息
     * 固定结构、固定SKU
     * 不可排产结构、不可排产SKU
     *
     * @param productionContext
     */
    private void setCxMachineInitInfo(TbrProductionContext productionContext) {
        Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo = getDataService().getCxMachineBaseInfo(productionContext);
        if (CollectionUtils.isEmpty(cxMachineBaseInfo)) {
            productionContext.setCxMachineBaseInfo(cxMachineBaseInfo);
            return;
        }
        cxMachineBaseInfo.forEach((cxMachineCode, singleBaseInfo) -> {
            Set<Integer> stopDayInfo = singleBaseInfo.getStopDayInfo();
            if (null == stopDayInfo) {
                stopDayInfo = new HashSet<>();
            }
            Integer monthDays = productionContext.getMonthDays();
            Integer maxProductionDays = monthDays - stopDayInfo.size();
            singleBaseInfo.setMaxProductionDays(maxProductionDays);
            singleBaseInfo.setRemainingDays(maxProductionDays);
        });
    }

    /**
     * 构建全钢排产上下文
     *
     * @param context
     * @return
     */
    private TbrProductionContext buildTbrProductionContext(Context context) {
        TbrProductionContext productionContext = new TbrProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        productionContext.createNewProductionVersion();
        productionContext.setOperationWorkNo(DateUtils.dateTimeNow());
        productionContext.setLogBuilder(new StringBuilder());
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 构建默认的排产上下文
     * 主要为半钢业务
     *
     * @param context
     * @return
     */
    private ProductionContext buildDefaultProductionContext(Context context) {
        ProductionContext productionContext = new ProductionContext();
        BeanUtils.copyProperties(context, productionContext);
        productionContext.createNewProductionVersion();
        productionContext.setOperationWorkNo(DateUtils.dateTimeNow());
        productionContext.setLogBuilder(new StringBuilder());
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 设置排产周期信息等信息
     *
     * @param context
     */
    private void setProductionCycleInfo(Context context) {
        FactoryProductionVersion productionVersion = getDataService().getFactoryMonthPlanVersion(context);
        if (null != productionVersion) {
            Date productionStartDate = productionVersion.getProductionStartDate();
            context.setProductionStartDate(productionStartDate);
            context.setStartDay(com.zlt.aps.factory.utils.DateUtils.getDaysByMonth(productionStartDate));
            context.setProductionEndDate(productionVersion.getProductionEndDate());
        }
    }
}
