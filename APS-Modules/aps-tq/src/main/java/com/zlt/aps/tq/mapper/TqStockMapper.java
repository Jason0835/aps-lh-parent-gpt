package com.zlt.aps.tq.mapper;

import com.zlt.aps.tq.api.domain.entity.TqStock;

import java.util.List;

/**
 * 胎圈库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface TqStockMapper {
    /**
     * 查询胎圈库存信息
     *
     * @param id 胎圈库存信息ID
     * @return 胎圈库存信息
     */
    public TqStock selectStockById(Long id);

    /**
     * 查询胎圈库存信息列表
     *
     * @param stock 胎圈库存信息
     * @return 胎圈库存信息集合
     */
    public List<TqStock> selectStockList(TqStock stock);

    /**
     * 新增胎圈库存信息
     *
     * @param stock 胎圈库存信息
     * @return 结果
     */
    public int insertStock(TqStock stock);

    /**
     * 修改胎圈库存信息
     *
     * @param stock 胎圈库存信息
     * @return 结果
     */
    public int updateStock(TqStock stock);

    /**
     * 批量删除胎圈库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<TqStock> checkStockListUnic(TqStock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqStock> list);
}
