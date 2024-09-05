package com.zlt.aps.gsq.mapper;

import com.zlt.aps.gsq.api.domain.entity.GsqStock;

import java.util.List;

/**
 * 钢丝圈库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface GsqStockMapper {
    /**
     * 查询钢丝圈库存信息
     *
     * @param id 钢丝圈库存信息ID
     * @return 钢丝圈库存信息
     */
    public GsqStock selectStockById(Long id);

    /**
     * 查询钢丝圈库存信息列表
     *
     * @param stock 钢丝圈库存信息
     * @return 钢丝圈库存信息集合
     */
    public List<GsqStock> selectStockList(GsqStock stock);

    /**
     * 新增钢丝圈库存信息
     *
     * @param stock 钢丝圈库存信息
     * @return 结果
     */
    public int insertStock(GsqStock stock);

    /**
     * 修改钢丝圈库存信息
     *
     * @param stock 钢丝圈库存信息
     * @return 结果
     */
    public int updateStock(GsqStock stock);

    /**
     * 批量删除钢丝圈库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<GsqStock> checkStockListUnic(GsqStock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqStock> list);
}
