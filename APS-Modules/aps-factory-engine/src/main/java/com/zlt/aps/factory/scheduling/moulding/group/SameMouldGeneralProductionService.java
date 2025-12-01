package com.zlt.aps.factory.scheduling.moulding.group;

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

import java.util.List;
import java.util.Set;

/**
 * 无交期分组的同模具排产
 *
 * @author
 */
@Slf4j
@Service(value = "sameMouldGeneral")
public class SameMouldGeneralProductionService extends AbstractProductionBusinessService {

    private final IProductionBusinessService singlePlanProductionService;

    public SameMouldGeneralProductionService(ProductionSchedulingDataService dataService,
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
        String startSameMouldGeneralLogContent = String.format("=====分厂%s, 计划年月：%d-%d, 计划版本：%s，生产计划同模具无交期排程开始====", factoryCode, year, month, monthPlanVersion);
        log.info(startSameMouldGeneralLogContent);
        //排产流程日志记录
        MouldProductionLog startSameMouldGeneralLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_GENERAL_LOG, startSameMouldGeneralLogContent);
        saveProductionLog(productionContext, startSameMouldGeneralLog);
        //获取对应的可用模具
        List<MouldInfoVO> enableMouldList = singleContext.getEnableMouldList();
        if (CollectionUtils.isEmpty(enableMouldList)) {
            String noEnableMouldLogContent = "====同模具无交期排产：没有可用模具";
            log.info(noEnableMouldLogContent);
            //排产流程日志记录
            MouldProductionLog noEnableMouldLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_GENERAL_LOG, noEnableMouldLogContent);
            saveProductionLog(productionContext, noEnableMouldLog);
            return;
        }
        //得到模具关联的物料信息
        Set<String> relationProductCodeSet = MouldUtils.getMouldRelationProductInfo(enableMouldList, productionContext);
        if (CollectionUtils.isEmpty(relationProductCodeSet)) {
            String noCommonProductLogContent = "====同模具无交期排产：模具没有共用的物料";
            log.info(noCommonProductLogContent);
            //排产流程日志记录
            MouldProductionLog noCommonProductLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_GENERAL_LOG, noCommonProductLogContent);
            saveProductionLog(productionContext, noCommonProductLog);
            return;
        }
        //获取上一个排产计划
        MonthPlanManufacturingRequirementVo previous = singleContext.getProductionPlan();
        List<MonthPlanManufacturingRequirementVo> sameMouldProductionList = ProductionPlanUtils.getRelationPlanByGroup(groupPlanProductionContext, previous.getMonthPlanId(), relationProductCodeSet, true);
        if (CollectionUtils.isEmpty(sameMouldProductionList)) {
            String noProductionPlanLogContent = "====同模具无交期排产：没有共用模具的物料排产计划";
            log.info(noProductionPlanLogContent);
            //排产流程日志记录
            MouldProductionLog noProductionPlanLog = ProductionLogUtils.buildProductionLog(productionContext, null, MouldProductionLogType.SAME_MOULD_GENERAL_LOG, noProductionPlanLogContent);
            saveProductionLog(productionContext, noProductionPlanLog);
            return;
        }
        sameMouldProductionList.stream().forEach(sameMouldPlan -> {
            String productionLogContent = String.format("====同模具无交期排产：排产计划ID：%s，计划量：%d, 单条硫化秒：%d", sameMouldPlan.getMonthPlanId(), sameMouldPlan.getProductionQty(), sameMouldPlan.getCuringTime().longValue());
            log.info(productionLogContent);
            //排产流程日志记录
            MouldProductionLog productionLog = ProductionLogUtils.buildProductionLog(productionContext, sameMouldPlan, MouldProductionLogType.SAME_MOULD_GENERAL_LOG, productionLogContent);
            saveProductionLog(productionContext, productionLog);
            SinglePlanProductionContext sameDeliverySingleContext = ProductionPlanUtils.buildSinglePlanProductionContext(sameMouldPlan, groupPlanProductionContext.getProductionContext());
            sameDeliverySingleContext.setGroupContext(groupPlanProductionContext);
            singlePlanProductionService.run(sameDeliverySingleContext, userObj);
            String endProductionLogContent = String.format("====同模具无交期排产结束：排产计划ID：%s，计划量：%d", sameMouldPlan.getMonthPlanId(), sameMouldPlan.getProductionQty());
            log.info(endProductionLogContent);
            //排产流程日志记录
            MouldProductionLog sameProductionEndLog = ProductionLogUtils.buildProductionLog(productionContext, sameMouldPlan, MouldProductionLogType.SAME_MOULD_GENERAL_LOG, endProductionLogContent);
            saveProductionLog(productionContext, sameProductionEndLog);
        });
    }
}
