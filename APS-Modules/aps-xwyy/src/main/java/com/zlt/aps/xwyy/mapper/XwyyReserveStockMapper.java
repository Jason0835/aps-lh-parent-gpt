package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.entity.XwyyReserveStock;

import java.util.List;

/**
 * 纤维压延预生产库存倍数设定Mapper接口
 *
 * @author hak
 * @date 2025-02-11
 */
public interface XwyyReserveStockMapper extends BaseMapper<XwyyReserveStock> {

    /**
     * 查询纤维压延预生产库存倍数设定
     *
     * @param id 纤维压延预生产库存倍数设定ID
     * @return 纤维压延预生产库存倍数设定
     */
    public XwyyReserveStock selectReserveStockById(Long id);

    /**
     * 查询纤维压延预生产库存倍数设定列表
     *
     * @param reserveStock 纤维压延预生产库存倍数设定
     * @return 纤维压延预生产库存倍数设定集合
     */
    public List<XwyyReserveStockDto> selectReserveStockList(XwyyReserveStock reserveStock);

    /**
     * 校验预生产库存倍数设定记录唯一性
     *
     * @param reserveStock 要校验的记录
     * @return 查询到的集合
     */
    public List<XwyyReserveStock> checkUnique(XwyyReserveStock reserveStock);

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
    public void mergeSql(List<XwyyReserveStockDto> list);
}
