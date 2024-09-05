package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcGlueGroupOrderDto;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.api.service.INcGlueGroupOrderService;
import com.zlt.aps.nc.api.service.INcMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("nc")
public class NcService {

    @Autowired
    private INcMachineInfoService iNcMachineInfoService;
    @Resource
    private INcGlueGroupOrderService iNcGlueGroupOrderService;

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<NcMachineInfo> getMachineInfo() {
        NcMachineInfo machineInfo=new NcMachineInfo();
        TableDataInfo info = iNcMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<NcMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<NcMachineInfo> getMachineInfo(String a) {
        NcMachineInfo machineInfo=new NcMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iNcMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<NcMachineInfo>) info.getRows();
    }

    /**
     * 胶料组列表
     *
     * @return
     */
    public List<NcGlueGroupOrderDto> getGlueGroups() {
        TableDataInfo tableList = iNcGlueGroupOrderService.listGlueGroupOrder(new NcGlueGroupOrderDto());
        return (List<NcGlueGroupOrderDto>) tableList.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     *
     * @return
     */
    public List<NcMachineInfo> getMachineInfo2(NcScheduleResult tcScheduleResult, int a) {
        NcMachineInfo machineInfo = new NcMachineInfo();
        //查询定点机台信息
        if (a == 1) {
            machineInfo.setId(1L);
            machineInfo.setMachineCode(tcScheduleResult.getLiningCode());
            List<NcMachineInfo> list1 = iNcMachineInfoService.list2(machineInfo);
            return list1;
        } else if (a == 2) {
            //查询口型板机台
            machineInfo.setId(2L);
            machineInfo.setMachineCode(tcScheduleResult.getLiningCode());
            machineInfo.setMachineName(tcScheduleResult.getMouthPlateCode());
            List<NcMachineInfo> list2 = iNcMachineInfoService.list2(machineInfo);
            return list2;
        } else if (a == 3) {
            //查询机台信息
            machineInfo.setId(3L);
            machineInfo.setMachineCode(tcScheduleResult.getLiningCode());
            List<NcMachineInfo> list3 = iNcMachineInfoService.list2(machineInfo);
            return list3;
        }
        return null;
    }
}
