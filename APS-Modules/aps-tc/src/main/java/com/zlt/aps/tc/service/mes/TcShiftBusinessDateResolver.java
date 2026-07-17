package com.zlt.aps.tc.service.mes;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;

import java.util.Date;

/**
 * 胎侧六班与MES三班业务日期映射器。
 *
 * <p>映射数据通过固定模板数组表达，避免在完成量回写、发布组装和滚动计算中重复编写六班分支。</p>
 */
public final class TcShiftBusinessDateResolver {

    /** class1至class6相对结果排程日期的MES业务日偏移。 */
    private static final int[] MES_BUSINESS_DATE_OFFSETS = {-1, 0, 0, 0, 1, 1};

    /** class1至class6对应的MES班别。 */
    private static final String[] MES_SHIFT_CODES = {"MID", "NIGHT", "DAY", "MID", "NIGHT", "DAY"};

    /** 工具类不允许实例化。 */
    private TcShiftBusinessDateResolver() {
    }

    /**
     * 根据结果排程日期、MES业务日期和MES班别反查六班序号。
     *
     * @param resultScheduleDate 结果排程日期
     * @param mesBusinessDate MES业务日期
     * @param mesShiftCode MES班别，取值NIGHT/DAY/MID
     * @return 六班序号，无法匹配时返回null
     */
    public static Integer resolveShiftOrder(Date resultScheduleDate, Date mesBusinessDate, String mesShiftCode) {
        if (resultScheduleDate == null || mesBusinessDate == null || StrUtil.isBlank(mesShiftCode)) {
            return null;
        }
        String normalizedShiftCode = StrUtil.trim(mesShiftCode).toUpperCase();
        String mesDateText = DateUtil.formatDate(mesBusinessDate);
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            Date expectedDate = resolveMesBusinessDate(resultScheduleDate, shiftOrder);
            if (DateUtil.formatDate(expectedDate).equals(mesDateText)
                    && resolveMesShiftCode(shiftOrder).equals(normalizedShiftCode)) {
                return shiftOrder;
            }
        }
        return null;
    }

    /**
     * 解析指定六班对应的MES业务日期。
     *
     * @param resultScheduleDate 结果排程日期
     * @param shiftOrder 六班序号
     * @return MES业务日期
     * @throws IllegalArgumentException 日期为空或班次超出1至6时抛出
     */
    public static Date resolveMesBusinessDate(Date resultScheduleDate, int shiftOrder) {
        validateShiftOrder(resultScheduleDate, shiftOrder);
        return DateUtil.offsetDay(resultScheduleDate, MES_BUSINESS_DATE_OFFSETS[shiftOrder - 1]);
    }

    /**
     * 解析指定六班对应的MES班别。
     *
     * @param shiftOrder 六班序号
     * @return MES班别，取值NIGHT/DAY/MID
     * @throws IllegalArgumentException 班次超出1至6时抛出
     */
    public static String resolveMesShiftCode(int shiftOrder) {
        validateShiftOrder(new Date(0L), shiftOrder);
        return MES_SHIFT_CODES[shiftOrder - 1];
    }

    /**
     * 校验结果日期和六班序号。
     *
     * @param resultScheduleDate 结果排程日期
     * @param shiftOrder 六班序号
     * @throws IllegalArgumentException 参数无效时抛出
     */
    private static void validateShiftOrder(Date resultScheduleDate, int shiftOrder) {
        if (resultScheduleDate == null || shiftOrder < 1 || shiftOrder > TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
            throw new IllegalArgumentException("Invalid TC shift mapping arguments");
        }
    }
}
