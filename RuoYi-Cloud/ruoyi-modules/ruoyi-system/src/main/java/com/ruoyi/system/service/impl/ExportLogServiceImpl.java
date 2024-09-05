package com.ruoyi.system.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.system.mapper.ExportLogMapper;
import com.ruoyi.system.service.ExportLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


/**
 * 导出记录Service业务层处理
 *
 * @author zlt
 * @date 2021-07-24
 */
@Service
public class ExportLogServiceImpl implements ExportLogService {
    @Autowired
    private ExportLogMapper exportLogMapper;

    /**
     * 查询导出记录
     *
     * @param id 导出记录ID
     * @return 导出记录
     */
    @Override
    public ExportLog selectExportLogById(Long id) {
        return exportLogMapper.selectExportLogById(id);
    }

    /**
     * 查询导出记录列表
     *
     * @param exportLog 导出记录
     * @return 导出记录
     */
    @Override
    public List<ExportLog> selectExportLogList(ExportLog exportLog) {
        return exportLogMapper.selectExportLogList(exportLog);
    }

    /**
     * 新增导出记录
     *
     * @param exportLog 导出记录
     * @return 结果
     */
    @Override
    public int insertExportLog(ExportLog exportLog) {
        exportLog.setCreateBy(SecurityUtils.getUsername());
        exportLog.setCreateTime(new Date());
        exportLog.setDelFlag("0");
        return exportLogMapper.insertExportLog(exportLog);
    }

    /**
     * 修改导出记录
     *
     * @param exportLog 导出记录
     * @return 结果
     */
    @Override
    public int updateExportLog(ExportLog exportLog) {
        exportLog.setUpdateBy(SecurityUtils.getUsername());
        exportLog.setUpdateTime(new Date());
        return exportLogMapper.updateExportLog(exportLog);
    }

    /**
     * 批量删除导出记录
     *
     * @param ids 需要删除的导出记录ID
     * @return 结果
     */
    @Override
    public int deleteExportLogByIds(Long[] ids) {
        return exportLogMapper.deleteExportLogByIds(ids);
    }

    /**
     * 删除导出记录信息
     *
     * @param id 导出记录ID
     * @return 结果
     */
    @Override
    public int deleteExportLogById(Long id) {
        return exportLogMapper.deleteExportLogById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkExportLogUnique(ExportLog exportLog) {
        if (exportLog == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<ExportLog> list = exportLogMapper.selectExportLogList(exportLog);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

}
