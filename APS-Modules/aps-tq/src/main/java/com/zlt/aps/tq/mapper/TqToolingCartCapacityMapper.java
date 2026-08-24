package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;

import java.util.List;

public interface TqToolingCartCapacityMapper extends BaseMapper<TqToolingCartCapacity> {

    List<TqToolingCartCapacity> listToolingCartCapacity(TqToolingCartCapacity entity);

    void mergeSql(List<TqToolingCartCapacity> list);

    void deleteAllToolingCartCapacity();
}
