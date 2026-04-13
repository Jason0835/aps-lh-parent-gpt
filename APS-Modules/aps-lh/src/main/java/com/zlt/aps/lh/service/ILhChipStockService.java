package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
import com.zlt.bill.common.service.IDocService;

/**
 * 芯片库存 Service接口
 *
 * @author APS Team
 * @date 2026-04-02
 */
public interface ILhChipStockService extends IDocService<LhChipStock> {

    String[] getQueryFormulas();

    /**
     * 更新完成量 - 供硫化排程回填
     * @param factoryCode 分厂
     * @param chipCode 芯片编号
     * @param finishQty 完成量
     * @return 结果
     */
    int updateFinishQty(String factoryCode, String chipCode, Integer finishQty);

    /**
     * 合并保存 - 新增时检测到重复，将库存量和完成量累加到已有数据上
     * @param lhChipStock 要保存的数据
     * @return 结果
     */
    AjaxResult mergeSave(LhChipStock lhChipStock);
}
