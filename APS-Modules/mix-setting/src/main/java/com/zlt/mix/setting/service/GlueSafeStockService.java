package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.GlueSafeStock;

import java.math.BigDecimal;
import java.util.List;

/**
 * 安全库存Service接口
 * 
 * @author Gim
 * @date 2022-03-21
 */
public interface GlueSafeStockService  extends IService<GlueSafeStock>
{
    /**
     * 查询安全库存列表
     * 
     * @param glueSafeStock 安全库存
     * @return 安全库存集合
     */
    List<GlueSafeStock> selectGlueSafeStockList(GlueSafeStock glueSafeStock);

    /**
     * 保存安全库存信息（id为空则新增，id不为空则修改）
     *
     * @param glueSafeStock
     */
    void saveGlueSafeStock(GlueSafeStock glueSafeStock);

    /**
     * 批量删除安全库存
     * 
     * @param ids 需要删除的安全库存ID
     * @return 结果
     */
    int deleteGlueSafeStockByIds(Long[] ids);

    /**
     * 校验安全库存唯一性
     */
    String checkGlueSafeStockUnique(GlueSafeStock glueSafeStock);

    /**
     * 导入安全库存数据
     */
    AjaxResult importData(List<GlueSafeStock> list, boolean updateSupport, Long importLogId);

    /**
     * 查询安全库存
     *
     * @param mixArea 密炼区
     * @param glue    胶料名称
     * @return 安全库存
     */
    BigDecimal selectGlueSafeStock(String mixArea, String glue);

    /**
     * 有则更新，无则插入
     *
     * @param mixArea   密炼区
     * @param glue      胶料名称
     * @param safeStock 安全库存
     */
    void saveOrUpdateGlueSafeStock(String mixArea, String glue, BigDecimal safeStock);

    /**
     * 根据密炼区和胶料名称更改安全库存
     */
    void updateSafeStockByMixAreaAndGlue(GlueSafeStock glueSafeStock);
}
