package com.zlt.aps.factory.scheduling.moulding.group;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.IProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.scheduling.moulding.GroupPlanProductionContext;
import com.zlt.aps.factory.scheduling.moulding.SinglePlanProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import com.zlt.aps.factory.utils.MouldUtils;
import com.zlt.aps.factory.utils.ProductionLogUtils;
import com.zlt.aps.factory.utils.ProductionPlanUtils;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 有交期分组的同模具排产
 *
 * @author
 */
@Slf4j
@Service(value = "sameMouldDeliverDate")
public class SameMouldDeliverDateProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService singlePlanProductionService;

    public SameMouldDeliverDateProductionService(ProductionSchedulingDataService dataService,
                                                 @Qualifier("singlePlanProductionService") IProductionBusinessService singlePlanProductionService) {
        super(dataService);
        this.singlePlanProductionService = singlePlanProductionService;
    }

    @Override
    public void run(Context context, Object userObj) {
        SinglePlanProductionContext singleContext = (SinglePlanProductionContext) context;
        GroupPlanProductionContext groupPlanProductionContext = singleContext.getGroupContext();
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        String factoryCode = productionContext.getFactoryCode();
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        String monthPlanVersion = productionContext.getMonthPlanVersion();
        String startSameMouldDeliverDateLogContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划同模具有交期排程开始====", factoryCode, year, month, monthPlanVersion);
        log.info(startSameMouldDeliverDateLogContent);
        //排产流程日志记录
        MouldProductionLog startSameMouldDeliverDateLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, startSameMouldDeliverDateLogContent);
        saveProductionLog(productionContext, startSameMouldDeliverDateLog);
        //获取对应的可用模具
        List<MouldInfoVO> enableMouldList = singleContext.getEnableMouldList();
        if (CollectionUtils.isEmpty(enableMouldList)) {
            String noEnableMouldLogContent = "====同模具有交期排产：没有可用模具";
            log.info(noEnableMouldLogContent);
            //排产流程日志记录
            MouldProductionLog noEnableMouldLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, noEnableMouldLogContent);
            saveProductionLog(productionContext, noEnableMouldLog);
            return;
        }
        //得到模具关联的物料信息
        Set<String> relationProductCodeSet = MouldUtils.getMouldRelationProductInfo(enableMouldList, productionContext);
        if (CollectionUtils.isEmpty(relationProductCodeSet)) {
            String noCommonProductLogContent = "====同模具有交期排产：模具没有共用的物料";
            log.info(noCommonProductLogContent);
            //排产流程日志记录
            MouldProductionLog noCommonProductLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, noCommonProductLogContent);
            saveProductionLog(productionContext, noCommonProductLog);
            return;
        }
        //获取上一个排产计划
        MonthPlanManufacturingRequirementVo previous = singleContext.getProductionPlan();
        List<MonthPlanManufacturingRequirementVo> relationProductionPlanList = ProductionPlanUtils.getRelationPlanByGroup(groupPlanProductionContext, previous.getMonthPlanId(), relationProductCodeSet, false);
        if (CollectionUtils.isEmpty(relationProductionPlanList)) {
            String noProductionPlanLogContent = "====同模具交期排产：没有共用模具的物料排产计划";
            log.info(noProductionPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noProductionPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, noProductionPlanLogContent);
            saveProductionLog(productionContext, noProductionPlanLog);
            return;
        }
        //取得交期
        Date deliveryDateDue = previous.getDeliveryDateDue();
        //同交期
        List<MonthPlanManufacturingRequirementVo> sameDeliveryDateList = new ArrayList<>();
        //不同交期
        List<MonthPlanManufacturingRequirementVo> noSameDeliveryDateList = new ArrayList<>();
        relationProductionPlanList.stream().forEach(productionPlan -> {
            if (deliveryDateDue.equals(productionPlan.getDeliveryDateDue())) {
                sameDeliveryDateList.add(productionPlan);
            } else {
                noSameDeliveryDateList.add(productionPlan);
            }
        });
        //排产流程日志记录
        String sameDeliveryDateLogContent = String.format("[%s]同交期优先排产", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, deliveryDateDue));
        MouldProductionLog sameDeliveryDateLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, sameDeliveryDateLogContent);
        saveProductionLog(productionContext, sameDeliveryDateLog);
        //同交期优先
        sameDeliveryDateProduction(sameDeliveryDateList, groupPlanProductionContext, userObj);
        //排产流程日志记录
        String noSameDeliveryDateLogContent = String.format("[%s]非同交期排产", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, deliveryDateDue));
        MouldProductionLog noSameDeliveryDateLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, noSameDeliveryDateLogContent);
        saveProductionLog(productionContext, noSameDeliveryDateLog);
        //非同交期后排
        noSameDeliveryDateProduction(noSameDeliveryDateList, groupPlanProductionContext, userObj);
    }

    /**
     * 同模具同交期排产
     *
     * @param sameDeliveryDateList       同模具同交期计划列表
     * @param groupPlanProductionContext 分组排产上下文
     * @param userObj                    其它参数
     */
    private void sameDeliveryDateProduction(List<MonthPlanManufacturingRequirementVo> sameDeliveryDateList, GroupPlanProductionContext groupPlanProductionContext, Object userObj) {
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        if (CollectionUtils.isEmpty(sameDeliveryDateList)) {
            String noProductionPlanLogContent = "====同模具交期排产：没有同交期的物料排产计划";
            log.info(noProductionPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noProductionPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, noProductionPlanLogContent);
            saveProductionLog(productionContext, noProductionPlanLog);
            return;
        }
        //按排产顺序排序
        sameDeliveryDateList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        sameDeliveryDateList.stream().forEach(sameDeliveryDatePlan -> {
            String productionLogContent = String.format("====同模具同交期排产：排产计划ID：%s，计划量：%d, 单条硫化秒：%d", sameDeliveryDatePlan.getMonthPlanId(), sameDeliveryDatePlan.getProductionQty(), sameDeliveryDatePlan.getCuringTime().longValue());
            log.info(productionLogContent);
            //排产流程日志记录
            MouldProductionLog productionLog = ProductionLogUtils.buildProductionLog(productionContext, sameDeliveryDatePlan, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, productionLogContent);
            saveProductionLog(productionContext, productionLog);
            SinglePlanProductionContext sameDeliverySingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(sameDeliveryDatePlan, groupPlanProductionContext.getProductionContext());
            sameDeliverySingleContext.setGroupContext(groupPlanProductionContext);
            singlePlanProductionService.run(sameDeliverySingleContext, userObj);
        });
    }

    /**
     * 同模具非同交期排产
     *
     * @param noSameDeliveryDateList     同模具非同交期排产计划
     * @param groupPlanProductionContext 分组排产上下文
     * @param userObj                    其它参数
     */
    private void noSameDeliveryDateProduction(List<MonthPlanManufacturingRequirementVo> noSameDeliveryDateList, GroupPlanProductionContext groupPlanProductionContext, Object userObj) {
        ProductionContext productionContext = groupPlanProductionContext.getProductionContext();
        //非同交期
        if (CollectionUtils.isEmpty(noSameDeliveryDateList)) {
            String noProductionPlanLogContent = "====同模具交期排产：没有非同交期的物料排产计划";
            log.info(noProductionPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noProductionPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, noProductionPlanLogContent);
            saveProductionLog(productionContext, noProductionPlanLog);
            return;
        }
        //按 先交期，后排产顺序排序
        noSameDeliveryDateList.sort(Comparator.comparing(MonthPlanManufacturingRequirementVo::getDeliveryDateDue)
                .thenComparing(MonthPlanManufacturingRequirementVo::getProductionSequence));
        noSameDeliveryDateList.stream().forEach(noSameDeliveryDatePlan -> {
            String productionLogContent = String.format("====同模具非同交期排产：排产计划ID：%s，计划量：%d, 单条硫化秒：%d", noSameDeliveryDatePlan.getMonthPlanId(), noSameDeliveryDatePlan.getProductionQty(), noSameDeliveryDatePlan.getCuringTime().longValue());
            log.info(productionLogContent);
            //排产流程日志记录
            MouldProductionLog productionLog = ProductionLogUtils.buildProductionLog(productionContext, noSameDeliveryDatePlan, MouldProductionLogType.SAME_MOULD_DELIVERY_LOG, productionLogContent);
            saveProductionLog(productionContext, productionLog);
            SinglePlanProductionContext noSameDeliverySingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(noSameDeliveryDatePlan, groupPlanProductionContext.getProductionContext());
            noSameDeliverySingleContext.setGroupContext(groupPlanProductionContext);
            singlePlanProductionService.run(noSameDeliverySingleContext, userObj);
        });
    }
}
