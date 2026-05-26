package com.zlt.aps.mp.engine.scheduling;

import com.google.common.collect.Lists;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.mp.api.domain.entity.MouldProductionLog;
import com.zlt.aps.mp.api.enums.ProductionProcessStage;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.ProductionStageLogRecorder;
import com.zlt.aps.mp.engine.service.MonthProductionDataService;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 抽象的排产业务类
 * 主要实现一些公用的业务处理
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
public abstract class AbstractBaseProductionService implements IProductionBusinessService {
    /**
     * 主数据数据提供接口
     */
    private final ProductionMdmDataService dataService;
    /**
     * 月度排产计划服务数据提供接口
     */
    private final MonthProductionDataService monthProductionDataService;

    public AbstractBaseProductionService(ProductionMdmDataService dataService,
                                         MonthProductionDataService monthProductionDataService) {
        this.dataService = dataService;
        this.monthProductionDataService = monthProductionDataService;
    }

    /**
     * 构建业务排产上下文
     *
     * @param context
     * @return
     */
    protected Context buildProductionContext(Context context) {
        //全钢业务
        if (ProductTypeEnum.WHOLE_STEEL == context.getProductType()) {
            return buildTbrProductionContext(context);
        }
        //主要为-半钢业务
        return buildDefaultProductionContext(context);
    }

    /**
     * 保存日志
     *
     * @param context
     */
    protected void saveProductionProcessLog(Context context, ProductionProcessStage processStage) {
        List<ProductionStageLogRecorder> logRecorderList = context.getLogBuilderList();
        if (CollectionUtils.isEmpty(logRecorderList)) {
            return;
        }
        //可分多阶段
        logRecorderList.forEach(stageLogRecorder -> {
            saveSingleStageLog(context, processStage, stageLogRecorder);
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
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        List<ProductionStageLogRecorder> logRecorderList = Lists.newArrayList();
        context.setLogBuilderList(logRecorderList);
        productionContext.setLogBuilderList(logRecorderList);
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
        context.setProductionVersion(productionContext.createNewProductionVersion());
        context.setOperationWorkNo(productionContext.createNewOperationWorkNo());
        List<ProductionStageLogRecorder> logRecorderList = Lists.newArrayList();
        context.setLogBuilderList(logRecorderList);
        productionContext.setLogBuilderList(logRecorderList);
        setProductionCycleInfo(productionContext);
        return productionContext;
    }

    /**
     * 设置排产周期信息等信息
     *
     * @param context
     */
    private void setProductionCycleInfo(Context context) {
        Integer cycleStartDay = dataService.getProductionCycleConfiguration(context);
        context.setStartDay(cycleStartDay);
        Integer year = context.getYear();
        Integer month = context.getMonth();
        //自然月
        if (context.isNaturalMonth()) {
            LocalDate productionMonth = context.getCurrentMonth();
            Integer monthDays = productionMonth.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
            context.setProductionStartDate(DateUtils.getDate(productionMonth));
            context.setProductionEndDate(DateUtils.getDate(year, month, monthDays));
            return;
        }
        //非自然月
        LocalDate previousMonth = context.getPreviousMonth();
        context.setProductionStartDate(DateUtils.getDate(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay));
        context.setProductionEndDate(DateUtils.getDate(year, month, cycleStartDay - 1));
    }

    /**
     * 单阶段日志保存
     *
     * @param context          排产上下文
     * @param processStage     排产入口模式
     * @param stageLogRecorder 排产阶段
     */
    private void saveSingleStageLog(Context context, ProductionProcessStage processStage, ProductionStageLogRecorder stageLogRecorder) {
        StringBuilder logBuilder = stageLogRecorder.getLogBuilder();
        String logContent = logBuilder.toString();
        if (StringUtils.isBlank(logContent)) {
            return;
        }
        logContent = String.format("%s-%s流程日志:%s%s", processStage.getDesc(), stageLogRecorder.getStage().getStageDesc(), System.lineSeparator(), logContent);
        MouldProductionLog log = new MouldProductionLog();
        log.setFactoryCode(context.getFactoryCode());
        log.setYear(context.getYear());
        log.setMonth(context.getMonth());
        log.setMonthPlanVersion(context.getMonthPlanVersion());
        log.setProductionVersion(context.getProductionVersion());
        log.setPlanType(context.getPlanType());
        log.setWorkNo(context.getOperationWorkNo());
        log.setLogContent(logContent);
        monthProductionDataService.saveMouldProductionLog(log);
    }

    public ProductionMdmDataService getDataService() {
        return dataService;
    }

    public MonthProductionDataService getMonthProductionDataService() {
        return monthProductionDataService;
    }

}
