package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.service.ICd90MachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("cd90")
public class Cd90Service {

    @Autowired
    private ICd90MachineInfoService iCd90MachineInfoService;

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<Cd90MachineInfo> getMachineInfo() {
        Cd90MachineInfo machineInfo=new Cd90MachineInfo();
        TableDataInfo info = iCd90MachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<Cd90MachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<Cd90MachineInfo> getMachineInfo(String a) {
        Cd90MachineInfo machineInfo=new Cd90MachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iCd90MachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<Cd90MachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     *
     * @return
     */
    public List<Cd90MachineInfo> getMachineInfo2(Cd90ScheduleResult cd90ScheduleResult, int a) {
        Cd90MachineInfo machineInfo = new Cd90MachineInfo();
        //定点机台信息
        if (a == 1) {
            machineInfo.setId(1L);
            machineInfo.setMachineCode(cd90ScheduleResult.getClothCode());
            List<Cd90MachineInfo> list1 = iCd90MachineInfoService.list2(machineInfo);
            return list1;
        } else if (a == 2) {
            //帘布大卷与机台映射表
            machineInfo.setId(2L);
            machineInfo.setMachineCode(cd90ScheduleResult.getClothCode());
            machineInfo.setMachineName(cd90ScheduleResult.getBigRollCode());
            List<Cd90MachineInfo> list2 = iCd90MachineInfoService.list2(machineInfo);
            return list2;
        } else if (a == 3) {
            //机台信息
            machineInfo.setId(3L);
            machineInfo.setMachineCode(cd90ScheduleResult.getClothCode());
            machineInfo.setMachineName(cd90ScheduleResult.getBigRollCode());
            List<Cd90MachineInfo> list3 = iCd90MachineInfoService.list2(machineInfo);
            return list3;
        }
        return null;
    }
}
