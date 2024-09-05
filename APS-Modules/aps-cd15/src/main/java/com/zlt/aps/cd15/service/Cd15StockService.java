package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;

import java.util.List;

/**
 * 15°裁断库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface Cd15StockService {
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
     * @param ids 需要删除的15°裁断库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验15°裁断库存唯一性（根据库存日期+物料编号+id）
     */
    public List<Cd15Stock> checkStockListUnic(Cd15Stock stock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<Cd15Stock> list, boolean updateSupport, Long importLogId);
}
