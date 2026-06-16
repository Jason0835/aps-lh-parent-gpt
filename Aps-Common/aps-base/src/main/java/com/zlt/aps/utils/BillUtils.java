package com.zlt.aps.utils;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.util.EntityUtil;

/**
 * 单据工具类
 * @author zlt
 *
 */
public class BillUtils {
    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    public static <T extends BaseEntity> QueryWrapper<T> builderCondition(T queryVO) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, queryVO);
        return queryWrapper;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    public static <T extends BaseEntity> void builderCondition(QueryWrapper<T> queryWrapper, T queryVO) {
        List<String> fields = EntityUtil.getAllFieldName(queryVO.getClass());
        String columnName;
        for (String field : fields) {
            if (field.equals("createTime") || field.equals("updateTime") || field.equals("isDelete")) {
                continue;
            }
            Object fieldValue = queryVO.getFieldValueByFieldName(field);
            if (PubUtil.isEmpty(fieldValue)) {
                continue;
            }

            columnName = EntityUtil.getColumnNameByFieldName(queryVO.getClass(), field);
            queryWrapper.eq(columnName, fieldValue);
        }
    }
}
