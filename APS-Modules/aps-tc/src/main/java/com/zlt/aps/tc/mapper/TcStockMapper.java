package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcStock;

import java.util.List;

/**
 * 胎侧库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface TcStockMapper {
    /**
     * 查询胎侧库存信息
     *
     * @param id 胎侧库存信息ID
     * @return 胎侧库存信息
     */
    public TcStock selectTcStockById(Long id);

    /**
     * 查询胎侧库存信息列表
     *
     * @param tcStock 胎侧库存信息
     * @return 胎侧库存信息集合
     */
    public List<TcStock> selectTcStockList(TcStock tcStock);

    /**
     * 新增胎侧库存信息
     *
     * @param tcStock 胎侧库存信息
     * @return 结果
     */
    public int insertTcStock(TcStock tcStock);

    /**
     * 修改胎侧库存信息
     *
     * @param tcStock 胎侧库存信息
     * @return 结果
     */
    public int updateTcStock(TcStock tcStock);

    /**
     * 删除胎侧库存信息
     *
     * @param id 胎侧库存信息ID
     * @return 结果
     */
    public int deleteTcStockById(Long id);

    /**
     * 批量删除胎侧库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTcStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<TcStock> checkTcStockListUnic(TcStock tcStock);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcStock> list);
}
