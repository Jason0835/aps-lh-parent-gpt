package com.zlt.aps.factory.domain.vo;

import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.enums.ProductionOrientEnum;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.common.utils.PubUtil;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模具信息
 *
 * @author ZLT
 * @date 20250219
 */
@Data
public class MouldInfoVO implements Serializable {

    /**
     * 模具编号
     */
    private String mouldCode;
    /**
     * 模具
     */
    private String mouldNo;
    /**
     * 分厂编号
     */
    private String factoryCode;

    /**
     * 模具类型
     */
    private String mouldType;

    /**
     * 模具气套类型
     */
    private String mouldAirType;
    /**
     * 总硫化时间-到秒
     */
    private BigDecimal totalSeconds;
    /**
     * 已经硫化时间-到秒
     */
    private BigDecimal usedSeconds;
    /**
     * 剩余硫化时间-到秒
     */
    private BigDecimal leftOverSeconds;
    /**
     * 预占剩余硫化时间-到秒--预占时使用
     */
    private BigDecimal preemptLeftOverSeconds;
    /**
     * 关联物料个数（预占产能计算使用）
     */
    private Integer assocaiationCount;
    /**
     * 排产日 1~31
     * 可排产日对应的硫化时间，单位秒<排产日, 剩余硫化时间秒>
     */
    private Map<Integer, BigDecimal> productionDayList;
    /**
     * 已排产完毕日--反向排产使用
     * --主要是因为交期导致不能连续排产，
     * 才出现需要记录已排产完毕日的判断
     */
    private Set<Integer> productionFinishDayList;
    /**
     * 不可排产日 1~31
     * 不可排产日列表--停工与维修初始固定，
     */
    private Map<Integer, NoProductionDayMouldVo> noProductionDayList;
    /**
     * 洗模日列表
     * 洗模日随着排产而加入
     */
    private Map<Integer, NoProductionDayMouldVo> cleanDayList;
    /**
     * 是否排产完成
     */
    private Boolean isFinish;
    /**
     * 标记是否为续作模具
     */
    private Integer isContinue;
    /**
     * 分组值--两副、两副一组
     */
    private Integer groupValue;
    /**
     * 规格
     */
    private String specifications;
    /**
     * 花纹
     */
    private String pattern;
    /**
     * 开始排产日期--随着排产继续一直变化
     * 正向排产初始为1，方向排产为月末
     */
    private Integer beginDay;
    /**
     * 排产截止日--随着排产继续会存在变化
     * 主要是因为交期影响，如果无交期，
     * 则正向排产=月末，反向排产=月初
     */
    private Integer endDay;
    /**
     * 当前排产的物料编号
     */
    private String currentProductCode;
    /**
     * 续作模具的续作规格--不会变动
     */
    private String continueProductCode;
    /**
     * 续作模具的续作排产分组
     */
    private String continueProductionGroupValue;
    /**
     * 续作排产模台数
     */
    private Integer continueMouldQty;
    /**
     * 续作排产分组本身-模台数
     */
    private Integer continueMouldNumber;
    /**
     * 排产方向
     */
    private ProductionOrientEnum productionOrient;
    /**
     * 同规格已连续排产天数-用以判断洗模
     */
    private Integer continuousDays;
    /**
     * 类型标识
     */
    private String tradeMode;
    /**
     * 模具日排产信息
     */
    private Map<Integer, List<MouldDayProductionVo>> dayProductionMap;
    /**
     * 交期预排使用：
     * 第二天是否需要扣减产能
     * 换规格时，可能出现跨天
     */
    private BigDecimal nextDaySubtractTime;
    /**
     * 交期预排使用：
     * 是否需要洗模
     */
    private Boolean isClearMould;
    /**
     * 拼模起始日--拼模使用
     */
    private Integer assemblingMouldStartDay;

    /**
     * 标记是否已排产
     *
     * @return
     */
    public Integer getIsProduction() {
        if (null == productionOrient) {
            return YesOrNoEnum.NO.getValue();
        }
        return YesOrNoEnum.YES.getValue();
    }

    /**
     * 模具大类
     * （大概率同模具）
     *
     * @return
     */
    public String getMouldClass() {
        return StringUtils.format("{}_{}", specifications, pattern);
    }

    @Override
    public boolean equals(Object v1) {
        if (PubUtil.isEmpty(v1) || v1.getClass() != this.getClass()) {
            return false;
        }
        if (StringUtils.equals(this.getMouldCode(), ((MouldInfoVO) v1).getMouldCode())) {
            return true;
        }
        return false;
    }
}
