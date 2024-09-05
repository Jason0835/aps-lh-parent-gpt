package com.zlt.aps.cx.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.mapper.ConstructionExportLogMapper;
import com.zlt.aps.cx.api.domain.entity.ConstructionExportLog;
import com.zlt.aps.cx.service.ConstructionExportLogService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 施工信息导出日志Service业务层处理
 * 
 * @author zlt
 * @date 2021-12-28
 */
@Service
public class ConstructionExportLogServiceImpl implements ConstructionExportLogService
{
    @Autowired
    private ConstructionExportLogMapper constructionExportLogMapper;

    /**
     * 查询施工信息导出日志
     * 
     * @param id 施工信息导出日志ID
     * @return 施工信息导出日志
     */
    @Override
    public ConstructionExportLog selectConstructionExportLogById(Long id)
    {
        return constructionExportLogMapper.selectConstructionExportLogById(id);
    }

    /**
     * 查询施工信息导出日志列表
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 施工信息导出日志
     */
    @Override
    public List<ConstructionExportLog> selectConstructionExportLogList(ConstructionExportLog constructionExportLog)
    {
        return constructionExportLogMapper.selectConstructionExportLogList(constructionExportLog);
    }

    /**
     * 新增施工信息导出日志
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 结果
     */
    @Override
    public int insertConstructionExportLog(ConstructionExportLog constructionExportLog)
    {
        constructionExportLog.setBaseVale(null);
        return constructionExportLogMapper.insertConstructionExportLog(constructionExportLog);
    }

    /**
     * 修改施工信息导出日志
     * 
     * @param constructionExportLog 施工信息导出日志
     * @return 结果
     */
    @Override
    public int updateConstructionExportLog(ConstructionExportLog constructionExportLog)
    {
        constructionExportLog.setBaseVale(constructionExportLog.getId());
        return constructionExportLogMapper.updateConstructionExportLog(constructionExportLog);
    }

    /**
     * 批量删除施工信息导出日志
     * 
     * @param ids 需要删除的施工信息导出日志ID
     * @return 结果
     */
    @Override
    public int deleteConstructionExportLogByIds(Long[] ids)
    {
        return constructionExportLogMapper.deleteConstructionExportLogByIds(ids);
    }

    /**
     * 删除施工信息导出日志信息
     * 
     * @param id 施工信息导出日志ID
     * @return 结果
     */
    @Override
    public int deleteConstructionExportLogById(Long id)
    {
        return constructionExportLogMapper.deleteConstructionExportLogById(id);
    }

    /**
     * 校验施工信息导出日志唯一性
     */
    @Override
    public String checkConstructionExportLogUnique(ConstructionExportLog constructionExportLog) {
        if (constructionExportLog == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<ConstructionExportLog> list = constructionExportLogMapper.selectConstructionExportLogList(constructionExportLog);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入施工信息导出日志数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<ConstructionExportLog> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<ConstructionExportLog> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            ConstructionExportLog constructionExportLog = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, constructionExportLog);
            if (CollectionUtils.isNotEmpty(validated)) {
                constructionExportLog.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                constructionExportLog.setBaseVale(null);
                importList.add(constructionExportLog);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    constructionExportLogMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    ConstructionExportLog constructionExportLog = list.get(i);
                    // 错误记录跳过
                    if (constructionExportLog.getId() != null && constructionExportLog.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkConstructionExportLogUnique(constructionExportLog);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertConstructionExportLog(constructionExportLog);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
