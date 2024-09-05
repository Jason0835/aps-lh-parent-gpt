package com.zlt.aps.service;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.service.ICxMachineInfoService;
import com.zlt.aps.cx.api.service.ICxMatchingSpecifyMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Chen 2021/5/28 15:24
 */
@Service("cx")
public class CxService {

    @Autowired
    private ICxMachineInfoService iCxMachineInfoService;

    @Autowired
    private ICxMatchingSpecifyMachineService iCxMatchingSpecifyMachineService;

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<CxMachineInfo> getMachineInfo() {
        CxMachineInfo machineInfo=new CxMachineInfo();
        TableDataInfo info = iCxMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<CxMachineInfo>) info.getRows();
    }


    public List<CxMachineInfo> getMachineInfoOrderByName() {
        CxMachineInfo machineInfo=new CxMachineInfo();
        List<CxMachineInfo> list = iCxMachineInfoService.listOrderByName(machineInfo);
        if (CollectionUtils.isEmpty(list)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return list;
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<CxMachineInfo> getMachineInfo(String a) {
        CxMachineInfo machineInfo=new CxMachineInfo();
        machineInfo.setStatus(a);
        TableDataInfo info = iCxMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<CxMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<CxMachineInfo> getMachineInfo(String a,String embryoCode) {
        CxMachineInfo machineInfo=new CxMachineInfo();
        machineInfo.setStatus(a);
        machineInfo.setMachineType(embryoCode.startsWith("Y") ? "1":"2");
        TableDataInfo info = iCxMachineInfoService.list(machineInfo);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.mouthPlate.message.getMachineInfo"));
        }
        return (List<CxMachineInfo>) info.getRows();
    }

    /**
     * 机台下拉列表
     *
     * @return
     */
    public List<CxMachineInfo> getMachineInfo2(CxScheduleResult cxScheduleResult, int a) {
        CxMachineInfo machineInfo = new CxMachineInfo();
        //成型定点机台信息
        if (a == 1) {
            machineInfo.setId(1L);
            machineInfo.setMachineCode(cxScheduleResult.getEmbryoCode());
            machineInfo.setMachineName(cxScheduleResult.getSapCode());
            machineInfo.setMachineType(cxScheduleResult.getEmbryoCode().startsWith("Y") ? "1":"2");
            List<CxMachineInfo> list1 = iCxMachineInfoService.list2(machineInfo);
            return list1;
        } else if (a == 3) {
            //成型机台信息
            machineInfo.setId(3L);
            machineInfo.setMachineCode(cxScheduleResult.getEmbryoCode());
            machineInfo.setMachineName(cxScheduleResult.getSapCode());
            machineInfo.setMachineType(cxScheduleResult.getEmbryoCode().startsWith("Y") ? "1":"2");
            List<CxMachineInfo> list3 = iCxMachineInfoService.list2(machineInfo);
            return list3;
        } else if (a == 4) {
            //硫化定点机台信息
            machineInfo.setId(4L);
            machineInfo.setMachineCode(cxScheduleResult.getSapCode());
            List<CxMachineInfo> list3 = iCxMachineInfoService.list2(machineInfo);
            return list3;
        } else if (a == 5) {
            //硫化机台信息
            machineInfo.setId(5L);
            if(cxScheduleResult!=null){
                machineInfo.setMachineCode(cxScheduleResult.getSapCode());
            }
            List<CxMachineInfo> list3 = iCxMachineInfoService.list2(machineInfo);
            return list3;
        } else if (a == 6) {
            //硫化机台信息
            machineInfo.setId(6L);
            machineInfo.setStatus("0");
            List<CxMachineInfo> list3 = iCxMachineInfoService.list2(machineInfo);
            return list3;
        }
        return null;
    }

    /**
     * 获取其他半部件机台下拉列表
     *
     * @return
     */
    public List<CxMachineInfo> getOrtherMachineInfo(int type,String status) {
        CxMachineInfo machineInfo = new CxMachineInfo();
        if (type == 1) {
            //胎面机台
            machineInfo.setId(1L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list1 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list1;
        } else if (type == 2) {
            //胎侧机台
            machineInfo.setId(2L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        } else if (type == 3) {
            //内衬机台
            machineInfo.setId(3L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        } else if (type == 4) {
            //15度裁断机台
            machineInfo.setId(4L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        } else if (type == 5) {
            //90度裁断机台
            machineInfo.setId(5L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        } else if (type == 6) {
            //胎圈机台
            machineInfo.setId(6L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        } else if (type == 7) {
            //钢丝圈机台
            machineInfo.setId(7L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        } else if (type == 8) {
            //纤维压延机台
            machineInfo.setId(8L);
            machineInfo.setStatus(status);
            List<CxMachineInfo> list3 = iCxMachineInfoService.getOrtherMachineInfo(machineInfo);
            return list3;
        }
        return null;
    }

    public List<CxMatchingSpecifyMachineList> viewList() {
        return iCxMatchingSpecifyMachineService.viewList(new CxMatchingSpecifyMachineList());
    }

}
