package com.zlt.aps.xwyy.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;

import java.util.List;

/**
 * 纤维压延库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface XwyyStockService {
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
     * @param ids 需要删除的纤维压延库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验纤维压延库存唯一性（根据库存日期+物料编号+id）
     */
    public List<XwyyStock> checkStockListUnic(XwyyStock stock);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyyStock> list, boolean updateSupport, Long importLogId);
}
