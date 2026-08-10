package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.mp.api.domain.entity.Cd90MesStock;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 直裁MES接口数据Mapper。
 */
@Mapper
@DS(DataSource.MES)
public interface Cd90MesItfMapper {

    /**
     * 查询直裁库存同步数据。
     *
     * @param syncDataLogs 同步参数
     * @return 库存列表
     */
    List<Cd90MesStock> selectStockList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询直裁自动滚动目标班次库存数据。
     *
     * @param request 目标库存日期和可选MES版本
     * @return 班次库存来源列表
     */
    List<Cd90MesStock> selectShiftStockList(MesShiftStockSyncRequest request);

    /**
     * 查询直裁库排状态最新快照。
     *
     * @param syncDataLogs 同步参数
     * @return 库排状态
     */
    List<Cd90StorageLaneLimit> selectStorageLaneLimitList(AuxReqSyncDataLogs syncDataLogs);

    /**
     * 查询直裁每日三班完成量同步数据。
     *
     * @param syncDataLogs 同步参数
     * @return 每日完成量列表
     */
    List<Cd90ScheFinishQty> selectClassShiftFinishQtyList(AuxReqSyncDataLogs syncDataLogs);
}
