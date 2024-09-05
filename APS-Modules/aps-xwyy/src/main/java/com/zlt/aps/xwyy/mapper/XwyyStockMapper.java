package com.zlt.aps.xwyy.mapper;

import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;

import java.util.List;

/**
 * 纤维压延库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface XwyyStockMapper {
    /**
     * 查询纤维压延库存信息
     *
     * @param id 纤维压延库存信息ID
     * @return 纤维压延库存信息
     */
    public XwyyStock selectStockById(Long id);

    /**
     * 查询纤维压延库存信息列表
     *
     * @param stock 纤维压延库存信息
     * @return 纤维压延库存信息集合
     */
    public List<XwyyStock> selectStockList(XwyyStock stock);

    /**
     * 新增纤维压延库存信息
     *
     * @param stock 纤维压延库存信息
     * @return 结果
     */
    public int insertStock(XwyyStock stock);

    /**
     * 修改纤维压延库存信息
     *
     * @param stock 纤维压延库存信息
     * @return 结果
     */
    public int updateStock(XwyyStock stock);

    /**
     * 批量删除纤维压延库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<XwyyStock> checkStockListUnic(XwyyStock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyStock> list);

}
