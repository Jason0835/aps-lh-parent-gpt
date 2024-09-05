package com.zlt.aps.tm.service.impl;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zlt.aps.tm.api.domain.dto.MachineDto;
import com.zlt.aps.tm.api.domain.dto.MaintenanceLogDto;
import com.zlt.aps.tm.entity.MaintenanceLog;
import com.zlt.aps.tm.mapper.MaintenanceLogMapper;
import com.zlt.aps.tm.service.MaintenanceLogService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 运维操作日志表 服务实现类
 * </p>
 *
 * @author zhangbinglin
 * @since 2022-02-08
 */
@Service
public class MaintenanceLogServiceImpl extends ServiceImpl<MaintenanceLogMapper, MaintenanceLog> implements MaintenanceLogService {

    @Resource
    private MaintenanceLogMapper maintenanceLogMapper;

    /**
     * 根据查询条件查询运维操作日志
     * @param dto
     * @return
     */
    public List<MaintenanceLog> listMaintenanceLog(MaintenanceLogDto dto) {
        if (StringUtils.isNotEmpty(dto.getEndTime())) {
            dto.setEndTime(dto.getEndTime() + " 23:59:59");
        }
        return maintenanceLogMapper.listMaintenanceLog(dto);
    }

    /**
     * 根据id查询运维操作日志的明细信息
     * @param id 主键id
     * @return
     */
    public MaintenanceLogDto getDetailInfo(@PathVariable("id") Long id) {
        MaintenanceLog maintenanceLog = maintenanceLogMapper.selectById(id);
        if(maintenanceLog == null) {
            return new MaintenanceLogDto();
        }
        MaintenanceLogDto dto = JSON.parseObject(maintenanceLog.getRequestParams(), MaintenanceLogDto.class); //JSON字符串转成对象
        dto = (dto == null ? new MaintenanceLogDto() : dto);
        BeanUtils.copyProperties(maintenanceLog, dto);

        //获取机台名称
        if(dto.getMachineId() != null && StringUtil.isNotBlank(dto.getProcedureCode())) {
             List<MachineDto> machineList = maintenanceLogMapper.listMachineByProcedure(dto.getProcedureCode(), dto.getMachineId());
             if(machineList != null && machineList.size() > 0) {
                 dto.setMachineName(machineList.get(0).getMachineName());
             }
        }
        return dto;
    }

    /**
     * 排程发布重置
     * @param dto
     */
    public void resetScheduleRelease(MaintenanceLogDto dto) {
        MaintenanceLog maintenanceLog = createMaintenanceLog(dto, "1");  //把MaintenanceLogDto转成MaintenanceLog
        try {
            maintenanceLogMapper.deleteSchedulePublishRecord(dto);  //删除排程发布记录
            maintenanceLogMapper.updateScheduleReleaseStatus(dto);  //把排程发布状态重置成：未发布，“最新发布时间”清空，“发布成功计数器”重置为 0
            this.save(maintenanceLog);   //新增运维操作日志
        } catch (Exception e) {
            log.error("排程发布重置异常", e);
            maintenanceLog.setOperStatus(1);   //操作状态默认设置为：异常
            this.save(maintenanceLog);   //新增运维操作日志
            throw new RuntimeException("排程发布重置异常：" + e.toString());
        }
    }

    /**
     * 排程删除
     * @param dto
     */
    public void deleteSchedule(MaintenanceLogDto dto) {
        MaintenanceLog maintenanceLog = createMaintenanceLog(dto, "2");  //把MaintenanceLogDto转成MaintenanceLog
        try {
            maintenanceLogMapper.deleteSchedule(dto);  //删除排程发布记录
            this.save(maintenanceLog);   //新增运维操作日志
        } catch (Exception e) {
            log.error("排程删除异常", e);
            maintenanceLog.setOperStatus(1);   //操作状态默认设置为：异常
            this.save(maintenanceLog);   //新增运维操作日志
            throw new RuntimeException("排程删除异常：" + e.toString());
        }
    }

    /**
     * 根据工序类型，查询指定工序的机台列表
     * @param procedureCode 工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE）
     * @return
     */
    public List<MachineDto> listMachineByProcedure(String procedureCode) {
        if(StringUtils.isBlank(procedureCode) || "9".equals(procedureCode)) {
            //工序类型为空 或 钢带压延工序（钢带压延没机台）
            return new ArrayList<>();
        }
        return maintenanceLogMapper.listMachineByProcedure(procedureCode, null);
    }

    /**
     * 把MaintenanceLogDto转成MaintenanceLog
     * @param dto
     * @param operType  操作类型
     * @return
     */
    private MaintenanceLog createMaintenanceLog(MaintenanceLogDto dto, String operType) {
        MaintenanceLog maintenanceLog = new MaintenanceLog();
        if(dto != null) {
            BeanUtils.copyProperties(dto, maintenanceLog);
            maintenanceLog.setBaseVale(null);
            String requestJson = JSON.toJSONString(dto);
            maintenanceLog.setRequestParams(requestJson);
            maintenanceLog.setOperStatus(0);   //操作状态默认设置为：正常
            maintenanceLog.setOperType(operType);
        }
        return maintenanceLog;
    }
}
