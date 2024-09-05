package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.ConstructionExportLog;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 施工信息导出日志Service接口
 * 
 * @author zlt
 * @date 2021-12-28
 */
public interface ConstructionExportLogService
{
    /**
     * 查询施工信息导出日志
     * 
     * @param id 施工信息导出日志ID
     * @return 施工信息导出日志
     */
    public ConstructionExportLog selectConstructionExportLogById(Long id);

    /**
     * 查询施工信息导出日志列表
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 施工信息导出日志集合
     */
    public List<ConstructionExportLog> selectConstructionExportLogList(ConstructionExportLog constructionExportLog);

    /**
     * 新增施工信息导出日志
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 结果
     */
    @Transactional
    public int insertConstructionExportLog(ConstructionExportLog constructionExportLog);

    /**
     * 修改施工信息导出日志
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 结果
     */
    @Transactional
    public int updateConstructionExportLog(ConstructionExportLog constructionExportLog);

    /**
     * 批量删除施工信息导出日志
     * 
     * @param ids 需要删除的施工信息导出日志ID
     * @return 结果
     */
    @Transactional
    public int deleteConstructionExportLogByIds(Long[] ids);

    /**
     * 删除施工信息导出日志信息
     * 
     * @param id 施工信息导出日志ID
     * @return 结果
     */
    @Transactional
    public int deleteConstructionExportLogById(Long id);

    /**
     * 校验施工信息导出日志唯一性
     */
    public String checkConstructionExportLogUnique(ConstructionExportLog constructionExportLog);

    /**
     * 导入施工信息导出日志数据
     */
    @Transactional
    public AjaxResult importData(List<ConstructionExportLog> list, boolean updateSupport, Long importLogId);
}
