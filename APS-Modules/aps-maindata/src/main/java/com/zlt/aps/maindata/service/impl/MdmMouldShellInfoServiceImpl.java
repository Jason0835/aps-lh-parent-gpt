package com.zlt.aps.maindata.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.maindata.service.IMdmMouldShellInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldShellInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMouldShellInfoServiceImpl.java
 * 描    述：MpMouldShellInfoServiceImpl模壳台账业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMouldShellInfoServiceImpl extends AbstractDocService<MdmMouldShellInfo> implements IMdmMouldShellInfoService {

    @Autowired
    private IMesItfService mesItfService;

    @Override
    protected String getDocTypeCode() {
        return "MP0208";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0208");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMouldShellInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpMouldShellInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "moldModelCode"));
    }

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @Override
    public AjaxResult mesCapture() {
        // Steve's TODO 待确认抓取模壳信息版本号取值
        AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
//        syncDataLogs.setDataVersion("");
        mesItfService.syncMoldShell(syncDataLogs);
        return AjaxResult.success();
    }
}
