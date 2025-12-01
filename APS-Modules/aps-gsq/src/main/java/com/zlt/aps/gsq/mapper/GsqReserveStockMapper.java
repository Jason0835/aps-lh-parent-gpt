package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;
import com.zlt.aps.gsq.entity.GsqReserveStock;

import java.util.List;

/**
 * 钢丝圈预生产库存倍数设定Mapper接口
 *
 * @author hak
 * @date 2025-02-11
 */
public interface GsqReserveStockMapper extends BaseMapper<GsqReserveStock> {

    /**
     * 查询钢丝圈预生产库存倍数设定
     *
     * @param id 钢丝圈预生产库存倍数设定ID
     * @return 钢丝圈预生产库存倍数设定
     */
    public GsqReserveStock selectReserveStockById(Long id);

    /**
     * 查询钢丝圈预生产库存倍数设定列表
     *
     * @param reserveStock 钢丝圈预生产库存倍数设定
     * @return 钢丝圈预生产库存倍数设定集合
     */
    public List<GsqReserveStockDto> selectReserveStockList(GsqReserveStock reserveStock);

    /**
     * 校验预生产库存倍数设定记录唯一性
     *
     * @param reserveStock 要校验的记录
     * @return 查询到的集合
     */
    public List<GsqReserveStock> checkUnique(GsqReserveStock reserveStock);

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
    public void mergeSql(List<GsqReserveStockDto> list);
}
