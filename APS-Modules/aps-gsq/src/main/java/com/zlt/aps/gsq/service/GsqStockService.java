package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;

import java.util.List;

/**
 * 钢丝圈库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface GsqStockService {
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
     * @param ids 需要删除的钢丝圈库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验钢丝圈库存唯一性（根据库存日期+物料编号+id）
     */
    public List<GsqStock> checkStockListUnic(GsqStock stock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqStock> list, boolean updateSupport, Long importLogId);
}
