package com.zlt.aps.context;

import com.zlt.aps.lh.api.domain.dto.ShiftTimeWindowDTO;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.mp.api.domain.entity.MdmDeviceMaintenancePlan;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import com.zlt.aps.lh.api.domain.vo.LhMoldInfoVo;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化自动排程上下文对象
 * @date 2025/3/6
 */
@Data
public class AutoLhScheduleResultContextDTO implements Serializable {


    private static final long serialVersionUID = 8736122348031246577L;

    @ApiModelProperty(value = "排程过程日志")
    private StringBuilder logDetail;

    @ApiModelProperty(value = "排程时间")
    private Date scheduleTime;

    @ApiModelProperty(value = "班制")
    private Integer workShifts;

    @ApiModelProperty(value = "批次号")
    private String batchNo;

    @ApiModelProperty(value = "月度计划年份")
    private Integer mpYear;

    @ApiModelProperty(value = "月度计划月份")
    private Integer mpMonth;

    @ApiModelProperty(value = "月度剩余量")
    private Map<String,Integer> remainMpQtyMap;

    @ApiModelProperty(value = "T-1日硫化规格代号List")
    private List<String> lastDaySpecCodeList;

    @ApiModelProperty(value = "T-1日硫化计划List")
    private List<LhScheduleResultVo> lastDayScheduleList;

    @ApiModelProperty(value = "T日硫化计划List")
    private List<LhScheduleResultVo> tDayScheduleList;

    @ApiModelProperty(value = "T+1日硫化计划List")
    private List<LhScheduleResultVo> t1DayScheduleList;

    @ApiModelProperty(value = "T日硫化规格列表")
    private Set<String> tDaySpecList;

    @ApiModelProperty(value = "月度计划List")
    private List<FactoryMonthPlanProdFinalVo> monthPlanList;

    @ApiModelProperty(value = "T日所有规格列表")
    private List<String> tDayAllSpecCodeList;

    @ApiModelProperty(value = "T-1日按机台序列化")
    private Map<String, List<LhScheduleResultVo>> lastMachineScheduledMap;

    @ApiModelProperty(value = "硫化参数List")
    private Map<String,String> lhParamsMap;

    @ApiModelProperty(value = "辅助时间")
    private Integer brushBagTime;

    @ApiModelProperty(value = "机械式设备操作时长（秒）")
    private Integer mechanicalMachineOperTime;

    @ApiModelProperty(value = "液压式设备操作时长（秒）")
    private Integer hydraulicMachineOperTime;

    @ApiModelProperty(value = "首排规格判断时间（天数）")
    private Integer firstSkuCheckTime;

    @ApiModelProperty(value = "首排规格排产计划量（条）")
    private Integer firstSkuScheduleNum;

    @ApiModelProperty(value = "日排程总计划量限制（条）")
    private Integer limitTotalPlanNum;

    @ApiModelProperty(value = "已排总计划量（条）")
    private Integer hadSchedulePlanNum;

    @ApiModelProperty(value = "是否夏季")
    private Boolean bSummerSeason;

    @ApiModelProperty(value = "T日标识")
    private String tDayFlag;

    @ApiModelProperty(value = "硫化未排计划List")
    private List<LhUnscheduledResult> lhUnscheduledResultList;

    @ApiModelProperty(value = "班次时间")
    private List<ShiftTimeWindowDTO> shiftTimeWindowDTOList;

    @ApiModelProperty(value = "规格可用模具")
    private Map<String, List<LhMoldInfoVo>> specRemainMoldQtyMap;

    @ApiModelProperty(value = "所有可用机台")
    private List<LhMachineInfo> allMachineList;

    @ApiModelProperty(value = "满排机台列表")
    private List<String> fullMachineCodeList;

    @ApiModelProperty(value = "所有机台维修保养计划")
    private List<MdmDeviceMaintenancePlan> maintenancePlanList;

    @ApiModelProperty(value = "所有有效机台（扣减维修时间）")
    List<LhMachineInfoVo> availableMachines;

    @ApiModelProperty(value = "维修机台Map")
    Map<String,LhMachineInfoVo> maintainMachineMap;

    @ApiModelProperty(value = "所有规格定点情况")
    private List<LhSpecifyMachine> lhSpecifyMachineList;
    /* 分组List */

    @ApiModelProperty(value = "续作规格List")
    private List<LhScheduleResultVo> continuedScheduleList;

    @ApiModelProperty(value = "限制规格List")
    private List<LhScheduleResultVo> restrictedScheduleList;

    @ApiModelProperty(value = "试产试制规格List")
    private List<LhScheduleResultVo> trialScheduleList;

    @ApiModelProperty(value = "剩余规格List")
    private List<LhScheduleResultVo> remainingScheduleList;

    @ApiModelProperty(value = "换模计划对象")
    private List<LhMoldChangePlan> moldChangePlanList;

    @ApiModelProperty(value = "换模key")
    private Set<String> moldChangePlanSet;

    @ApiModelProperty(value = "月度计划列表")
    List<FactoryMonthPlanProdFinalVo> currentMonthPlanList;

    @ApiModelProperty(value = "品牌排序，用英文(,)分割")
    private String brandOrder;

    @ApiModelProperty(value = "换模次数限制")
    private String changeMouldLimit;
}
