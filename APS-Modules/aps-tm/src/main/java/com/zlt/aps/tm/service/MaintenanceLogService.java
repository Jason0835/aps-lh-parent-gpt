package com.zlt.aps.tm.service;

import com.zlt.aps.tm.api.domain.dto.MachineDto;
import com.zlt.aps.tm.api.domain.dto.MaintenanceLogDto;
import com.zlt.aps.tm.entity.MaintenanceLog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 运维操作日志表 服务类
 * </p>
 *
 * @author zhangbinglin
 * @since 2022-02-08
 */
public interface MaintenanceLogService extends IService<MaintenanceLog> {

    /**
     * 根据查询条件查询运维操作日志
     * @param dto
     * @return
     */
    List<MaintenanceLog> listMaintenanceLog(MaintenanceLogDto dto);

    /**
     * 根据id查询运维操作日志的明细信息
     * @param id 主键id
     * @return
     */
    MaintenanceLogDto getDetailInfo(@PathVariable("id") Long id);

    /**
     * 排程发布重置
     * @param dto
     */
    void resetScheduleRelease(MaintenanceLogDto dto);

    /**
     * 排程删除
     * @param dto
     */
    void deleteSchedule(MaintenanceLogDto dto);

    /**
     * 根据工序类型，查询指定工序的机台列表
     * @param procedureCode 工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE）
     * @return
     */
    List<MachineDto> listMachineByProcedure(String procedureCode);
}
