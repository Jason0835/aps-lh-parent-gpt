package com.zlt.aps.xwyy.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;
import com.zlt.aps.xwyy.mapper.XwyyBigRollRubberCarRelationMapper;
import com.zlt.aps.xwyy.service.XwyyBigRollRubberCarRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
  *
  * @ClassName XwyyBigRollRubberCarRelationServiceImpl
  * @Description 纤维压延大卷胶料号车数关系逻辑实现类
  * @Author Joran.Zhang
  * @Date 2022/5/10 10:05
  * @Version 1.0
**/
@Service("xwyyBigRollRubberCarRelationService")
public class XwyyBigRollRubberCarRelationServiceImpl implements XwyyBigRollRubberCarRelationService {

    @Autowired
    private XwyyBigRollRubberCarRelationMapper xwyyBigRollRubberCarRelationMapper;

    @Override
    public XwyyBigRollRubberCarRelation selectXwyyBigRollRubberCarRelationById(Long id) {
        return xwyyBigRollRubberCarRelationMapper.selectXwyyBigRollRubberCarRelationById(id);
    }

    @Override
    public List<XwyyBigRollRubberCarRelation> selectXwyyBigRollRubberCarRelationList(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        return xwyyBigRollRubberCarRelationMapper.selectXwyyBigRollRubberCarRelationList(xwyyBigRollRubberCarRelation);
    }

    @Override
    public int insertXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        if (UserConstants.NOT_UNIQUE.equals(checkXwyyBigRollRubberCarRelationUnique(xwyyBigRollRubberCarRelation))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.carRelation.datebaseUnique"));
        }
        xwyyBigRollRubberCarRelation.setBaseVale(null);
        return xwyyBigRollRubberCarRelationMapper.insertXwyyBigRollRubberCarRelation(xwyyBigRollRubberCarRelation);
    }

    @Override
    public int updateXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        if (UserConstants.NOT_UNIQUE.equals(checkXwyyBigRollRubberCarRelationUnique(xwyyBigRollRubberCarRelation))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.carRelation.datebaseUnique"));
        }
        xwyyBigRollRubberCarRelation.setBaseVale(xwyyBigRollRubberCarRelation.getId());
        return xwyyBigRollRubberCarRelationMapper.updateXwyyBigRollRubberCarRelation(xwyyBigRollRubberCarRelation);
    }

    @Override
    public int deleteXwyyBigRollRubberCarRelationByIds(Long[] ids) {
        return xwyyBigRollRubberCarRelationMapper.deleteXwyyBigRollRubberCarRelationByIds(ids);
    }

    @Override
    public int deleteXwyyBigRollRubberCarRelationById(Long id) {
        return xwyyBigRollRubberCarRelationMapper.deleteXwyyBigRollRubberCarRelationById(id);
    }

    @Override
    public String checkXwyyBigRollRubberCarRelationUnique(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        if (xwyyBigRollRubberCarRelation == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int unique = xwyyBigRollRubberCarRelationMapper.checkXwyyBigRollRubberCarRelationUnique(xwyyBigRollRubberCarRelation);
        if (unique > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<XwyyBigRollRubberCarRelation> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<XwyyBigRollRubberCarRelation> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(XwyyBigRollRubberCarRelation::getBigRollCode, Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(XwyyBigRollRubberCarRelation.getBigRollCode());
            if (hasValue > 1) {
                failureNum++;
                XwyyBigRollRubberCarRelation.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.carRelation.bigRollCode");
                message=String.format(message, columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, XwyyBigRollRubberCarRelation);
            if (CollectionUtils.isNotEmpty(validated)) {
                XwyyBigRollRubberCarRelation.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                XwyyBigRollRubberCarRelation.setBaseVale(null);
                importList.add(XwyyBigRollRubberCarRelation);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                xwyyBigRollRubberCarRelationMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    XwyyBigRollRubberCarRelation XwyyBigRollRubberCarRelation = list.get(i);
                    // 错误记录跳过
                    if (XwyyBigRollRubberCarRelation.getId() != null && XwyyBigRollRubberCarRelation.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkXwyyBigRollRubberCarRelationUnique(XwyyBigRollRubberCarRelation);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertXwyyBigRollRubberCarRelation(XwyyBigRollRubberCarRelation);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.message.carRelation.datebaseUnique"), importErrorLogs);
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

    @Override
    public XwyyBigRollRubberCarRelation selectByBigRollCode(XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        return xwyyBigRollRubberCarRelationMapper.selectByBigRollCode(xwyyBigRollRubberCarRelation);
    }
}
