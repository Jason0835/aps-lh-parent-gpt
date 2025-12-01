package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyyReserveStockDto;
import com.zlt.aps.gdyy.entity.GdyyReserveStock;

import java.util.List;

/**
 * 钢带压延预生产库存倍数设定Mapper接口
 *
 * @author hak
 * @date 2025-02-11
 */
public interface GdyyReserveStockMapper extends BaseMapper<GdyyReserveStock> {

    /**
     * 查询钢带压延预生产库存倍数设定
     *
     * @param id 钢带压延预生产库存倍数设定ID
     * @return 钢带压延预生产库存倍数设定
     */
    public GdyyReserveStock selectReserveStockById(Long id);

    /**
     * 查询钢带压延预生产库存倍数设定列表
     *
     * @param reserveStock 钢带压延预生产库存倍数设定
     * @return 钢带压延预生产库存倍数设定集合
     */
    public List<GdyyReserveStockDto> selectReserveStockList(GdyyReserveStock reserveStock);

    /**
     * 校验预生产库存倍数设定记录唯一性
     *
     * @param reserveStock 要校验的记录
     * @return 查询到的集合
     */
    public List<GdyyReserveStock> checkUnique(GdyyReserveStock reserveStock);

    /**
     * 批量删除预生产库存倍数设定记录
     * @param ids id集合
     * @return 结果
     */
    public int deleteReserveStockByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GdyyReserveStockDto> list);
}
