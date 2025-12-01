package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmConstructionInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmConstructionInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import io.seata.common.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmConstructionInfoServiceImpl.java
 * 描    述：MdmConstructionInfoServiceImpl投产胎胚施工信息业务层处理
 *@author zlt
 *@date 2025-02-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmConstructionInfoServiceImpl extends AbstractDocService<MdmConstructionInfo>  implements IMdmConstructionInfoService {

    @Autowired
    private MdmConstructionInfoEntityMapper mdmConstructionInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0106";
    }

    @Override
    public AjaxResult importData(List<MdmConstructionInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmConstructionInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmConstructionInfo constructionInfo = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, constructionInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                constructionInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else if (updateSupport) {
                constructionInfo.setBaseVale(null);
                importList.add(constructionInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport) {
                // 无覆盖情况
//                successNum = importList.size();
//                this.mergerIntoBatchData(importList);
            } else {
                List<MdmConstructionInfo>  insertList=new ArrayList<>();
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MdmConstructionInfo constructionInfo = list.get(i);
                    // 错误记录跳过
                    if (constructionInfo.getId() != null && constructionInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkConstructionInfoUnique(constructionInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        constructionInfo.setBaseVale(null);
                        insertList.add(constructionInfo);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                "", importErrorLogs);
                    }
                }
                if(CollectionUtils.isNotEmpty(insertList)){
                    baseDao.insertBatch(insertList);
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
     * 校验生产施工表唯一性
     */
    public String checkConstructionInfoUnique(MdmConstructionInfo constructionInfo) {
        if (constructionInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<MdmConstructionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmConstructionInfo::getConstructionCode, constructionInfo.getConstructionCode());
        wrapper.eq(MdmConstructionInfo::getMouldMethod, constructionInfo.getMouldMethod());
        List<MdmConstructionInfo> list = mdmConstructionInfoEntityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        List<String> fields = new ArrayList<>();
        fields.add("constructionCode");
        fields.add("mouldMethod");
        return fields;
    }
}
