package com.zlt.sync.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.sync.domain.AuxSyncStatusRecords;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 状态记录数据接口
 *  - 记录首日
 *  - 最大时间等
 */
@DS(DataSource.MASTER)
@Mapper
public interface AuxSyncStatusRecordsMapper {

    /**
     * 返回列表
     * @param params
     * @return
     */
    List<AuxSyncStatusRecords> selectList(Map<String, Object> params);

    int insert(@Param("auxSyncStatusRecords") AuxSyncStatusRecords auxSyncStatusRecords);

    int update(@Param("auxSyncStatusRecords") AuxSyncStatusRecords auxSyncStatusRecords);
}
