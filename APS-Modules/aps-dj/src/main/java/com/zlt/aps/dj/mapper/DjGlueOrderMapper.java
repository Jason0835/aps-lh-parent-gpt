package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjGlueOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueOrder;

/**
 * <p>
 * 垫胶胶料顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
public interface DjGlueOrderMapper extends BaseMapper<DjGlueOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<DjGlueOrderDto> listGlueOrder(DjGlueOrderDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     * @param list 要合并的集合
     */
    public void mergeSql(List<DjGlueOrderDto> list);
}
