package com.zlt.aps.cx.engine.service;

import com.zlt.aps.cx.engine.domain.CxEngineSuppleBatchRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 成型前日计划增补批次Service接口
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
public interface CxEngineSuppleBatchRecordService
{
    /**
     * 查询成型前日计划增补批次
     * 
     * @param id 成型前日计划增补批次ID
     * @return 成型前日计划增补批次
     */
    public CxEngineSuppleBatchRecord selectCxEngineSuppleBatchRecordById(Long id);

    /**
     * 查询成型前日计划增补批次列表
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 成型前日计划增补批次集合
     */
    public List<CxEngineSuppleBatchRecord> selectCxEngineSuppleBatchRecordList(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord);

    /**
     * 生成增补计划批次
     * @param suppleBatchNo
     * @param suppleDate
     * @return
     */
    public CxEngineSuppleBatchRecord createSuppleBatchRecord(String suppleBatchNo, Date suppleDate);

    /**
     * 新增成型前日计划增补批次
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 结果
     */
    @Transactional
    public int insertCxEngineSuppleBatchRecord(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord);

    /**
     * 修改成型前日计划增补批次
     *
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 结果
     */
    @Transactional
    public int updateCxEngineSuppleBatchRecord(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord);

    /**
     * 批量删除成型前日计划增补批次
     * 
     * @param ids 需要删除的成型前日计划增补批次ID
     * @return 结果
     */
    @Transactional
    public int deleteCxEngineSuppleBatchRecordByIds(Long[] ids);

    /**
     * 删除成型前日计划增补批次信息
     * 
     * @param id 成型前日计划增补批次ID
     * @return 结果
     */
    @Transactional
    public int deleteCxEngineSuppleBatchRecordById(Long id);

    /**
     * 验证是否存在日期增补计划批次记录
     * @param suppleDateStr 增补计划日期
     * @return 存在返回错误信息，不存在则返回空
     */
    public String isExistRecordByDateStr(String suppleDateStr);

    /**
     * 根据增补日期删除日期对应的增补批次
     * @param suppleDate 增补日期
     * @return
     */
    public int deleteCxEngineSuppleBatchRecordBySuppleDate(String suppleDate);

}
