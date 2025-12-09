package com.zlt.aps.factory.scheduling.init;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.exception.BusinessException;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.scheduling.AbstractProductionBusinessService;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.factory.service.ProductionSchedulingDataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 通用的工厂生产计划初始化及检查业务
 * 主要针对半钢，全钢业务
 *
 * @author
 */
@Slf4j
@Service(value = "generalInitService")
public class GeneralInitService extends AbstractProductionBusinessService {

    public GeneralInitService(ProductionSchedulingDataService dataService) {
        super(dataService);
    }

    @Override
    public void run(Context context, Object userObj) {

    }

    /**
     * 初始化 物料信息、系统控制参数、物料折损率、施工阶段信息、模具信息
     * 物料与模具关系信息、利润优先值
     *
     * @param context
     * @return
     */
    private void initCache(ProductionContext context) {

    }

    /**
     * 设置生产版本号，如果已经有生产版本号，则不进行设置
     * 否则根据当前时间戳及版本号前缀设置
     * 已有生产版本号，则根据生产版本号删除旧有数据
     *
     * @param productionContext
     */
    private void deleteOldData(ProductionContext productionContext) {
        String productionVersion = productionContext.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            throw new BusinessException(I18nUtil.getMessage("alg.data.alter.message.productionVersionNoEmpty"));
        }
        //删除版本已有数据
        getDataService().deletedInitData(productionContext);
        getDataService().deletedMouldProductionData(productionContext);
    }

}
