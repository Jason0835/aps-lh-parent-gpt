package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.Context;

/**
 * 排产过程业务接口
 *
 * @author ZLT
 * @date 20250220
 */
public interface IProductionBusinessService {
    /**
     * 执行业务排产逻辑
     *
     * @param context 排产上下文
     * @param userObj 用户数据
     */
    void run(Context context, Object userObj);

}
