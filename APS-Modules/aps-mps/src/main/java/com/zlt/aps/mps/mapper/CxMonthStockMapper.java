package com.zlt.aps.mps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import com.zlt.aps.mps.domain.CxMonthStock;

import java.util.List;

/**
 * 成型月结库存Mapper接口
 *
 * @author chen
 * @date 2021-06-17
 */
public interface CxMonthStockMapper extends BaseMapper<CxMonthStock> {

    /**
     * 查询成型定额设定列表
     *
     * @param dto 成型定额设定
     * @return 成型定额设定集合
     */
    public List<CxMonthStockDto> selectCxMonthStockList(CxMonthStockDto dto);

    public List<CxMonthStock> checkCxMonthStockUnique(CxMonthStock entity);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxMonthStock> list);

}
