package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineAutoScheduleRecord;

import java.util.Date;
import java.util.List;

/**
 * 成型自动排程记录Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-07-14
 */
public interface CxEngineAutoScheduleRecordService
{
    /**
     * 查询成型自动排程记录
     * 
     * @param id 成型自动排程记录ID
     * @return 成型自动排程记录
     */
    public CxEngineAutoScheduleRecord selectCxEngineAutoScheduleRecordById(Long id);

    /**
     * 查询成型自动排程记录列表
     * 
     * @param cxEngineAutoScheduleRecord 成型自动排程记录
     * @return 成型自动排程记录集合
     */
    public List<CxEngineAutoScheduleRecord> selectCxEngineAutoScheduleRecordList(CxEngineAutoScheduleRecord cxEngineAutoScheduleRecord);

    /**
     * 新增成型自动排程记录
     * 
     * @param cxEngineAutoScheduleRecord 成型自动排程记录
     * @return 结果
     */
    public int insertCxEngineAutoScheduleRecord(CxEngineAutoScheduleRecord cxEngineAutoScheduleRecord);

    /**
     * 修改成型自动排程记录
     * 
     * @param cxEngineAutoScheduleRecord 成型自动排程记录
     * @return 结果
     */
    public int updateCxEngineAutoScheduleRecord(CxEngineAutoScheduleRecord cxEngineAutoScheduleRecord);

    /**
     * 删除成型自动排程记录
     * 
     * @param id 成型自动排程记录ID
     * @return 结果
     */
    public int deleteCxEngineAutoScheduleRecordById(Long id);

    /**
     * 批量删除成型自动排程记录
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxEngineAutoScheduleRecordByIds(Long[] ids);

    /**
     * 删除成型自动排程记录
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int deleteAutoScheduleRecordByScheduleDate(String scheduleDate);

    /**
     * 生成日志
     * @param scheduleDate
     * @param monthPlanApsVersion
     * @param cxBatchNo
     * @param status
     * @param remark
     */
    public CxEngineAutoScheduleRecord generagAutoScheduleRecord(Date scheduleDate, String monthPlanApsVersion, String cxBatchNo, String status, String remark);

    /**
     * 根据排程日期获取排程日期对应的抓取记录
     * @param scheduleDate
     * @return
     */
    public CxEngineAutoScheduleRecord selectAutoScheduleRecordByScheduleDate(String scheduleDate);
}
