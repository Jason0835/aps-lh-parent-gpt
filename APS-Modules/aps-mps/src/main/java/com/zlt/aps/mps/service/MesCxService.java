package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mps.domain.TMesCxMonthStock;
import com.zlt.aps.mps.domain.TMesCxStock;
import com.zlt.aps.mps.domain.TMesSapStock;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存回报
 * @author Gim
 */
public interface MesCxService {
    // 胎胚库存
    @Transactional
    public AjaxResult mergeCxStock(String dataVersion);

    // 胎胚月结库存
    @Transactional
    public AjaxResult mergeCxMonthStock(String dataVersion);

    // 成品库存
    @Transactional
    public AjaxResult mergeCxSapStock(String dataVersion);
}
