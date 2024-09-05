package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.tc.api.domain.dto.TcGlueGroupOrderDto;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.service.ITcGlueGroupOrderService;
import com.zlt.aps.tc.api.service.ITcMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("tc")
public class TcService {

    @Autowired
    private ITcMachineInfoService iTcMachineInfoService;
    @Resource
    private ITcGlueGroupOrderService iTcGlueGroupOrderService;

    /**
     * 机台下拉列表
     * @return
     */
    public List<TcMachineInfo> getMachineInfo() {
        TcMachineInfo machineInfo = new TcMachineInfo();
        TableDataInfo info = iTcMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TcMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     * @return
     */
    public List<TcMachineInfo> getMachineInfo(String a) {
        TcMachineInfo machineInfo = new TcMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iTcMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TcMachineInfo>) info.getRows();
    }

    /**
     * 胶料组列表
     * @return
     */
    public List<TcGlueGroupOrderDto> getGlueGroups() {
        TableDataInfo tableList = iTcGlueGroupOrderService.listGlueGroupOrder(new TcGlueGroupOrderDto());
        return (List<TcGlueGroupOrderDto>)tableList.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     * @return
     */
    public List<TcMachineInfo> getMachineInfo2(TcScheduleResult tcScheduleResult, int a) {
        TcMachineInfo machineInfo = new TcMachineInfo();
        //查询定点机台信息
        if(a==1){
            machineInfo.setId(1L);
            machineInfo.setMachineCode(tcScheduleResult.getSidewallCode());
            List<TcMachineInfo> list1 = iTcMachineInfoService.list2(machineInfo);
            return list1;
        }else if(a==2){
            //查询口型板机台
            machineInfo.setId(2L);
            machineInfo.setMachineCode(tcScheduleResult.getSidewallCode());
            machineInfo.setMachineName(tcScheduleResult.getMouthPlateCode());
            List<TcMachineInfo> list2 = iTcMachineInfoService.list2(machineInfo);
            return list2;
        }else if(a==3){
            //查询机台信息
            machineInfo.setId(3L);
            machineInfo.setMachineCode(tcScheduleResult.getSidewallCode());
            machineInfo.setMachineName(tcScheduleResult.getMouthPlateCode());
            List<TcMachineInfo> list3 = iTcMachineInfoService.list2(machineInfo);
            return list3;
        }
        return null;
    }
}
