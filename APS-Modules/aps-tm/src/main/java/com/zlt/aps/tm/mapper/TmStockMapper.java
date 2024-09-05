package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmStock;

import java.util.List;


/**
 * 胎面库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TmStockMapper {
    /**
     * 查询胎面库存信息
     *
     * @param id 胎面库存信息ID
     * @return 胎面库存信息
     */
    public TmStock selectTmStockById(Long id);

    /**
     * 查询胎面库存信息列表
     *
     * @param TmStock 胎面库存信息
     * @return 胎面库存信息集合
     */
    public List<TmStock> selectTmStockList(TmStock TmStock);

    /**
     * 新增胎面库存信息
     *
     * @param TmStock 胎面库存信息
     * @return 结果
     */
    public int insertTmStock(TmStock TmStock);

    /**
     * 修改胎面库存信息
     *
     * @param TmStock 胎面库存信息
     * @return 结果
     */
    public int updateTmStock(TmStock TmStock);

    /**
     * 删除胎面库存信息
     *
     * @param id 胎面库存信息ID
     * @return 结果
     */
    public int deleteTmStockById(Long id);

    /**
     * 批量删除胎面库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTmStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<TmStock> checkTmStockListUnic(TmStock TmStock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmStock> list);

}
