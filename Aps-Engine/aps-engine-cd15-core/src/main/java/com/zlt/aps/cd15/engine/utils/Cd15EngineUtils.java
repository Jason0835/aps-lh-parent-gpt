package com.zlt.aps.cd15.engine.utils;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;

/**
 * 钢丝斜裁工具
 *
 */
public class Cd15EngineUtils {

    /**
     * 获取钢带编号
     * 
     * @param resultVo
     * @return
     */
    public static String getSteelStripCode(Cd15ScheduleResultVo resultVo) {
        return StringUtils.isNotEmpty(resultVo.getSteelStripCode1()) ? resultVo.getSteelStripCode1()
                : resultVo.getSteelStripCode2();
    }
}
