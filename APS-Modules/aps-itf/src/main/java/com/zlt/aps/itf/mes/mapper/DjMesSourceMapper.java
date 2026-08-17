package com.zlt.aps.itf.mes.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishTotal;
import com.zlt.aps.dj.api.domain.entity.DjScheFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjStock;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;

/**
 * MES垫胶库存和完成量源表查询Mapper。
 */
@DS(DataSource.MES)
@Mapper
public interface DjMesSourceMapper {

    /**
     * 查询垫胶库存最新快照。
     *
     * @param request 同步请求
     * @return 库存列表
     */
    List<DjStock> selectStockList(AuxReqSyncDataLogs request);

    /**
     * 查询垫胶当天三班完成量最新快照。
     *
     * @param request 同步请求
     * @return 班次完成量
     */
    List<DjDayFinishQty> selectShiftFinishQtyList(AuxReqSyncDataLogs request);

    /**
     * 查询垫胶指定日期日完成量快照。
     *
     * @param request 同步请求
     * @return 日完成量
     */
    List<DjDayFinishTotal> selectDayFinishQtyList(AuxReqSyncDataLogs request);
}
