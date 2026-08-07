package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.domain.Cd15MesStock;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 斜裁MES接口数据Mapper。
 */
@Mapper
@DS(DataSource.MES)
public interface Cd15MesItfMapper {

    /** 查询斜裁库存同步数据。 */
    List<Cd15MesStock> selectStockList(AuxReqSyncDataLogs syncDataLogs);

    /** 查询斜裁自动滚动目标班次库存数据。 */
    List<Cd15MesStock> selectShiftStockList(MesShiftStockSyncRequest request);

    /** 查询斜裁目标日期班次库排快照。 */
    List<Cd15StorageLaneLimit> selectStorageLaneLimitList(AuxReqSyncDataLogs syncDataLogs);
}
