package com.zlt.aps.tc.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcGlueGroupOrder;
import com.zlt.aps.tc.mapper.TcGlueGroupOrderMapper;
import com.zlt.aps.tc.service.ITcGlueGroupOrderService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcGlueGroupOrderServiceImpl extends AbstractDocService<TcGlueGroupOrder> implements ITcGlueGroupOrderService {

    @Resource
    private TcGlueGroupOrderMapper tcGlueGroupOrderMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0908";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0908");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcGlueGroupOrder query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.glueGroupOrder.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "glueGroupCode"));
    }
}