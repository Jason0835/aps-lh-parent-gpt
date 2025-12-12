package com.zlt.aps.factory.scheduling;

import com.ruoyi.common.core.utils.DateUtils;
import com.tlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import lombok.extern.slf4j.Slf4j;
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
public abstract class AbstractProductionBusinessService implements IProductionBusinessService {
    /**
     * 数据提供接口
     */
    private final ProductionSchedulingDataService dataService;

    public AbstractProductionBusinessService(ProductionSchedulingDataService dataService) {
        this.dataService = dataService;
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

    public ProductionSchedulingDataService getDataService() {
        return dataService;
    }
}
