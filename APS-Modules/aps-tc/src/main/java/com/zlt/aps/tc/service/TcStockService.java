package com.zlt.aps.tc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcStock;

import java.util.List;

/**
 * 胎侧库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface TcStockService {
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
     * 批量删除胎侧库存信息
     *
     * @param ids 需要删除的胎侧库存信息ID
     * @return 结果
     */
    public int deleteTcStockByIds(Long[] ids);

    /**
     * 删除胎侧库存信息信息
     *
     * @param id 胎侧库存信息ID
     * @return 结果
     */
    public int deleteTcStockById(Long id);

    /**
     * 校验胎侧库存唯一性（根据库存日期+物料编号+id）
     */
    public List<TcStock> checkTcStockListUnic(TcStock TmStock);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcStock> list, boolean updateSupport, Long importLogId);
}
