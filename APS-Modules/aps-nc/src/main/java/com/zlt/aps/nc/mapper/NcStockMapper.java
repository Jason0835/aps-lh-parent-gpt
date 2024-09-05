package com.zlt.aps.nc.mapper;

import com.zlt.aps.nc.api.domain.entity.NcStock;

import java.util.List;

/**
 * 内衬库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface NcStockMapper {
    /**
     * 查询内衬库存信息
     *
     * @param id 内衬库存信息ID
     * @return 内衬库存信息
     */
    public NcStock selectStockById(Long id);

    /**
     * 查询内衬库存信息列表
     *
     * @param stock 内衬库存信息
     * @return 内衬库存信息集合
     */
    public List<NcStock> selectStockList(NcStock stock);

    /**
     * 新增内衬库存信息
     *
     * @param stock 内衬库存信息
     * @return 结果
     */
    public int insertStock(NcStock stock);

    /**
     * 修改内衬库存信息
     *
     * @param stock 内衬库存信息
     * @return 结果
     */
    public int updateStock(NcStock stock);

    /**
     * 批量删除内衬库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<NcStock> checkStockListUnic(NcStock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcStock> list);
}
