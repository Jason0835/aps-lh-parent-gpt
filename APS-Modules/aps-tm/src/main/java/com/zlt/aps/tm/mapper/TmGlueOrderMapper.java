package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmGlueOrderDto;
import com.zlt.aps.tm.entity.TmGlueOrder;

import java.util.List;

/**
 * <p>
 * 胎面胶料顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
public interface TmGlueOrderMapper extends BaseMapper<TmGlueOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TmGlueOrderDto> listGlueOrder(TmGlueOrderDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmGlueOrderDto> list);
}
