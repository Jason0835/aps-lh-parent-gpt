package com.zlt.aps.gdyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;

import java.util.List;

/**
 * 钢带压延库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface GdyyStockService {
    /**
     * 查询钢带压延库存信息
     *
     * @param id 钢带压延库存信息ID
     * @return 钢带压延库存信息
     */
    public GdyyStock selectStockById(Long id);

    /**
     * 查询钢带压延库存信息列表
     *
     * @param stock 钢带压延库存信息
     * @return 钢带压延库存信息集合
     */
    public List<GdyyStock> selectStockList(GdyyStock stock);

    /**
     * 新增钢带压延库存信息
     *
     * @param stock 钢带压延库存信息
     * @return 结果
     */
    public int insertStock(GdyyStock stock);

    /**
     * 修改钢带压延库存信息
     *
     * @param stock 钢带压延库存信息
     * @return 结果
     */
    public int updateStock(GdyyStock stock);

    /**
     * 批量删除钢带压延库存信息
     *
     * @param ids 需要删除的钢带压延库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验钢带压延库存唯一性（根据库存日期+物料编号+id）
     */
    public List<GdyyStock> checkStockListUnic(GdyyStock stock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GdyyStock> list, boolean updateSupport, Long importLogId);

    /**
     * 判断是否按大卷计算库存
     */
    boolean isRollStock();
}
