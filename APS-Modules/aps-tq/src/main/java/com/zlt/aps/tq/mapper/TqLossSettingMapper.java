package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqLossSetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TqLossSettingMapper extends BaseMapper<TqLossSetting> {

    List<TqLossSetting> selectLossSettingList(TqLossSetting lossSetting);

    void mergeSql(@Param("list") List<TqLossSetting> list);
}
