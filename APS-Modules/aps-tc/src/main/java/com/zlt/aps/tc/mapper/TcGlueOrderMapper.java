package com.zlt.aps.tc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tc.api.domain.dto.TcGlueOrderDto;
import com.zlt.aps.tc.entity.TcGlueOrder;

import java.util.List;

/**
 * <p>
 * 胎侧胶料顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
public interface TcGlueOrderMapper extends BaseMapper<TcGlueOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<TcGlueOrderDto> listGlueOrder(TcGlueOrderDto dto);

    /**
     * 合并操作，存在则更新，否则新增
     */
    public void mergeSql(List<TcGlueOrderDto> list);
}
