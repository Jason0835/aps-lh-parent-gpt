package com.ruoyi.system.mapper;


import com.ruoyi.api.gateway.system.domain.ImportLog;

import java.util.List;

/**
 * 导入记录Mapper接口
 * 
 * @author zlt
 * @date 2021-07-26
 */
public interface ImportLogMapper
{
    /**
     * 查询导入记录
     * 
     * @param id 导入记录ID
     * @return 导入记录
     */
    public ImportLog selectImportLogById(Long id);

    /**
     * 查询导入记录列表
     * 
     * @param importLog 导入记录
     * @return 导入记录集合
     */
    public List<ImportLog> selectImportLogList(ImportLog importLog);

    /**
     * 新增导入记录
     * 
     * @param importLog 导入记录
     * @return 结果
     */
    public int insertImportLog(ImportLog importLog);

    /**
     * 修改导入记录
     * 
     * @param importLog 导入记录
     * @return 结果
     */
    public int updateImportLog(ImportLog importLog);

    /**
     * 删除导入记录
     * 
     * @param id 导入记录ID
     * @return 结果
     */
    public int deleteImportLogById(Long id);

    /**
     * 批量删除导入记录
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteImportLogByIds(Long[] ids);
}
