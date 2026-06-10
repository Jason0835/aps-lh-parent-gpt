package com.zlt.aps.tm.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmGlueGroupOrder;
import com.zlt.aps.tm.mapper.TmGlueGroupOrderMapper;
import com.zlt.aps.tm.service.ITmGlueGroupOrderService;
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
public class TmGlueGroupOrderServiceImpl extends AbstractDocService<TmGlueGroupOrder> implements ITmGlueGroupOrderService {

    @Resource
    private TmGlueGroupOrderMapper tmGlueGroupOrderMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0808";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0808");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmGlueGroupOrder query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.glueGroupOrder.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "glueGroupCode"));
    }
}
