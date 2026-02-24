package com.zlt.aps.mp.engine.service.impl;

import com.zlt.aps.mp.engine.domain.Context;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据获取
 * 抽象类-通用的业务写法，主要为基础参数校验
 *
 * @author ZLT
 * @date 20251208
 */
@Slf4j
public abstract class AbstractDataService {

    /**
     * 是否空的工厂、年份、月份、需求版本、排产版本条件
     *
     * @param context 排产上下文
     * @return
     */
    protected boolean isEmptyFactoryAndProductionVersion(Context context) {
        boolean isEmptyFactoryAndRequireVersion = isEmptyFactoryAndRequireVersion(context);
        if (isEmptyFactoryAndRequireVersion) {
            return true;
        }
        return StringUtils.isBlank(context.getProductionVersion());
    }
    /**
     * 是否空的工厂、年份、月份、需求版本
     *
     * @param context 排产上下文
     * @return
     */
    protected boolean isEmptyFactoryAndRequireVersion(Context context) {
        boolean isEmptyFactoryAndYearMonth = isEmptyFactoryAndYearMonth(context);
        if (isEmptyFactoryAndYearMonth) {
            return true;
        }
        return StringUtils.isBlank(context.getMonthPlanVersion());
    }

    /**
     * 是否空的工厂及年份、月份查询条件
     *
     * @param context 排产上下文
     * @return
     */
    protected boolean isEmptyFactoryAndYearMonth(Context context) {
        boolean isEmptyFactoryCode = isEmptyFactoryCode(context);
        if (isEmptyFactoryCode) {
            return true;
        }
        return null == context.getYear() || null == context.getMonth();
    }

    /**
     * 是否空的工厂查询条件
     *
     * @param context 排产上下文
     * @return
     */
    protected boolean isEmptyFactoryCode(Context context) {
        if (null == context) {
            return true;
        }
        return StringUtils.isBlank(context.getFactoryCode());
    }
}
