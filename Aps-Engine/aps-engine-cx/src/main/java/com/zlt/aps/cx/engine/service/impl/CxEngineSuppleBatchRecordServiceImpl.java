package com.zlt.aps.cx.engine.service.impl;

import java.util.Date;
import java.util.List;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxEngineSuppleBatchRecord;
import com.zlt.aps.cx.engine.mapper.CxEngineSuppleBatchRecordMapper;
import com.zlt.aps.cx.engine.service.CxEngineSuppleBatchRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 成型前日计划增补批次Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2022-02-09
 */
@Service("cxEngineSuppleBatchRecordService")
public class CxEngineSuppleBatchRecordServiceImpl implements CxEngineSuppleBatchRecordService
{
    @Autowired
    private CxEngineSuppleBatchRecordMapper cxEngineSuppleBatchRecordMapper;

    /**
     * 查询成型前日计划增补批次
     * 
     * @param id 成型前日计划增补批次ID
     * @return 成型前日计划增补批次
     */
    @Override
    public CxEngineSuppleBatchRecord selectCxEngineSuppleBatchRecordById(Long id)
    {
        return cxEngineSuppleBatchRecordMapper.selectCxEngineSuppleBatchRecordById(id);
    }

    /**
     * 查询成型前日计划增补批次列表
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 成型前日计划增补批次
     */
    @Override
    public List<CxEngineSuppleBatchRecord> selectCxEngineSuppleBatchRecordList(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord)
    {
        return cxEngineSuppleBatchRecordMapper.selectCxEngineSuppleBatchRecordList(cxEngineSuppleBatchRecord);
    }

    /**
     * 根据批次号日期进行增补计划批次获取
     * @param suppleBatchNo
     * @param suppleDate
     * @return
     */
    @Override
    public CxEngineSuppleBatchRecord createSuppleBatchRecord(String suppleBatchNo, Date suppleDate) {
        CxEngineSuppleBatchRecord record =new CxEngineSuppleBatchRecord();
        record.setSuppleBatchNo(suppleBatchNo);
        record.setSuppleDate(suppleDate);
        insertCxEngineSuppleBatchRecord(record);
        return record;
    }

    /**
     * 新增成型前日计划增补批次
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 结果
     */
    @Override
    public int insertCxEngineSuppleBatchRecord(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord)
    {
        cxEngineSuppleBatchRecord.setBaseVale(null);
        return cxEngineSuppleBatchRecordMapper.insertCxEngineSuppleBatchRecord(cxEngineSuppleBatchRecord);
    }

    /**
     * 修改成型前日计划增补批次
     * 
     * @param cxEngineSuppleBatchRecord 成型前日计划增补批次
     * @return 结果
     */
    @Override
    public int updateCxEngineSuppleBatchRecord(CxEngineSuppleBatchRecord cxEngineSuppleBatchRecord)
    {
        cxEngineSuppleBatchRecord.setBaseVale(cxEngineSuppleBatchRecord.getId());
        return cxEngineSuppleBatchRecordMapper.updateCxEngineSuppleBatchRecord(cxEngineSuppleBatchRecord);
    }

    /**
     * 批量删除成型前日计划增补批次
     * 
     * @param ids 需要删除的成型前日计划增补批次ID
     * @return 结果
     */
    @Override
    public int deleteCxEngineSuppleBatchRecordByIds(Long[] ids)
    {
        return cxEngineSuppleBatchRecordMapper.deleteCxEngineSuppleBatchRecordByIds(ids);
    }

    /**
     * 删除成型前日计划增补批次信息
     * 
     * @param id 成型前日计划增补批次ID
     * @return 结果
     */
    @Override
    public int deleteCxEngineSuppleBatchRecordById(Long id)
    {
        return cxEngineSuppleBatchRecordMapper.deleteCxEngineSuppleBatchRecordById(id);
    }

    /**
     * 验证是否存在日期增补计划批次记录
     * @param suppleDateStr 增补计划日期
     * @return 存在返回错误信息，不存在则返回空
     */
    @Override
    public String isExistRecordByDateStr(String suppleDateStr) {
        StringBuilder errorMsg=new StringBuilder("");
        CxEngineSuppleBatchRecord condition=new CxEngineSuppleBatchRecord();
        //未确认状态
        if(StringUtils.isNotEmpty(suppleDateStr)){
            condition.setSuppleDateStr(suppleDateStr);
        }
        List<CxEngineSuppleBatchRecord> existList=selectCxEngineSuppleBatchRecordList(condition);
        if(StringUtils.isNotEmpty(existList)){
            errorMsg.append(I18nUtil.getMessage("cx.engine.last.supple.plan.exist"));
        }

        return errorMsg.toString();
    }

    /**
     * 根据增补日期删除增补批次
     * @param suppleDate 增补日期 增补日期
     * @return
     */
    @Override
    public int deleteCxEngineSuppleBatchRecordBySuppleDate(String suppleDate) {
        return cxEngineSuppleBatchRecordMapper.deleteCxEngineSuppleBatchRecordBySuppleDate(suppleDate);
    }


}
