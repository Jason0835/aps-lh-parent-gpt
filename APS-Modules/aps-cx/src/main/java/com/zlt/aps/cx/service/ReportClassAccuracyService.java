package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;

import java.util.List;

/**
 * 班次完成统计报表Service接口
 *
 * @author chen
 * @date 2022-05-23
 */
public interface ReportClassAccuracyService {

    /**
     * 查询班次完成统计报表列表
     *
     * @param reportClassAccuracy 班次完成统计报表
     * @return 班次完成统计报表集合
     */
    public List<ReportClassAccuracyDto> selectReportClassAccuracyList(ReportClassAccuracyDto reportClassAccuracy);

    /**
     * 导出班次完成统计报表列表
     */
    public byte[] export(ReportClassAccuracyDto reportClassAccuracy);
}
