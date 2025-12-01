package com.tlt.aps.utils;

import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DictDataUtil {

    public static String getLabel4DictValue(String dictValue, List<?> sysDictDataList) {
        AtomicReference<String> dictLabel = new AtomicReference<>("");
        if (CollectionUtils.isEmpty(sysDictDataList)) {
            return dictLabel.get();
        }

        for (Object item : sysDictDataList) {
            if (item instanceof SysDictData) {
                SysDictData sysDictData = (SysDictData) item;
                if (StringUtils.equals(dictValue, sysDictData.getDictValue())) {
                    dictLabel.set(sysDictData.getDictLabel());
                    break; // 找到匹配项后退出循环
                }
            } else {
                // 处理错误情况或记录日志
            }
        }
        return dictLabel.get();
    }
}
