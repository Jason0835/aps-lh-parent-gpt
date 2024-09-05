package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleStopInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型机台自动停排信息Service接口
 *
 * @author chen
 * @date 2022-04-03
 */
public interface CxScheduleStopInfoService {
    /**
     * 查询成型机台自动停排信息
     *
     * @param id 成型机台自动停排信息ID
     * @return 成型机台自动停排信息
     */
    public CxScheduleStopInfo selectCxScheduleStopInfoById(Long id);

    /**
     * 查询成型机台自动停排信息列表
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 成型机台自动停排信息集合
     */
    public List<CxScheduleStopInfo> selectCxScheduleStopInfoList(CxScheduleStopInfo cxScheduleStopInfo);

    /**
     * 新增成型机台自动停排信息
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 结果
     */
    @Transactional
    public int insertCxScheduleStopInfo(CxScheduleStopInfo cxScheduleStopInfo);

    /**
     * 修改成型机台自动停排信息
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 结果
     */
    @Transactional
    public int updateCxScheduleStopInfo(CxScheduleStopInfo cxScheduleStopInfo);

    /**
     * 批量删除成型机台自动停排信息
     *
     * @param ids 需要删除的成型机台自动停排信息ID
     * @return 结果
     */
    @Transactional
    public int deleteCxScheduleStopInfoByIds(Long[] ids);

    /**
     * 删除成型机台自动停排信息信息
     *
     * @param id 成型机台自动停排信息ID
     * @return 结果
     */
    @Transactional
    public int deleteCxScheduleStopInfoById(Long id);

    /**
     * 校验成型机台自动停排信息唯一性
     */
    public String checkCxScheduleStopInfoUnique(CxScheduleStopInfo cxScheduleStopInfo);

    /**
     * 导入成型机台自动停排信息数据
     */
    @Transactional
    public AjaxResult importData(List<CxScheduleStopInfo> list, boolean updateSupport, Long importLogId);
}
