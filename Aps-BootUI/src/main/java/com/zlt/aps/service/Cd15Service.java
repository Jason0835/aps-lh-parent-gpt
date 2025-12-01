package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.service.ICd15MachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("cd15")
public class Cd15Service {

    @Autowired
    private ICd15MachineInfoService iCd15MachineInfoService;

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<Cd15MachineInfo> getMachineInfo(String a) {
        Cd15MachineInfo machineInfo= new Cd15MachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iCd15MachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<Cd15MachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<Cd15MachineInfo> getMachineInfo() {
        Cd15MachineInfo machineInfo= new Cd15MachineInfo();
        TableDataInfo info = iCd15MachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<Cd15MachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     *
     * @return
     */
    public List<Cd15MachineInfo> getMachineInfo2(Cd15ScheduleResult cd15ScheduleResult, int a) {
        Cd15MachineInfo machineInfo = new Cd15MachineInfo();
        //查询定点机台信息
        if (a == 1) {
            machineInfo.setId(1L);
            machineInfo.setMachineCode(cd15ScheduleResult.getSteelStripCode1());
            List<Cd15MachineInfo> list1 = iCd15MachineInfoService.list2(machineInfo);
            return list1;
        } else if (a == 2) {
            //查询钢压大卷与机台映射表
            machineInfo.setId(2L);
            machineInfo.setMachineCode(cd15ScheduleResult.getSteelStripCode1());
            machineInfo.setMachineName(cd15ScheduleResult.getBigRollCode());
            List<Cd15MachineInfo> list2 = iCd15MachineInfoService.list2(machineInfo);
            return list2;
        } else if (a == 3) {
            //查询机台信息
            machineInfo.setId(3L);
            machineInfo.setMachineCode(cd15ScheduleResult.getSteelStripCode1());
            machineInfo.setMachineName(cd15ScheduleResult.getBigRollCode());
            List<Cd15MachineInfo> list3 = iCd15MachineInfoService.list2(machineInfo);
            return list3;
        }
        return null;
    }
}
