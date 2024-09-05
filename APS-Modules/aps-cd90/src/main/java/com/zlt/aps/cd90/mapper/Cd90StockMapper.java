package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;

import java.util.List;

/**
 * 90°裁断库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface Cd90StockMapper {
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
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<Cd90Stock> checkStockListUnic(Cd90Stock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90Stock> list);
}
