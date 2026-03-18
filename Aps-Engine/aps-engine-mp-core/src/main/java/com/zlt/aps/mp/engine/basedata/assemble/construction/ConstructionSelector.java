package com.zlt.aps.mp.engine.basedata.assemble.construction;

import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductConstructionInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 施工关系选择器
 *
 * @author ZLT
 * @date 20260314
 */
@Slf4j
public class ConstructionSelector {

    /**
     * 获取施工关系信息
     *
     * @param constructionConfigurationList
     * @param productTypeCode
     * @return
     */
    public static MonthPlanProductConstructionInfoVo selectOneConstruction(List<MonthPlanProductConstructionInfoVo> constructionConfigurationList, String productTypeCode) {
        //入参校验
        if (CollectionUtils.isEmpty(constructionConfigurationList) || StringUtils.isBlank(productTypeCode)) {
            return null;
        }
        boolean isPCR = ProductTypeEnum.SEMI_STEEL.getValue().equalsIgnoreCase(productTypeCode);
        if (isPCR) {
            //优先一次法 按成型法排序 1-1次法 2-2次法
            constructionConfigurationList.sort(Comparator.comparing(MonthPlanProductConstructionInfoVo::getMouldMethod));
            return constructionConfigurationList.get(BigDecimal.ZERO.intValue());
        }
        constructionConfigurationList.forEach(singleConfiguration -> {
            String tireStatus = singleConfiguration.getProductStatus();
            ConstructionStageEnum constructionStage = ConstructionStageEnum.matchByMarkFlag(tireStatus);
            singleConfiguration.setSortValue(constructionStage.getSort());
        });
        //按排序，取得第一个
        constructionConfigurationList.sort(Comparator.comparing(MonthPlanProductConstructionInfoVo::getSortValue));
        return constructionConfigurationList.get(BigDecimal.ZERO.intValue());
    }
}
