package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxScheduleStopInfo;
import com.zlt.aps.cx.mapper.CxScheduleStopInfoMapper;
import com.zlt.aps.cx.service.CxScheduleStopInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型机台自动停排信息Service业务层处理
 *
 * @author chen
 * @date 2022-04-03
 */
@Service
public class CxScheduleStopInfoServiceImpl implements CxScheduleStopInfoService {
    @Autowired
    private CxScheduleStopInfoMapper cxScheduleStopInfoMapper;

    /**
     * 查询成型机台自动停排信息
     *
     * @param id 成型机台自动停排信息ID
     * @return 成型机台自动停排信息
     */
    @Override
    public CxScheduleStopInfo selectCxScheduleStopInfoById(Long id) {
        return cxScheduleStopInfoMapper.selectCxScheduleStopInfoById(id);
    }

    /**
     * 查询成型机台自动停排信息列表
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 成型机台自动停排信息
     */
    @Override
    public List<CxScheduleStopInfo> selectCxScheduleStopInfoList(CxScheduleStopInfo cxScheduleStopInfo) {
        return cxScheduleStopInfoMapper.selectCxScheduleStopInfoList(cxScheduleStopInfo);
    }

    /**
     * 新增成型机台自动停排信息
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 结果
     */
    @Override
    public int insertCxScheduleStopInfo(CxScheduleStopInfo cxScheduleStopInfo) {
        cxScheduleStopInfo.setBaseVale(null);
        return cxScheduleStopInfoMapper.insertCxScheduleStopInfo(cxScheduleStopInfo);
    }

    /**
     * 修改成型机台自动停排信息
     *
     * @param cxScheduleStopInfo 成型机台自动停排信息
     * @return 结果
     */
    @Override
    public int updateCxScheduleStopInfo(CxScheduleStopInfo cxScheduleStopInfo) {
        cxScheduleStopInfo.setBaseVale(cxScheduleStopInfo.getId());
        return cxScheduleStopInfoMapper.updateCxScheduleStopInfo(cxScheduleStopInfo);
    }

    /**
     * 批量删除成型机台自动停排信息
     *
     * @param ids 需要删除的成型机台自动停排信息ID
     * @return 结果
     */
    @Override
    public int deleteCxScheduleStopInfoByIds(Long[] ids) {
        return cxScheduleStopInfoMapper.deleteCxScheduleStopInfoByIds(ids);
    }

    /**
     * 删除成型机台自动停排信息信息
     *
     * @param id 成型机台自动停排信息ID
     * @return 结果
     */
    @Override
    public int deleteCxScheduleStopInfoById(Long id) {
        return cxScheduleStopInfoMapper.deleteCxScheduleStopInfoById(id);
    }

    /**
     * 校验成型机台自动停排信息唯一性
     */
    @Override
    public String checkCxScheduleStopInfoUnique(CxScheduleStopInfo cxScheduleStopInfo) {
        if (cxScheduleStopInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxScheduleStopInfo> list = cxScheduleStopInfoMapper.selectCxScheduleStopInfoList(cxScheduleStopInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型机台自动停排信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxScheduleStopInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxScheduleStopInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleStopInfo cxScheduleStopInfo = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxScheduleStopInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxScheduleStopInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                cxScheduleStopInfo.setBaseVale(null);
                importList.add(cxScheduleStopInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                cxScheduleStopInfoMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxScheduleStopInfo cxScheduleStopInfo = list.get(i);
                    // 错误记录跳过
                    if (cxScheduleStopInfo.getId() != null && cxScheduleStopInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxScheduleStopInfoUnique(cxScheduleStopInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxScheduleStopInfo(cxScheduleStopInfo);
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
