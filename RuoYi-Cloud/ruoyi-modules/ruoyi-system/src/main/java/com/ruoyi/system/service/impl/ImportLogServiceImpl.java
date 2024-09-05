package com.ruoyi.system.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.system.mapper.ImportLogMapper;
import com.ruoyi.system.service.ImportLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


/**
 * 导入记录Service业务层处理
 * 
 * @author zlt
 * @date 2021-07-26
 */
@Service
public class ImportLogServiceImpl implements ImportLogService
{
    @Autowired
    private ImportLogMapper importLogMapper;

    /**
     * 查询导入记录
     * 
     * @param id 导入记录ID
     * @return 导入记录
     */
    @Override
    public ImportLog selectImportLogById(Long id)
    {
        return importLogMapper.selectImportLogById(id);
    }

    /**
     * 查询导入记录列表
     * 
     * @param importLog 导入记录
     * @return 导入记录
     */
    @Override
    public List<ImportLog> selectImportLogList(ImportLog importLog)
    {
        return importLogMapper.selectImportLogList(importLog);
    }

    /**
     * 新增导入记录
     * 
     * @param importLog 导入记录
     * @return 结果
     */
    @Override
    public ImportLog insertImportLog(ImportLog importLog)
    {
        importLog.setCreateBy(SecurityUtils.getUsername());
        importLog.setCreateTime(new Date());
        importLog.setDelFlag("0");
        importLogMapper.insertImportLog(importLog);
        return importLog;
    }

    /**
     * 修改导入记录
     * 
     * @param importLog 导入记录
     * @return 结果
     */
    @Override
    public int updateImportLog(ImportLog importLog)
    {
        importLog.setUpdateBy(SecurityUtils.getUsername());
        importLog.setUpdateTime(new Date());
        return importLogMapper.updateImportLog(importLog);
    }

    /**
     * 批量删除导入记录
     * 
     * @param ids 需要删除的导入记录ID
     * @return 结果
     */
    @Override
    public int deleteImportLogByIds(Long[] ids)
    {
        return importLogMapper.deleteImportLogByIds(ids);
    }

    /**
     * 删除导入记录信息
     * 
     * @param id 导入记录ID
     * @return 结果
     */
    @Override
    public int deleteImportLogById(Long id)
    {
        return importLogMapper.deleteImportLogById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkImportLogUnique(ImportLog importLog) {
        if (importLog == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<ImportLog> list = importLogMapper.selectImportLogList(importLog);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

}
