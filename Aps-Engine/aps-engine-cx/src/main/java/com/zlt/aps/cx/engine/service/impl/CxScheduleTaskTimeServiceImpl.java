package com.zlt.aps.cx.engine.service.impl;

import java.util.Date;
import java.util.List;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleTaskTime;
import com.zlt.aps.cx.engine.exception.CxScheduleEngineException;
import com.zlt.aps.cx.engine.mapper.CxScheduleTaskTimeMapper;
import com.zlt.aps.cx.engine.service.CxScheduleTaskTimeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import org.springframework.transaction.annotation.Transactional;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型排程任务时间Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2022-05-17
 */
@Service
@Slf4j
public class CxScheduleTaskTimeServiceImpl implements CxScheduleTaskTimeService
{
    @Autowired
    private CxScheduleTaskTimeMapper cxScheduleTaskTimeMapper;

    /**
     * 查询成型排程任务时间
     * 
     * @param id 成型排程任务时间ID
     * @return 成型排程任务时间
     */
    @Override
    public CxScheduleTaskTime selectCxScheduleTaskTimeById(Long id)
    {
        return cxScheduleTaskTimeMapper.selectCxScheduleTaskTimeById(id);
    }

    /**
     * 查询成型排程任务时间列表
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 成型排程任务时间
     */
    @Override
    public List<CxScheduleTaskTime> selectCxScheduleTaskTimeList(CxScheduleTaskTime cxScheduleTaskTime)
    {
        return cxScheduleTaskTimeMapper.selectCxScheduleTaskTimeList(cxScheduleTaskTime);
    }

    /**
     * 新增成型排程任务时间
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 结果
     */
    @Override
    public int insertCxScheduleTaskTime(CxScheduleTaskTime cxScheduleTaskTime)
    {
        cxScheduleTaskTime.setBaseVale(null);
        return cxScheduleTaskTimeMapper.insertCxScheduleTaskTime(cxScheduleTaskTime);
    }

    /**
     * 批量新增成型任务时间
     * @param cxScheduleTaskTimeList
     * @return
     */
    @Override
    public int batchInsertCxScheduleTaskTime(List<CxScheduleTaskTime> cxScheduleTaskTimeList) {
        return cxScheduleTaskTimeMapper.batchInsertCxScheduleTaskTime(cxScheduleTaskTimeList);
    }

    /**
     * 修改成型排程任务时间
     * 
     * @param cxScheduleTaskTime 成型排程任务时间
     * @return 结果
     */
    @Override
    public int updateCxScheduleTaskTime(CxScheduleTaskTime cxScheduleTaskTime)
    {
        cxScheduleTaskTime.setBaseVale(cxScheduleTaskTime.getId());
        return cxScheduleTaskTimeMapper.updateCxScheduleTaskTime(cxScheduleTaskTime);
    }

    /**
     * 批量删除成型排程任务时间
     * 
     * @param ids 需要删除的成型排程任务时间ID
     * @return 结果
     */
    @Override
    public int deleteCxScheduleTaskTimeByIds(Long[] ids)
    {
        return cxScheduleTaskTimeMapper.deleteCxScheduleTaskTimeByIds(ids);
    }

    /**
     * 删除成型排程任务时间信息
     * 
     * @param id 成型排程任务时间ID
     * @return 结果
     */
    @Override
    public int deleteCxScheduleTaskTimeById(Long id)
    {
        return cxScheduleTaskTimeMapper.deleteCxScheduleTaskTimeById(id);
    }

    /**
     * 校验成型排程任务时间唯一性
     */
    @Override
    public String checkCxScheduleTaskTimeUnique(CxScheduleTaskTime cxScheduleTaskTime) {
        if (cxScheduleTaskTime == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxScheduleTaskTime> list = cxScheduleTaskTimeMapper.selectCxScheduleTaskTimeList(cxScheduleTaskTime);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型排程任务时间数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxScheduleTaskTime> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxScheduleTaskTime> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleTaskTime cxScheduleTaskTime = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxScheduleTaskTime);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxScheduleTaskTime.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                cxScheduleTaskTime.setBaseVale(null);
                importList.add(cxScheduleTaskTime);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    cxScheduleTaskTimeMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxScheduleTaskTime cxScheduleTaskTime = list.get(i);
                    // 错误记录跳过
                    if (cxScheduleTaskTime.getId() != null && cxScheduleTaskTime.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxScheduleTaskTimeUnique(cxScheduleTaskTime);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxScheduleTaskTime(cxScheduleTaskTime);
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

    /**
     * 根据排程日期进行任务时间删除
     * @param scheduleDate yyyyMMdd
     * @return
     */
    @Override
    public int deleteCxScheduleTaskTimeByScheduleDate(Date scheduleDate) {
        if(scheduleDate==null){
            if(scheduleDate==null){
                log.warn("排程日期为空，排程日期为当前日期，传入日期{}",scheduleDate);
                scheduleDate=new Date();
            }
        }
        String scheduleDateStr= DateUtils.parseDateToStr("yyyyMMdd",scheduleDate);
        return cxScheduleTaskTimeMapper.deleteCxScheduleTaskTimeByScheduleDate(scheduleDateStr);
    }
}
