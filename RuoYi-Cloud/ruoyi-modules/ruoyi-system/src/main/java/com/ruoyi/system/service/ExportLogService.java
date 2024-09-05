package com.ruoyi.system.service;

import com.ruoyi.api.gateway.system.domain.ExportLog;

import java.util.List;


/**
 * 导出记录Service接口
 *
 * @author zlt
 * @date 2021-07-24
 */
public interface ExportLogService {
    /**
     * 查询导出记录
     *
     * @param id 导出记录ID
     * @return 导出记录
     */
    public ExportLog selectExportLogById(Long id);

    /**
     * 查询导出记录列表
     *
     * @param exportLog 导出记录
     * @return 导出记录集合
     */
    public List<ExportLog> selectExportLogList(ExportLog exportLog);

    /**
     * 新增导出记录
     *
     * @param exportLog 导出记录
     * @return 结果
     */
    public int insertExportLog(ExportLog exportLog);

    /**
     * 修改导出记录
     *
     * @param exportLog 导出记录
     * @return 结果
     */
    public int updateExportLog(ExportLog exportLog);

    /**
     * 批量删除导出记录
     *
     * @param ids 需要删除的导出记录ID
     * @return 结果
     */
    public int deleteExportLogByIds(Long[] ids);

    /**
     * 删除导出记录信息
     *
     * @param id 导出记录ID
     * @return 结果
     */
    public int deleteExportLogById(Long id);

    /**
     * 校验导出记录唯一性
     */
    public String checkExportLogUnique(ExportLog exportLog);
}
