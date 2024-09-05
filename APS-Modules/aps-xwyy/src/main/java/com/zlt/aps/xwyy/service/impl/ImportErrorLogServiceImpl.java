package com.zlt.aps.xwyy.service.impl;


import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.xwyy.mapper.ImportErrorLogMapper;
import com.zlt.aps.xwyy.service.ImportErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 导入错误日志记录Service业务层处理
 *
 * @author zlt
 * @date 2021-07-26
 */
@Service
public class ImportErrorLogServiceImpl implements ImportErrorLogService {
    @Autowired
    private ImportErrorLogMapper importErrorLogMapper;

    /**
     * 查询导入错误日志记录
     *
     * @param id 导入错误日志记录ID
     * @return 导入错误日志记录
     */
    @Override
    public ImportErrorLog selectImportErrorLogById(Long id) {
        return importErrorLogMapper.selectImportErrorLogById(id);
    }

    /**
     * 查询导入错误日志记录列表
     *
     * @param importErrorLog 导入错误日志记录
     * @return 导入错误日志记录
     */
    @Override
    public List<ImportErrorLog> selectImportErrorLogList(ImportErrorLog importErrorLog) {
        return importErrorLogMapper.selectImportErrorLogList(importErrorLog);
    }

    /**
     * 新增导入错误日志记录
     *
     * @param importErrorLog 导入错误日志记录
     * @return 结果
     */
    @Override
    public int insertImportErrorLog(ImportErrorLog importErrorLog) {
        importErrorLog.setCreateBy(SecurityUtils.getUsername());
        importErrorLog.setCreateTime(new Date());
        importErrorLog.setDelFlag("0");
        return importErrorLogMapper.insertImportErrorLog(importErrorLog);
    }

    /**
     * 修改导入错误日志记录
     *
     * @param importErrorLog 导入错误日志记录
     * @return 结果
     */
    @Override
    public int updateImportErrorLog(ImportErrorLog importErrorLog) {
        importErrorLog.setUpdateBy(SecurityUtils.getUsername());
        importErrorLog.setUpdateTime(new Date());
        return importErrorLogMapper.updateImportErrorLog(importErrorLog);
    }

    /**
     * 批量删除导入错误日志记录
     *
     * @param ids 需要删除的导入错误日志记录ID
     * @return 结果
     */
    @Override
    public int deleteImportErrorLogByIds(Long[] ids) {
        return importErrorLogMapper.deleteImportErrorLogByIds(ids);
    }

    /**
     * 删除导入错误日志记录信息
     *
     * @param id 导入错误日志记录ID
     * @return 结果
     */
    @Override
    public int deleteImportErrorLogById(Long id) {
        return importErrorLogMapper.deleteImportErrorLogById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkImportErrorLogUnique(ImportErrorLog importErrorLog) {
        if (importErrorLog == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<ImportErrorLog> list = importErrorLogMapper.selectImportErrorLogList(importErrorLog);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 批量新增导入错误日志记录
     *
     * @param importErrorLogs 导入错误日志记录
     * @return 结果
     */
    @Override
    public int insertImportErrorLogList(List<ImportErrorLog> importErrorLogs) {
        return importErrorLogMapper.insertImportErrorLogList(importErrorLogs);
    }
}
