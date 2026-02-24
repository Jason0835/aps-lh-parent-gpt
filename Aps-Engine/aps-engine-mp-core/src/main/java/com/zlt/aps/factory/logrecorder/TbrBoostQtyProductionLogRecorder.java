package com.zlt.aps.factory.logrecorder;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.enums.TbrMouldProductionLogType;
import com.zlt.aps.factory.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * TBR 补量排产日志记录器
 *
 * @author ZLT
 * @date 20260128
 */
@Slf4j
public class TbrBoostQtyProductionLogRecorder {
    /**
     * 增加物料排产补量排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s 在[%s]日对[Sku：%s 排产分类：%s]进行补量排产，使用[%s]模具补量排产量[%s]====
     *
     * @param context           排程上下文
     * @param productionSkuInfo 排产Sku信息
     * @param productionDay     排产日
     * @param boostQty          补量
     * @param usedMouldCodeSet  使用模具
     * @return
     */
    public static String addBoostQtyProductionPlanLog(Context context, MonthPlanProductionRequirePlanVo productionSkuInfo, Integer productionDay, Integer boostQty, Set<String> usedMouldCodeSet) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s 在[%s]日对[Sku：%s 排产分类：%s]进行补量排产，使用[%s]模具补量排产量[%s]====";
        String usedMouldCodeInfo = String.join(StringConstant.COMMA, usedMouldCodeSet);
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                productionDay, productionSkuInfo.getMaterialDesc(), productionSkuInfo.getProductionType(),
                usedMouldCodeInfo, boostQty);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.BOOST_QTY_PRODUCTION, logContent);
        return logContent;
    }


    private TbrBoostQtyProductionLogRecorder() {

    }
}
