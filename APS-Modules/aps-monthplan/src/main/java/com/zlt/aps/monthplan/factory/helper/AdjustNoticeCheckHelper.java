package com.zlt.aps.monthplan.factory.helper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import lombok.Data;

import java.io.Serializable;

/**
 * 调整通知单检查辅助类
 *
 * @author ZLT
 * @date 20250528
 */
@Data
public class AdjustNoticeCheckHelper implements Serializable {
    /**
     * 调整通知单信息
     */
    private MonthPlanNoticeOrder noticeOrder;
    /**
     * 定稿版本信息
     */
    private FactoryProductionVersion productionVersion;

    public AdjustNoticeCheckHelper(MonthPlanNoticeOrder noticeOrder, FactoryProductionVersion productionVersion) {
        this.noticeOrder = noticeOrder;
        this.productionVersion = productionVersion;
    }
}
