package com.zlt.aps.lh.check;

import com.zlt.aps.cx.entity.schedule.CxEmbryoEndingShift;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldShellInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.MdmCapsuleChuck;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 硫化排程校验上下文（每次校验新建，checker 只读）。
 *
 * <p>结果集加载口径：</p>
 * <ul>
 *   <li>已剔除硫化虚拟机台行（V 前缀）；</li>
 *   <li>已按「机台+物理班开始时刻」跨批次去重（多批次并存取最新批次行）；</li>
 *   <li>班次9的独立计划不纳入（仅 class1~class8）。</li>
 * </ul>
 *
 * @author APS
 * @date 2026/10/27
 */
@Data
public class LhScheduleCheckContext {

    /** 工厂编号 */
    private String factoryCode;

    /** 排程日期（与排程结果行落库的 SCHEDULE_DATE 一致） */
    private Date scheduleDate;

    /** 排程结果集（已剔除虚拟机台、已跨批次去重） */
    private List<LhScheduleResult> results = Collections.emptyList();

    /** 结果表最新更新时间（用于 dataStale 判断） */
    private Date latestResultUpdateTime;

    /** 模具台账（T_MDM_MODEL_INFO，工厂维度） */
    private List<MdmModelInfo> mouldInfoList = Collections.emptyList();

    /** 模壳台账（T_MDM_MOULD_SHELL_INFO，工厂维度） */
    private List<MdmMouldShellInfo> mouldShellInfoList = Collections.emptyList();

    /**
     * 胶囊卡盘台账（T_MDM_CAPSULE_CHUCK，工厂维度）
     */
    private List<MdmCapsuleChuck> capsuleChuckList = Collections.emptyList();

    /**
     * SKU模具关系（T_MDM_SKU_MOULD_REL，按结果集物料集合加载）
     */
    private List<MdmSkuMouldRel> skuMouldRelList = Collections.emptyList();

    /** 物料主数据（T_MDM_MATERIAL_INFO，按结果集物料集合加载） */
    private List<MdmMaterialInfo> materialInfoList = Collections.emptyList();

    /** 成型收尾记录（T_CX_EMBRYO_ENDING_SHIFT，按排程日窗口加载） */
    private List<CxEmbryoEndingShift> cxEndingShiftList = Collections.emptyList();

    /**
     * 判断结果集是否为空。
     *
     * @return true-为空
     */
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /**
     * 读取指定班的计划量（班次9返回 null，独立计划不纳入校验）。
     *
     * @param result 排程结果行
     * @param shiftIndex 班次序号（1~8）
     * @return 计划量；无效班次或无值返回 null
     */
    public Integer getShiftPlanQty(LhScheduleResult result, int shiftIndex) {
        if (shiftIndex < 1 || shiftIndex > LhScheduleChecker.SHIFT_SLOT_COUNT) {
            return null;
        }
        return ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
    }

    /**
     * 读取指定班的物理开始时刻。
     *
     * @param result 排程结果行
     * @param shiftIndex 班次序号（1~8）
     * @return 开始时刻；无效班次或无值返回 null
     */
    public Date getShiftStartTime(LhScheduleResult result, int shiftIndex) {
        if (shiftIndex < 1 || shiftIndex > LhScheduleChecker.SHIFT_SLOT_COUNT) {
            return null;
        }
        return ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
    }

    /**
     * 读取指定班的收尾标记。
     *
     * @param result 排程结果行
     * @param shiftIndex 班次序号（1~8）
     * @return 收尾标记（"1"-收尾）；无效班次或无值返回 null
     */
    public String getShiftIsEnd(LhScheduleResult result, int shiftIndex) {
        if (shiftIndex < 1 || shiftIndex > LhScheduleChecker.SHIFT_SLOT_COUNT) {
            return null;
        }
        return ShiftFieldUtil.getShiftIsEnd(result, shiftIndex);
    }
}
