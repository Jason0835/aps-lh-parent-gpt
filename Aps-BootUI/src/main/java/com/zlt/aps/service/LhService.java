package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.service.ILhMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("lh")
public class LhService {

    @Autowired
    private ILhMachineInfoService machineInfoService;

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<LhMachineInfo> getMachineInfo() {
        LhMachineInfo machineInfo=new LhMachineInfo();
        TableDataInfo info = machineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<LhMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<LhMachineInfo> getMachineInfo(String a) {
        LhMachineInfo machineInfo=new LhMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = machineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<LhMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     *
     * @return
     */
    public List<LhMachineInfo> getMachineInfo(LhScheduleResultDto dto, int a) {
        LhMachineInfo machineInfo = new LhMachineInfo();
        if (a == 1) {
            // 设置查询标识查询定点机台
            machineInfo.setId(1L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getSapCode());
            return machineInfoService.listMachineInfo(machineInfo);
        } else if (a == 2) {
            //查询其他机台
            machineInfo.setId(2L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getSapCode());
            return machineInfoService.listMachineInfo(machineInfo);
        }
        return null;
    }
}
