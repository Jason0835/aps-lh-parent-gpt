package com.zlt.aps.gdyy.mapper;

import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 钢带压延库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-31
 */
public interface GdyyStockMapper {
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
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<GdyyStock> checkStockListUnic(GdyyStock stock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GdyyStock> list);

    /**
     * 根据库存日期删除库存信息
     *
     * @param scheduleDate 库存日期
     * @return 删除数量
     */
    int deleteStockByDate(@Param("scheduleDate") Date scheduleDate);
}
