package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmDeviceMaintenancePlan;
import com.zlt.aps.mdm.api.domain.vo.MdmDeviceMaintenancePlanVo;

import java.util.List;

public interface IMdmDeviceMaintenancePlanService {

    /**
     * 查询设备维护计划数据
     *
     * @param docDeviceMaintenancePlan
     * @return
     */
    List<MdmDeviceMaintenancePlan> selectDocDeviceMaintenancePlanList(MdmDeviceMaintenancePlanVo docDeviceMaintenancePlan);

    /**
     * 导入设备维护计划
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    AjaxResult importData(List<MdmDeviceMaintenancePlanVo> list, boolean updateSupport, Long importLogId);

    /**
     * 根据多个主键删除记录
     *
     * @param ids 主键列表
     * @return 删除的记录数量
     */
    int deleteByIds(List<Long> ids);

    /**
     * 新增设备维护计划
     */
    int insert(MdmDeviceMaintenancePlan docDeviceMaintenancePlan);


    /**
     * 根据主键编辑设备维护计划
     */
    int updateByPrimaryKey(MdmDeviceMaintenancePlan docDeviceMaintenancePlan);

    /**
     * 根据主键查询
     */
    MdmDeviceMaintenancePlan selectByPrimaryKey(Long id);
}
