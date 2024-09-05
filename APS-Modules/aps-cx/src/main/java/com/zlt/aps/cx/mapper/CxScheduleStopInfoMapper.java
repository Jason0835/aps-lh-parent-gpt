package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxScheduleStopInfo;

import java.util.List;

/**
 * 成型机台自动停排信息Mapper接口
 *
 * @author chen
 * @date 2022-04-03
 */
public interface CxScheduleStopInfoMapper {
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
    public int insertCxScheduleStopInfo(CxScheduleStopInfo cxScheduleStopInfo);

    /**
     * 修改成型机台自动停排信息
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 结果
     */
    public int updateCxScheduleStopInfo(CxScheduleStopInfo cxScheduleStopInfo);

    /**
     * 删除成型机台自动停排信息
     *
     * @param id 成型机台自动停排信息ID
     * @return 结果
     */
    public int deleteCxScheduleStopInfoById(Long id);

    /**
     * 批量删除成型机台自动停排信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxScheduleStopInfoByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxScheduleStopInfo> list);
}
