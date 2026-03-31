package com.zlt.aps.mp.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class MpStructureAllocationVo extends MpStructureAllocation {

    private static final long serialVersionUID = 1L;

    /**
     * 成型机编码
     */
    @ApiModelProperty(value = "成型机编码", name = "scheduledMachines")
    private String scheduledMachines;


}
