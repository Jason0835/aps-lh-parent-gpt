package com.zlt.aps.nc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcStock;

import java.util.List;

/**
 * 内衬库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface NcStockService {
    /**
     * 查询内衬库存信息
     *
     * @param id 内衬库存信息ID
     * @return 内衬库存信息
     */
    public NcStock selectStockById(Long id);

    /**
     * 查询内衬库存信息列表
     *
     * @param stock 内衬库存信息
     * @return 内衬库存信息集合
     */
    public List<NcStock> selectStockList(NcStock stock);

    /**
     * 新增内衬库存信息
     *
     * @param stock 内衬库存信息
     * @return 结果
     */
    public int insertStock(NcStock stock);

    /**
     * 修改内衬库存信息
     *
     * @param stock 内衬库存信息
     * @return 结果
     */
    public int updateStock(NcStock stock);

    /**
     * 批量删除内衬库存信息
     *
     * @param ids 需要删除的内衬库存信息ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验内衬库存唯一性（根据库存日期+物料编号+id）
     */
    public List<NcStock> checkStockListUnic(NcStock stock);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcStock> list, boolean updateSupport, Long importLogId);
}
