package com.zlt.aps.nc.engine.service;

import java.util.Date;
import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;

/**
 * 内衬排程批次号与工单号生成服务
 * <p>
 * 统一管理批次号和工单号的生成规则，供排程引擎和调整模块共同使用。
 * </p>
 */
public interface INcOrderGeneratorService {

    /**
     * 生成批次号
     * <p>
     * 格式：DJ + yyyyMMdd + 3位序号（如 DJ20260627001）。
     * 序号基于当天已存在的批次号最大值自增。
     * </p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排产日期
     * @return 批次号
     */
    String generateBatchNo(String factoryCode, Date scheduleDate);

    /**
     * 生成工单号
     * <p>
     * 格式：batchNo + "-" + 4位序号（如 DJ20260627001-0001）。
     * 基于批次号和当前最大流水号生成下一个工单号（maxSeq + 1）。
     * 不包含数据库查询逻辑，调用方需自行计算最大流水号。
     * </p>
     *
     * @param batchNo 批次号
     * @param maxSeq  当前最大流水号（从0开始），返回 maxSeq + 1 对应的工单号
     * @return 工单号
     */
    String generateOrderNo(String batchNo, int maxSeq);

    /**
     * 批量填充批次号和工单号
     * <p>
     * 为排程结果列表统一分配批次号，并按顺序生成工单号。
     * 用于自动排程场景。
     * </p>
     *
     * @param results      排程结果列表
     * @param factoryCode  工厂编码
     * @param scheduleDate 排产日期
     * @return 生成的批次号
     */
    String fillOrderInfo(List<NcScheduleResult> results, String factoryCode, Date scheduleDate);
}
