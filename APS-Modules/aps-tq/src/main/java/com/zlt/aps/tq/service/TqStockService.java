package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqStock;

import java.util.List;

/**
 * 胎圈库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface TqStockService {
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
     * @param ids 需要删除的胎圈库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验胎圈库存唯一性（根据库存日期+物料编号+id）
     */
    public List<TqStock> checkStockListUnic(TqStock stock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqStock> list, boolean updateSupport, Long importLogId);
}
