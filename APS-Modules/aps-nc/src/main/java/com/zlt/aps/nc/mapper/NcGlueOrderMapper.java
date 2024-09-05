package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcGlueOrderDto;
import com.zlt.aps.nc.entity.NcGlueOrder;

import java.util.List;

/**
 * <p>
 * 内衬胶料顺序维护 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-05-28
 */
public interface NcGlueOrderMapper extends BaseMapper<NcGlueOrder> {

    /**
     * 根据条件查询胶料组别顺序列表
     *
     * @param dto
     * @return
     */
    List<NcGlueOrderDto> listGlueOrder(NcGlueOrderDto dto);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcGlueOrderDto> list);
}
