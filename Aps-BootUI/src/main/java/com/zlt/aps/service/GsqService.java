package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.service.IGsqMachineInfoService;
import com.zlt.aps.gsq.api.service.IGsqSpecifyMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("gsq")
public class GsqService {

    @Autowired
    private IGsqMachineInfoService iGsqMachineInfoService;
    @Autowired
    private IGsqSpecifyMachineService iGsqSpecifyMachineService;

    /**
     * 机台下拉列表
     * @return
     */
    public List<GsqMachineInfo> getMachineInfo() {
        GsqMachineInfo machineInfo=new GsqMachineInfo();
        TableDataInfo info = iGsqMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<GsqMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     * @return
     */
    public List<GsqMachineInfo> getMachineInfo(String a) {
        GsqMachineInfo machineInfo=new GsqMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iGsqMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<GsqMachineInfo>) info.getRows();
    }

    /**
     * 所有机台下拉列表
     * @return 查询到的集合
     */
    public List<GsqMachineInfo> getMachineInfo(GsqScheduleResultDto dto) {
        GsqMachineInfo machineInfo = new GsqMachineInfo();
        machineInfo.setStatus("0");
        TableDataInfo info = iGsqMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<GsqMachineInfo>) info.getRows();
    }

    /**
     * 根据查询条件及标识查询机台信息
     * @param dto 查询条件
     * @param a 查询标识，1 查询定点机台，2 查询机台信息
     * @return 集合
     */
    public List<GsqMachineInfo> getMachineInfo(GsqScheduleResultDto dto, int a) {
        if (a == 1) {
            // 定点机台下拉列表
            GsqMachineInfo machineInfo = new GsqMachineInfo();
            // 设置查询标识查询定点机台
            machineInfo.setId(1L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getSteelRingCode());
            return iGsqMachineInfoService.listMachineInfo(machineInfo);
        } else if (a == 2) {
            // 所有机台下拉列表
            GsqMachineInfo machineInfo = new GsqMachineInfo();
            // 设置查询标识查询定点机台
            machineInfo.setId(2L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getSteelRingCode());
            return iGsqMachineInfoService.listMachineInfo(machineInfo);
        }
        return null;
    }
}
