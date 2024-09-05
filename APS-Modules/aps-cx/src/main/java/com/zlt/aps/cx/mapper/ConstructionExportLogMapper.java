package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.ConstructionExportLog;

/**
 * 施工信息导出日志Mapper接口
 * 
 * @author zlt
 * @date 2021-12-28
 */
public interface ConstructionExportLogMapper 
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
    public int insertConstructionExportLog(ConstructionExportLog constructionExportLog);

    /**
     * 修改施工信息导出日志
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 结果
     */
    public int updateConstructionExportLog(ConstructionExportLog constructionExportLog);

    /**
     * 删除施工信息导出日志
     * 
     * @param id 施工信息导出日志ID
     * @return 结果
     */
    public int deleteConstructionExportLogById(Long id);

    /**
     * 批量删除施工信息导出日志
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteConstructionExportLogByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<ConstructionExportLog> list);
}
