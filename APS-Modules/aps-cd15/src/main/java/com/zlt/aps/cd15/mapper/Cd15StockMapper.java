package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;

import java.util.List;

/**
 * 15°裁断库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface Cd15StockMapper {
    /**
     * 查询15°裁断库存信息
     *
     * @param id 15°裁断库存信息ID
     * @return 15°裁断库存信息
     */
    public Cd15Stock selectStockById(Long id);

    /**
     * 查询15°裁断库存信息列表
     *
     * @param stock 15°裁断库存信息
     * @return 15°裁断库存信息集合
     */
    public List<Cd15Stock> selectStockList(Cd15Stock stock);

    /**
     * 新增15°裁断库存信息
     *
     * @param stock 15°裁断库存信息
     * @return 结果
     */
    public int insertStock(Cd15Stock stock);

    /**
     * 修改15°裁断库存信息
     *
     * @param stock 15°裁断库存信息
     * @return 结果
     */
    public int updateStock(Cd15Stock stock);

    /**
     * 批量删除15°裁断库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<Cd15Stock> checkStockListUnic(Cd15Stock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15Stock> list);
}
