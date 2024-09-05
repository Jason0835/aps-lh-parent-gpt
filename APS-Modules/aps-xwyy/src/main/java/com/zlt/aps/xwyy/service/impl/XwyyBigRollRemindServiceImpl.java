package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;
import com.zlt.aps.xwyy.mapper.XwyyBigRollRemindMapper;
import com.zlt.aps.xwyy.service.XwyyBigRollRemindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 帘布大卷原线提醒Service业务层处理
 *
 * @author chen
 * @date 2022-04-27
 */
@Service
public class XwyyBigRollRemindServiceImpl implements XwyyBigRollRemindService {
    @Autowired
    private XwyyBigRollRemindMapper xwyyBigRollRemindMapper;

    /**
     * 查询帘布大卷原线提醒
     *
     * @param id 帘布大卷原线提醒ID
     * @return 帘布大卷原线提醒
     */
    @Override
    public XwyyBigRollRemind selectXwyyBigRollRemindById(Long id) {
        return xwyyBigRollRemindMapper.selectXwyyBigRollRemindById(id);
    }

    /**
     * 查询帘布大卷原线提醒列表
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 帘布大卷原线提醒
     */
    @Override
    public List<XwyyBigRollRemind> selectXwyyBigRollRemindList(XwyyBigRollRemind xwyyBigRollRemind) {
        return xwyyBigRollRemindMapper.selectXwyyBigRollRemindList(xwyyBigRollRemind);
    }

    /**
     * 新增帘布大卷原线提醒
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 结果
     */
    @Override
    public int insertXwyyBigRollRemind(XwyyBigRollRemind xwyyBigRollRemind) {
        if (UserConstants.NOT_UNIQUE.equals(checkXwyyBigRollRemindUnique(xwyyBigRollRemind))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.bigRollRemind.datebaseUnique"));
        }
        xwyyBigRollRemind.setBaseVale(null);
        return xwyyBigRollRemindMapper.insertXwyyBigRollRemind(xwyyBigRollRemind);
    }

    /**
     * 修改帘布大卷原线提醒
     *
     * @param xwyyBigRollRemind 帘布大卷原线提醒
     * @return 结果
     */
    @Override
    public int updateXwyyBigRollRemind(XwyyBigRollRemind xwyyBigRollRemind) {
        if (UserConstants.NOT_UNIQUE.equals(checkXwyyBigRollRemindUnique(xwyyBigRollRemind))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.bigRollRemind.datebaseUnique"));
        }
        xwyyBigRollRemind.setBaseVale(xwyyBigRollRemind.getId());
        return xwyyBigRollRemindMapper.updateXwyyBigRollRemind(xwyyBigRollRemind);
    }

    /**
     * 批量删除帘布大卷原线提醒
     *
     * @param ids 需要删除的帘布大卷原线提醒ID
     * @return 结果
     */
    @Override
    public int deleteXwyyBigRollRemindByIds(Long[] ids) {
        return xwyyBigRollRemindMapper.deleteXwyyBigRollRemindByIds(ids);
    }

    /**
     * 删除帘布大卷原线提醒信息
     *
     * @param id 帘布大卷原线提醒ID
     * @return 结果
     */
    @Override
    public int deleteXwyyBigRollRemindById(Long id) {
        return xwyyBigRollRemindMapper.deleteXwyyBigRollRemindById(id);
    }

    /**
     * 校验帘布大卷原线提醒唯一性
     */
    @Override
    public String checkXwyyBigRollRemindUnique(XwyyBigRollRemind xwyyBigRollRemind) {
        if (xwyyBigRollRemind == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int unique = xwyyBigRollRemindMapper.checkXwyyBigRollRemindUnique(xwyyBigRollRemind);
        if (unique > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入帘布大卷原线提醒数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<XwyyBigRollRemind> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyBigRollRemind> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(XwyyBigRollRemind::getBigRollCode, Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            XwyyBigRollRemind xwyyBigRollRemind = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(xwyyBigRollRemind.getBigRollCode());
            if (hasValue > 1) {
                failureNum++;
                xwyyBigRollRemind.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.bigRollRemind.bigRollCode");
                message=String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, xwyyBigRollRemind);
            if (CollectionUtils.isNotEmpty(validated)) {
                xwyyBigRollRemind.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                xwyyBigRollRemind.setBaseVale(null);
                importList.add(xwyyBigRollRemind);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                xwyyBigRollRemindMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    XwyyBigRollRemind xwyyBigRollRemind = list.get(i);
                    // 错误记录跳过
                    if (xwyyBigRollRemind.getId() != null && xwyyBigRollRemind.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkXwyyBigRollRemindUnique(xwyyBigRollRemind);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertXwyyBigRollRemind(xwyyBigRollRemind);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.message.bigRollRemind.datebaseUnique"), importErrorLogs);
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
