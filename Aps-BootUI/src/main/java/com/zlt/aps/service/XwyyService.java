package com.zlt.aps.service;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;
import com.zlt.aps.xwyy.api.service.IXwyyMachineInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("xwyy")
public class XwyyService {

    @Autowired
    private IXwyyMachineInfoService iXwyyMachineInfoService;

    /**
     * 机台下拉列表
     * @return
     */
    public List<XwyyMachineInfo> getMachineInfo() {
        XwyyMachineInfo machineInfo=new XwyyMachineInfo();
        TableDataInfo info = iXwyyMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<XwyyMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     * @return
     */
    public List<XwyyMachineInfo> getMachineInfo(String a) {
        XwyyMachineInfo machineInfo=new XwyyMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iXwyyMachineInfoService.list(machineInfo);
        if(StringUtils.isNull(info)){
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<XwyyMachineInfo>) info.getRows();
    }

    /**
     * 根据查询条件及标识查询机台信息
     * @param dto 查询条件
     * @param a 查询标识，1 查询定点机台，2 查询机台信息
     * @return 集合
     */
    public List<XwyyMachineInfo> getMachineInfo(XwyyScheduleResultDto dto, int a) {
        if (a == 1) {
            // 定点机台下拉列表
            XwyyMachineInfo machineInfo = new XwyyMachineInfo();
            // 设置查询标识查询定点机台
            machineInfo.setId(1L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getBigRollCode());
            return iXwyyMachineInfoService.listMachineInfo(machineInfo);
        } else if (a == 2) {
            // 帘布大卷和机台映射下拉列表
            XwyyMachineInfo machineInfo = new XwyyMachineInfo();
            // 设置查询标识查询定点机台
            machineInfo.setId(2L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getBigRollCode());
            return iXwyyMachineInfoService.listMachineInfo(machineInfo);
        } else if (a == 3) {
            // 所有机台下拉列表
            XwyyMachineInfo machineInfo = new XwyyMachineInfo();
            // 设置查询标识查询定点机台
            machineInfo.setId(3L);
            // 设置查询关联条件，只是用来装数据，并没有实际含义
            machineInfo.setMachineCode(dto.getBigRollCode());
            return iXwyyMachineInfoService.listMachineInfo(machineInfo);
        }
        return null;
    }
}
