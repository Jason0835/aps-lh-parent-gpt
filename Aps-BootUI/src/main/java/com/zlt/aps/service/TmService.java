package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.service.ITmGlueGroupOrderService;
import com.zlt.aps.tm.api.service.ITmMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("tm")
public class TmService {

    @Autowired
    private ITmMachineInfoService iTmMachineInfoService;
    @Resource
    private ITmGlueGroupOrderService iTmGlueGroupOrderService;

    /**
     * 机台下拉列表
     * @return
     */
    public List<TmMachineInfo> getMachineInfo() {
        TmMachineInfo machineInfo = new TmMachineInfo();
        TableDataInfo info = iTmMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TmMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     * @return
     */
    public List<TmMachineInfo> getMachineInfo(String a) {
        TmMachineInfo machineInfo = new TmMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iTmMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TmMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     * @return
     */
    public List<TmMachineInfo> getMachineInfo2(TmScheduleResult tmScheduleResult,int a) {
        TmMachineInfo machineInfo = new TmMachineInfo();
        //查询定点机台信息
        if(a==1){
            machineInfo.setId(1L);
            machineInfo.setMachineCode(tmScheduleResult.getTreadCode());
            List<TmMachineInfo> list1 = iTmMachineInfoService.list2(machineInfo);
            return list1;
        }else if(a==2){
            //查询口型板机台
            machineInfo.setId(2L);
            machineInfo.setMachineCode(tmScheduleResult.getTreadCode());
            machineInfo.setMachineName(tmScheduleResult.getMouthPlateCode());
            List<TmMachineInfo> list2 = iTmMachineInfoService.list2(machineInfo);
            return list2;
        }else if(a==3){
            //查询机台信息
            machineInfo.setId(3L);
            machineInfo.setMachineCode(tmScheduleResult.getTreadCode());
            machineInfo.setMachineName(tmScheduleResult.getMouthPlateCode());
            List<TmMachineInfo> list3 = iTmMachineInfoService.list2(machineInfo);
            return list3;
        }
        return null;
    }

    /**
     * 胶料组列表
     * @return
     */
    public List<TmGlueGroupOrderDto> getGlueGroups() {
        TableDataInfo tableList = iTmGlueGroupOrderService.listGlueGroupOrder(new TmGlueGroupOrderDto());
        return (List<TmGlueGroupOrderDto>)tableList.getRows();
    }
}
