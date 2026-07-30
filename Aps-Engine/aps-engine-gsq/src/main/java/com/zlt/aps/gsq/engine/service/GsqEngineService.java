package com.zlt.aps.gsq.engine.service;

import com.zlt.aps.gsq.engine.vo.GsqScheduleBaseInfoVo;

import java.util.List;

/**
 * 钢丝圈排程引擎服务接口
 *
 * <p>提供钢丝圈自动排程、施工基础信息查询、批次号/工单号生成等能力，
 * 供 aps-gsq 业务模块的插单、调量等人工操作调用。</p>
 *
 * @author APS
 */
public interface GsqEngineService {

    /**
     * 钢丝圈胶自动排程（6班制新架构入口）
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param factoryCode  分厂编码
     */
    void autoGsqSchedule(String scheduleDate, String factoryCode);

    /**
     * 根据钢丝圈代码列表查询施工基础信息（用于插单前规格校验和施工字段回填）。
     *
     * <p>查询 T_PRODUCT_CONSTRUCTION_INFO 施工表，按钢丝圈代码聚合返回：
     * 钢丝类型、排列、英寸、钢丝直径、BOM用量等。</p>
     *
     * @param steelRingCodes 钢丝圈代码列表
     * @return 施工基础信息列表，空列表表示施工不存在
     */
    List<GsqScheduleBaseInfoVo> listGsqScheduleBaseInfo(List<String> steelRingCodes);

    /**
     * 生成钢丝圈插单的批次号和工单号。
     *
     * <p>批次号规则：前缀 + 排程日期(yyyyMMdd) + 自增序号；
     * 工单号规则：批次号 + 自增序号。</p>
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return 长度为2的数组：[0]=批次号，[1]=工单号
     */
    String[] generateBatchNoAndOrderNo(String scheduleDate);
}
