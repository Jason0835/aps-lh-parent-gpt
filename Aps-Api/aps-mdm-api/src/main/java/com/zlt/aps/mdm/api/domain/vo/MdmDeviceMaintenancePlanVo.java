package com.zlt.aps.mdm.api.domain.vo;

import com.zlt.aps.mdm.api.domain.entity.MdmDeviceMaintenancePlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Chen
 * @since: 2021/9/26 17:41
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmDeviceMaintenancePlanVo extends MdmDeviceMaintenancePlan {

    private String productTypeCode;
}
