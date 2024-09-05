package com.ruoyi.system.service;


import com.ruoyi.api.gateway.system.domain.ImportLog;

import java.util.List;


/**
 * 导入记录Service接口
 * 
 * @author zlt
 * @date 2021-07-26
 */
public interface ImportLogService
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
    public ImportLog insertImportLog(ImportLog importLog);

    /**
     * 修改导入记录
     * 
     * @param importLog 导入记录
     * @return 结果
     */
    public int updateImportLog(ImportLog importLog);

    /**
     * 批量删除导入记录
     * 
     * @param ids 需要删除的导入记录ID
     * @return 结果
     */
    public int deleteImportLogByIds(Long[] ids);

    /**
     * 删除导入记录信息
     * 
     * @param id 导入记录ID
     * @return 结果
     */
    public int deleteImportLogById(Long id);

    /**
     * 校验导入记录唯一性
     */
    public String checkImportLogUnique(ImportLog importLog);
}
