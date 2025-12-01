package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.service.ITqMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("tq")
public class TqService {

    @Autowired
    private ITqMachineInfoService iTqMachineInfoService;

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<TqMachineInfo> getMachineInfo() {
        TqMachineInfo machineInfo=new TqMachineInfo();
        TableDataInfo info = iTqMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TqMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<TqMachineInfo> getMachineInfo(String a) {
        TqMachineInfo machineInfo=new TqMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iTqMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TqMachineInfo>) info.getRows();
    }

    /**
     * 所有机台下拉列表
     * @return 查询到的集合
     */
    public List<TqMachineInfo> getMachineInfo(TqScheduleResultDto dto) {
        TqMachineInfo machineInfo = new TqMachineInfo();
        machineInfo.setStatus("0");
        TableDataInfo info = iTqMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<TqMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表（根据tai）
     *
     * @return
     */
    public List<TqMachineInfo> getMachineInfo(TqScheduleResultDto dto, int a) {
        TqMachineInfo machineInfo = new TqMachineInfo();
        if (a == 1) {
            // 设置查询标识查询定点机台
            machineInfo.setId(1L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getBeadCode());
            return iTqMachineInfoService.listMachineInfo(machineInfo);
        } else if (a == 2) {
            //查询口型板机台
            machineInfo.setId(2L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getBeadCode());
            machineInfo.setMachineName(dto.getMouthPlateCode());
            return iTqMachineInfoService.listMachineInfo(machineInfo);
        } else if (a == 3) {
            //查询其他机台信息
            machineInfo.setId(3L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getBeadCode());
            machineInfo.setMachineName(dto.getMouthPlateCode());
            return iTqMachineInfoService.listMachineInfo(machineInfo);
        }
        return null;
    }
}
