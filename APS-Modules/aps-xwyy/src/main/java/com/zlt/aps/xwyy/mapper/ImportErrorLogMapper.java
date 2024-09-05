package com.zlt.aps.xwyy.mapper;


import com.ruoyi.api.gateway.system.domain.ImportErrorLog;

import java.util.List;

/**
 * 导入错误日志记录Mapper接口
 *
 * @author zlt
 * @date 2021-07-26
 */
public interface ImportErrorLogMapper {
    /**
     * 查询导入错误日志记录
     *
     * @param id 导入错误日志记录ID
     * @return 导入错误日志记录
     */
    public ImportErrorLog selectImportErrorLogById(Long id);

    /**
     * 查询导入错误日志记录列表
     *
     * @param importErrorLog 导入错误日志记录
     * @return 导入错误日志记录集合
     */
    public List<ImportErrorLog> selectImportErrorLogList(ImportErrorLog importErrorLog);

    /**
     * 新增导入错误日志记录
     *
     * @param importErrorLog 导入错误日志记录
     * @return 结果
     */
    public int insertImportErrorLog(ImportErrorLog importErrorLog);

    /**
     * 修改导入错误日志记录
     *
     * @param importErrorLog 导入错误日志记录
     * @return 结果
     */
    public int updateImportErrorLog(ImportErrorLog importErrorLog);

    /**
     * 删除导入错误日志记录
     *
     * @param id 导入错误日志记录ID
     * @return 结果
     */
    public int deleteImportErrorLogById(Long id);

    /**
     * 批量删除导入错误日志记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteImportErrorLogByIds(Long[] ids);

    /**
     * 批量新增导入错误日志记录
     *
     * @param list 要新增的记录
     * @return 结果
     */
    public int insertImportErrorLogList(List<ImportErrorLog> list);
}
