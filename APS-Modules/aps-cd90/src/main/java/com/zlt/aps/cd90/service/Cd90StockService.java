package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;

import java.util.List;

/**
 * 90°裁断库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface Cd90StockService {
    /**
     * 查询90°裁断库存信息
     *
     * @param id 90°裁断库存信息ID
     * @return 90°裁断库存信息
     */
    public Cd90Stock selectStockById(Long id);

    /**
     * 查询90°裁断库存信息列表
     *
     * @param stock 90°裁断库存信息
     * @return 90°裁断库存信息集合
     */
    public List<Cd90Stock> selectStockList(Cd90Stock stock);

    /**
     * 新增90°裁断库存信息
     *
     * @param stock 90°裁断库存信息
     * @return 结果
     */
    public int insertStock(Cd90Stock stock);

    /**
     * 修改90°裁断库存信息
     *
     * @param stock 90°裁断库存信息
     * @return 结果
     */
    public int updateStock(Cd90Stock stock);

    /**
     * 批量删除90°裁断库存信息
     *
     * @param ids 需要删除的90°裁断库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验90°裁断库存唯一性（根据库存日期+物料编号+id）
     */
    public List<Cd90Stock> checkStockListUnic(Cd90Stock stock);

    /**
     * 导入数据
     */
    AjaxResult importData(List<Cd90Stock> list, boolean updateSupport, Long importLogId);
}
