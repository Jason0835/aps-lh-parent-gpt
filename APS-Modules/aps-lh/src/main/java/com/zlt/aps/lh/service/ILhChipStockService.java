package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

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

    /**
     * 逻辑删除分厂指定数据来源的芯片库存并批量插入新数据（事务性操作）
     *
     * @param factoryCode 分厂编号
     * @param dataSource  数据来源
     * @param updateBy    更新者
     * @param insertList  待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, String dataSource, String updateBy, List<LhChipStock> insertList);

    /**
     * 增量更新芯片库存完成量
     * 根据分厂编号+芯片编码匹配：已存在则累加完成量，不存在则新增记录
     *
     * @param factoryCode 分厂编号
     * @param list        待更新的芯片库存列表（需设置chipCode和finishQty）
     */
    void upsertFinishQty(String factoryCode, List<LhChipStock> list);
}
