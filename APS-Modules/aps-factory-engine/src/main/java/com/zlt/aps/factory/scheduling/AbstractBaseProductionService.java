package com.zlt.aps.factory.scheduling;

import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.MonthProductionDataService;
import com.zlt.aps.factory.service.ProductionMdmDataService;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.ProductionProcessStage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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
        StringBuilder logBuilder = context.getLogBuilder();
        String logContent = logBuilder.toString();
        if (StringUtils.isBlank(logContent)) {
            return;
        }
        logContent = String.format("%s流程日志:%s%s", processStage.getDesc(), System.lineSeparator(), logContent);
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
        StringBuilder logBuilder = new StringBuilder();
        context.setLogBuilder(logBuilder);
        productionContext.setLogBuilder(logBuilder);
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
        StringBuilder logBuilder = new StringBuilder();
        context.setLogBuilder(logBuilder);
        productionContext.setLogBuilder(logBuilder);
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
            context.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(productionMonth));
            context.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, monthDays));
            return;
        }
        //非自然月
        LocalDate previousMonth = context.getPreviousMonth();
        context.setProductionStartDate(com.zlt.aps.factory.utils.DateUtils.getDate(previousMonth.getYear(), previousMonth.getMonthValue(), cycleStartDay));
        context.setProductionEndDate(com.zlt.aps.factory.utils.DateUtils.getDate(year, month, cycleStartDay - 1));
    }

    public ProductionMdmDataService getDataService() {
        return dataService;
    }

    public MonthProductionDataService getMonthProductionDataService() {
        return monthProductionDataService;
    }

}
