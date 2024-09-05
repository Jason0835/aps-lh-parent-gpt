package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.dto.MachineDto;
import com.zlt.aps.tm.api.domain.dto.MaintenanceLogDto;
import com.zlt.aps.tm.entity.MaintenanceLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 运维操作日志表 Mapper 接口
 * </p>
 *
 * @author zhangbinglin
 * @since 2022-02-08
 */
public interface MaintenanceLogMapper extends BaseMapper<MaintenanceLog> {

    /**
     * 根据查询条件查询运维操作日志
     * @param dto
     * @return
     */
    List<MaintenanceLog> listMaintenanceLog(MaintenanceLogDto dto);

    /**
     * 删除排程发布记录
     * @param dto
     */
    void deleteSchedulePublishRecord(MaintenanceLogDto dto);

    /**
     * 把排程发布状态重置成：未发布
     * @param dto
     */
    void updateScheduleReleaseStatus(MaintenanceLogDto dto);

    /**
     * 排程删除
     * @param dto
     */
    void deleteSchedule(MaintenanceLogDto dto);

    /**
     * 根据工序类型，查询指定工序的机台列表
     * @param procedureCode 工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE）
     * @param machineId 机台id
     * @return
     */
    List<MachineDto> listMachineByProcedure(@Param("procedureCode") String procedureCode, @Param("machineId") Long machineId);
}
