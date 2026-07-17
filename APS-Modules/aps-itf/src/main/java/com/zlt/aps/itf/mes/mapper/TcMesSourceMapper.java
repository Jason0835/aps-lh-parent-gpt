package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcMesStock;
import com.zlt.aps.tc.api.domain.entity.TcScheFinishQty;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES胎侧库存和完成量源表查询Mapper。
 */
@DS(DataSource.MES)
@Mapper
public interface TcMesSourceMapper {

    /**
     * 查询胎侧库存最新快照。
     *
     * @param request 同步请求
     * @return 库存列表
     */
    List<TcMesStock> selectStockList(AuxReqSyncDataLogs request);

    /**
     * 查询胎侧当天三班完成量最新快照。
     *
     * @param request 同步请求
     * @return 班次完成量
     */
    List<TcScheFinishQty> selectShiftFinishQtyList(AuxReqSyncDataLogs request);

    /**
     * 查询胎侧指定日期日完成量快照。
     *
     * @param request 同步请求
     * @return 日完成量
     */
    List<TcDayFinishQty> selectDayFinishQtyList(AuxReqSyncDataLogs request);
}
