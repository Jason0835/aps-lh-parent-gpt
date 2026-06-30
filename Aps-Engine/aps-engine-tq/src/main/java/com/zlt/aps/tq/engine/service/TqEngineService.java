package com.zlt.aps.tq.engine.service;

import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.engine.vo.TqScheduleBaseInfoVo;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;

import java.util.List;

public interface TqEngineService {

    /**
     * 胎圈胶自动排程
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param factoryCode 分厂编码
     */
    void autoTqSchedule(String scheduleDate, String factoryCode);

    /**
     * 胎圈插单
     * @param scheduleVo
     */
    int inertTqOrder(TqScheduleResultVo scheduleVo);

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    int batchSaveTqSchedule(String scheduleDate, List<TqScheduleResultVo> scheduleList);

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    void changeTqMachine(String oldMachineIds, TqScheduleResultVo scheduleResult);

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    void confirmTqMachine(TqScheduleResultDto scheduleResult);

    void batchUpdateBatchNoAndOrderNo(String scheduleDate);

    /**
     * 为插单记录生成批次号和工单号
     * 复用当前排程日期已有的批次号（若有），否则生成新批次号并创建排程记录；
     * 工单号基于批次号生成。不会影响其他记录的批次号/工单号。
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return 长度为2的数组，[0]=批次号batchNo，[1]=工单号orderNo
     */
    String[] generateBatchNoAndOrderNo(String scheduleDate);

    /**
     * 查询胎圈施工基础信息（用于校验胎圈规格施工是否存在，并回显钢丝圈、三角胶、尺寸等）
     *
     * @param beadCodes       胎圈代码集合
     * @param productionStage 生产阶段过滤（空串表示不过滤）
     * @return 施工基础信息列表
     */
    List<TqScheduleBaseInfoVo> listTqScheduleBaseInfo(List<String> beadCodes, String productionStage);
}
