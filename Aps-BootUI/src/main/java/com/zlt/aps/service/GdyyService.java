package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import com.zlt.aps.gdyy.api.service.IGdyyMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2025/2/20 15:24
 */
@Service("gdyy")
public class GdyyService {

    @Autowired
    private IGdyyMachineInfoService iGdyyMachineInfoService;

    /**
     * 机台下拉列表
     * @return
     */
    public List<GdyyMachineInfo> getMachineInfo() {
        GdyyMachineInfo machineInfo=new GdyyMachineInfo();
        TableDataInfo info = iGdyyMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<GdyyMachineInfo>) info.getRows();
    }
}
