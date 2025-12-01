package com.zlt.aps.common;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constants.CxParamCodeConstants;
import com.zlt.aps.cx.mapper.entity.CxParamsMapper;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提供自增长流水号
 * @author 16799
 */
@Component("commonCacheService")
@Slf4j
public class CommonCacheService {

    @Autowired
    private CxParamsMapper cxEngineParamsMapper;

    /**
     * 获取刷囊时间
     *
     * @return
     */
    public Integer getBrushBagTime(Map<String, String> lhParamsMap) {
        if (StringUtils.isNotEmpty(lhParamsMap) && lhParamsMap.containsKey(CxParamCodeConstants.BRUSH_BAG_TIME)) {
            String brushBagTimeParams = lhParamsMap.get(CxParamCodeConstants.BRUSH_BAG_TIME);
            if (StringUtils.isNotEmpty(brushBagTimeParams)) {
                return Integer.valueOf(brushBagTimeParams);
            }
        }
        return 2;
    }


    /**
     * title: 获取班制 [没取到默认2班]
     * description: 优先使用参数, 没有取参数取默认值
     */
    public Integer getCxShiftSystem(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.DEFAULT_SHIFT_SYSTEM)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.DEFAULT_SHIFT_SYSTEM);
            String shiftSystem = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(shiftSystem)) {
                try {
                    return Integer.valueOf(shiftSystem);
                } catch (Exception e) {
                    log.error("班制参数错误，取默认值2班", e);
                }
            }
        }
        return 2;
    }


    /**
     * title: 获取班制开始的小时
     * description: 优先使用参数, 没有取参数取默认值
     */
    public Integer getCxShiftSystemStartHour(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.DEFAULT_SHIFT_HOURS)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.DEFAULT_SHIFT_HOURS);
            String shiftSystemStartTime = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(shiftSystemStartTime)) {
                try {
                    return Integer.valueOf(shiftSystemStartTime);
                } catch (Exception e) {
                    log.error("班制开始小时参数错误，取默认值19", e);
                }
            }
        }
        return 19;
    }


    /**
     * title: 获取硫化可等待时间（分钟）
     */
    public Integer getLhWaitTime(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.DEFAULT_SULFURIZATION_WAIT_TIME)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.DEFAULT_SULFURIZATION_WAIT_TIME);
            String lhWaitTime = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhWaitTime)) {
                try {
                    return Integer.valueOf(lhWaitTime);
                } catch (Exception e) {
                    log.error("硫化可等待时间参数错误，取默认值10", e);
                }
            }
        }
        return 10;
    }


    /**
     * 最大换工装次数
     *
     * @param cxParamsMap 成型参数集合
     * @return 实际值
     */
    public int getChangeSpecNum(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.DEFAULT_MAX_CHANGE_SPEC_SHIFTS)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.DEFAULT_MAX_CHANGE_SPEC_SHIFTS);
            String lhWaitTime = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhWaitTime)) {
                try {
                    return Integer.parseInt(lhWaitTime);
                } catch (Exception e) {
                    log.error("最大换工装次数参数错误，取默认值3", e);
                }
            }
        }
        return 3;
    }


    /**
     * 获取一次性安排投产月度剩余量参数
     */
    public int getOncePlanMonthRemain(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.ONCE_CLOSE_OUT_QTY)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.ONCE_CLOSE_OUT_QTY);
            String onceCloseOutQtyParamValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(onceCloseOutQtyParamValue)) {
                try {
                    return Integer.parseInt(onceCloseOutQtyParamValue);
                } catch (Exception e) {
                    log.error("一次性安排投产月度剩余量参数错误，取默认值200", e);
                }
            }
        }
        return 200;
    }


    /**
     * 成型允许补量最低差值分钟
     */
    public int getChangeSpecMinDiff(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.SHIFT_QUOTA_DIFF_MIN)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.SHIFT_QUOTA_DIFF_MIN);
            String shiftQuotaDiffMin = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(shiftQuotaDiffMin)) {
                try {
                    return Integer.parseInt(shiftQuotaDiffMin);
                } catch (Exception e) {
                    log.error("成型允许补量最低差值分钟参数错误，取默认值10", e);
                }
            }
        }
        return 10;
    }

    /**
     * 换工装时长
     */
    public Double getChangeSpecTime(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.CX_MIN_CHANGE_SPEC_TIME)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.CX_MIN_CHANGE_SPEC_TIME);
            String changeSpecTime = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(changeSpecTime)) {
                try {
                    return Double.parseDouble(changeSpecTime);
                } catch (Exception e) {
                    log.error("换工装时长参数错误，取默认值30.0", e);
                }
            }
        }
        return 30.0;
    }

    /**
     * 大换工装时长
     */
    public Double getBigChangeSpecTime(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.CX_MAX_CHANGE_SPEC_TIME)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.CX_MAX_CHANGE_SPEC_TIME);
            String changeSpecTime = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(changeSpecTime)) {
                try {
                    return Double.parseDouble(changeSpecTime);
                } catch (Exception e) {
                    log.error("大换工装时长参数错误，取默认值60.0", e);
                }
            }
        }
        return 60.0;
    }

    /**
     * 获取参数设定大规格模数默认值
     *
     * @param cxParamsContextMap 参数列表
     * @return 参数值
     */
    public int getBigSpecMold(Map<String, CxParams> cxParamsContextMap) {
        if (StringUtils.isNotEmpty(cxParamsContextMap) && cxParamsContextMap.containsKey(CxParamCodeConstants.DEFAULT_LH_MAX_SPEC_QTY)) {
            CxParams cxParams = cxParamsContextMap.get(CxParamCodeConstants.DEFAULT_LH_MAX_SPEC_QTY);
            String isBigMoldSpec = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(isBigMoldSpec)) {
                try {
                    return Integer.parseInt(isBigMoldSpec);
                } catch (Exception e) {
                    log.error("成型胎胚任务是否大规格参数错误，取默认值8", e);
                }
            }
        }
        return 8;
    }

    /**
     * 获取胎胚冷却时间S
     */
    public Long getLhCoolingTime(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.EMBRYO_COOL_TIME)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.EMBRYO_COOL_TIME);
            String lhCoolingTime = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhCoolingTime)) {
                try {
                    return Long.valueOf(lhCoolingTime);
                } catch (Exception e) {
                    log.error("胎胚冷却时间参数错误，取默认值10", e);
                }
            }
        }
        return 60 * 60L;
    }


    /**
     * 加载成型工序参数信息
     */
    public Map<String, String> loadCxParamsMap() {
        Map<String, String> params = new HashMap<>(16);
        List<CxParamsDto> cxParamsDtoList = this.cxEngineParamsMapper.listParams(new CxParams());
        if (StringUtils.isNotEmpty(cxParamsDtoList)) {
            params = new HashMap<>(16);
            for (CxParamsDto cxParamsDto : cxParamsDtoList) {
                params.put(cxParamsDto.getParamCode(), cxParamsDto.getParamValue());
            }
        }
        return params;
    }

    /**
     * 获取成型机台允许扁平比差额
     */
    public Double getLhFlatRatioDiff(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.FLAT_RATE_DIFF)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.FLAT_RATE_DIFF);
            String lhFlatRatioDiff = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhFlatRatioDiff)) {
                try {
                    return Double.parseDouble(lhFlatRatioDiff);
                } catch (Exception e) {
                    log.error("成型机台允许扁平比差额参数错误，取默认值0.05", e);
                }
            }
        }
        return 1.0;
    }

    /**
     * 限制机台安排不上是否抢占机台开关
     */
    public Boolean getCxRePlanLimitMachineSwitch(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.IS_PREEMPT_MACHINE)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.IS_PREEMPT_MACHINE);
            String lhLimitMachine = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhLimitMachine)) {
                try {
                    return Boolean.parseBoolean(lhLimitMachine);
                } catch (Exception e) {
                    log.error("限制机台未排时是否抢占机台参数错误，取默认值false", e);
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     *  使用正向极限值安排计划,是否开启规格拉满算法
     */
    public Boolean getCxPlanBalanceSwitch(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.IS_OPEN_BALANCE_ALGORITHM)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.IS_OPEN_BALANCE_ALGORITHM);
            String lhStartBalance = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhStartBalance)) {
                try {
                    return Boolean.parseBoolean(lhStartBalance);
                } catch (Exception e) {
                    log.error("是启用规格拉满参数错误，取默认值true", e);
                }
            }
        }
        return Boolean.TRUE;
    }

    /**
     *  开始补量算法
     */
    public Boolean getAllocateRemainingCapacitySwitch(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.IS_OPEN_MACHINE_FULL_ALGORITHM)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.IS_OPEN_MACHINE_FULL_ALGORITHM);
            String lhStartBalance = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhStartBalance)) {
                try {
                    return Boolean.parseBoolean(lhStartBalance);
                } catch (Exception e) {
                    log.error("是启用规格拉满参数错误，取默认值true", e);
                }
            }
        }
        return Boolean.TRUE;
    }


    /**
     *  是否预排明日的任务
     */
    public Boolean getCxPlanTomorrowSwitch(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.IS_PLAN_TOMORROW)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.IS_PLAN_TOMORROW);
            String lhPlanTomorrow = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanTomorrow)) {
                try {
                    return Boolean.parseBoolean(lhPlanTomorrow);
                } catch (Exception e) {
                    log.error("是否预排明日的任务参数错误，取默认值false", e);
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 胎胚备库上限值
     */
    public int getEmbryoStockMax(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.EMBRYO_STOCK_MAX)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.EMBRYO_STOCK_MAX);
            String lhPlanNoScheduleValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanNoScheduleValue)) {
                try {
                    return Integer.parseInt(lhPlanNoScheduleValue);
                } catch (Exception e) {
                    log.error("胎胚备库上限值参数错误，取默认值100", e);
                }
            }
        }
        return 100;
    }

    /**
     * 胎胚备库下限值
     */
    public int getEmbryoStockMin(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.EMBRYO_STOCK_MIN)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.EMBRYO_STOCK_MIN);
            String lhPlanNoScheduleMinValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanNoScheduleMinValue)) {
                try {
                    return Integer.parseInt(lhPlanNoScheduleMinValue);
                } catch (Exception e) {
                    log.error("胎胚备库下限值参数错误，取默认值10", e);
                }
            }
        }
        return 10;
    }

    /**
     * 胎胚备库比例
     */
    public double getEmbryoStockRatio(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.EMBRYO_STOCK_RATIO)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.EMBRYO_STOCK_RATIO);
            String lhPlanNoScheduleMinValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanNoScheduleMinValue)) {
                try {
                    return Double.parseDouble(lhPlanNoScheduleMinValue);
                } catch (Exception e) {
                    log.error("胎胚备库比例参数错误，取默认值10", e);
                }
            }
        }
        return 0.25;
    }

    /**
     * 允许手贴的轻卡规格
     */
    public String getAllowLightCarSpec(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.SPECIAL_ALLOW_HAND)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.SPECIAL_ALLOW_HAND);
            String lhPlanNoScheduleMinValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanNoScheduleMinValue)) {
                return lhPlanNoScheduleMinValue;
            }
        }
        return null;
    }

    /**
     * 前后规格允许的扁平比差额
     */
    public int getFlatRatioDiff(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.FLAT_RATE_DIFF)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.FLAT_RATE_DIFF);
            String lhPlanNoScheduleMinValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanNoScheduleMinValue)) {
                try {
                    return Integer.parseInt(lhPlanNoScheduleMinValue);
                } catch (Exception e) {
                    log.error("前后规格允许的扁平比差额参数错误，取默认值5", e);
                }
            }
        }
        return 5;
    }

    /**
     * 前后规格允许的断面宽差额
     */
    public int getSectionWidthDiff(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.SECTION_WIDTH_DIFF)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.SECTION_WIDTH_DIFF);
            String lhPlanNoScheduleMinValue = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(lhPlanNoScheduleMinValue)) {
                try {
                    return Integer.parseInt(lhPlanNoScheduleMinValue);
                } catch (Exception e) {
                    log.error("前后规格允许的断面宽差额参数错误，取默认值10", e);
                }
            }
        }

        return 10;
    }

    /**
     * 跳过的不参与培训的机台
     */
    public String getSkipMachineNo(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.SKIP_TRAINING_MACHINE_NO)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.SKIP_TRAINING_MACHINE_NO);
            String skipMachineNo = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(skipMachineNo)) {
                return skipMachineNo;
            }
        }
        return "L101,L102,L103,L104,L105,L106,L107,L108,L109,L201,L202,L203,L204,L205,L206,L207,L208,L209";
    }

    /**
     * 培训档系数
     */
    public Double getTrainingCoefficient(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.TRAINING_COEFFICIENT)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.TRAINING_COEFFICIENT);
            String trainingCoefficient = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(trainingCoefficient)) {
                try {
                    return Double.parseDouble(trainingCoefficient);
                } catch (Exception e) {
                    log.error("培训档系数参数错误，取默认值1.0", e);
                }
            }
        }
        return 1.0;
    }

    /**
     * 期初库存是否允许负数
     */
    public Boolean getIsAllowNegativeStock(Map<String, CxParams> cxParamsMap) {
        if (StringUtils.isNotEmpty(cxParamsMap) && cxParamsMap.containsKey(CxParamCodeConstants.IS_ALLOW_NEGATIVE_STOCK)) {
            CxParams cxParams = cxParamsMap.get(CxParamCodeConstants.IS_ALLOW_NEGATIVE_STOCK);
            String isAllowNegativeStock = cxParams.getParamValue();
            if (StringUtils.isNotEmpty(isAllowNegativeStock)) {
                try {
                    return Boolean.parseBoolean(isAllowNegativeStock);
                } catch (Exception e) {
                    log.error("库存是否允许负数参数错误，取默认值false", e);
                }
            }
        }
        return Boolean.FALSE;
    }
}
