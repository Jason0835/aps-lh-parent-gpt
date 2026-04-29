package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqTooling;

import java.util.List;

public interface TqToolingMapper extends BaseMapper<TqTooling> {

    List<TqTooling> listTooling(TqTooling entity);

    int checkUnique(TqTooling tooling);

    void mergeSql(List<TqTooling> list);

    void deleteAllTooling();
}
