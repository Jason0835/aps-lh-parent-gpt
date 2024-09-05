package com.zlt.aps.tm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.entity.TmStock;

import java.util.List;


/**
 * 胎面库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface TmStockService {
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
     * 批量删除胎面库存信息
     *
     * @param ids 需要删除的胎面库存信息ID
     * @return 结果
     */
    public int deleteTmStockByIds(Long[] ids);

    /**
     * 删除胎面库存信息信息
     *
     * @param id 胎面库存信息ID
     * @return 结果
     */
    public int deleteTmStockById(Long id);

    /**
     * 校验胎面库存唯一性（根据库存日期+物料编号+id）
     */
    public List<TmStock> checkTmStockListUnic(TmStock TmStock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TmStock> list, boolean updateSupport, Long importLogId);
}
