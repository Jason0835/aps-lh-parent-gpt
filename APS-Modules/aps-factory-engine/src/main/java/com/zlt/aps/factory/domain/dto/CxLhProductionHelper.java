package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 成型配比下的硫化机台排产信息
 * 用以记录成型硫化配比的收尾排产信息，只保留最后一个信息
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class CxLhProductionHelper implements Serializable {
    /**
     * 硫化配比分组编号 1~最大组
     */
    private Integer lhGroupNo;
    /**
     * 分组信息--TBR = 结构名
     */
    private String groupName;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 胎胚号
     */
    private String embryoCode;
    /**
     * 当天排产量
     */
    private Integer productionQty;

    /**
     * 排产模具
     */
    private Set<String> productionMouldSet;

    /**
     * 排产天 周期第几天
     */
    private Integer productionDay;

    /**
     * 天日硫化量--满产
     */
    private Integer dayMaxProductionQty;
    /**
     * 成型机台编码信息
     * 有可能一个，也有可能多个
     */
    private Set<String> cxMachineInfo;
    /**
     * 结束日--修正时使用
     */
    private Integer endDay;
    /**
     * 不再参与排产 0 不参与 否则参与
     */
    private Integer isProduction;

    /**
     * 构建空的成型下硫化分组信息
     *
     * @param groupName 分组计划名-TBR为结构名
     * @param lhGroupNo 虚拟的硫化分组
     * @return
     */
    public static CxLhProductionHelper createEmptyLhGroup(String groupName, Integer lhGroupNo, Set<String> cxMachineInfo) {
        CxLhProductionHelper cxLh = new CxLhProductionHelper();
        cxLh.setGroupName(groupName);
        cxLh.setLhGroupNo(lhGroupNo);
        cxLh.setIsProduction(YesOrNoEnum.YES.getValue());
        if (CollectionUtils.isEmpty(cxMachineInfo)) {
            cxMachineInfo = new HashSet<>();
        }
        cxLh.setCxMachineInfo(cxMachineInfo);
        return cxLh;
    }

    /**
     * 重新设置成型硫化排产信息
     *
     * @param groupName 分组计划名-TBR为结构
     * @param startDay  排产开始日-即在排产周期的第几天
     */
    public void resetProductionInfoByNewGroupName(String groupName, Integer startDay) {
        this.groupName = groupName;
        this.productionDay = startDay;
        this.productionQty = BigDecimal.ZERO.intValue();
        this.dayMaxProductionQty = null;
        this.materialCode = null;
        this.materialDesc = null;
        this.isProduction = YesOrNoEnum.YES.getValue();
        //排产模具是否要清空？
    }


    /**
     * 当前硫化组是否需要换模
     *
     * @param addSkuInfo
     * @return
     */
    public boolean isChangeMould(MonthPlanProductionRequirePlanVo addSkuInfo) {
        if (null == addSkuInfo || StringUtils.isEmpty(addSkuInfo.getMaterialDesc())) {
            return false;
        }
        String connectSkuMaterialDesc = addSkuInfo.getMaterialDesc();
        return !connectSkuMaterialDesc.equals(materialDesc);
    }
    /**
     * 更新可排产时间范围
     *
     * @param closingDay 新收尾日
     * @param endDay     新排产结束日
     */
    public void updateProductionDateRange(Integer closingDay, Integer endDay) {
        this.productionDay = closingDay;
        this.endDay = endDay;
    }
}
