package com.zlt.aps.common.core.utils;

import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机台开机班次 标签与值 转换工具。
 *
 * <p>开机班次在导入模板中由用户直接填写班次名称(如 夜班,早班,中班，多个用英文逗号分隔)，
 * 需转换为对应字典 {@code class_num_three_plan} 的值(01,02,03)入库；导出时再把字典值转回标签。</p>
 */
public final class MachineShiftDictUtil {

    /** 开机班次字典类型。 */
    private static final String DICT_TYPE = "class_num_three_plan";

    private MachineShiftDictUtil() {
    }

    /**
     * 开机班次标签转值：夜班,早班 -> 01,02。
     *
     * @param labels 逗号分隔的班次标签，可为空
     * @return 逗号分隔的班次值；未命中的标签原样保留
     */
    public static String labelsToValues(String labels) {
        if (StringUtils.isBlank(labels)) {
            return labels;
        }
        return Arrays.stream(labels.split(","))
                .map(String::trim)
                .map(MachineShiftDictUtil::labelToValue)
                .collect(Collectors.joining(","));
    }

    /**
     * 开机班次值转标签：01,02 -> 夜班,早班。
     *
     * @param values 逗号分隔的班次值，可为空
     * @return 逗号分隔的班次标签；未命中的值原样保留
     */
    public static String valuesToLabels(String values) {
        if (StringUtils.isBlank(values)) {
            return values;
        }
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .map(MachineShiftDictUtil::valueToLabel)
                .collect(Collectors.joining(","));
    }

    /**
     * 单个班次标签转值，未命中时原样保留。
     */
    private static String labelToValue(String label) {
        return getDictData().stream()
                .filter(item -> label.equals(item.getDictLabel()))
                .map(SysDictData::getDictValue)
                .findFirst()
                .orElse(label);
    }

    /**
     * 单个班次值转标签，未命中时原样保留。
     */
    private static String valueToLabel(String value) {
        return getDictData().stream()
                .filter(item -> value.equals(item.getDictValue()))
                .map(SysDictData::getDictLabel)
                .findFirst()
                .orElse(value);
    }

    /**
     * 获取开机班次字典项；字典为空时返回空集合避免 NPE。
     */
    private static List<SysDictData> getDictData() {
        List<SysDictData> dictDatas = DictUtils.getDictCache(DICT_TYPE);
        return dictDatas == null || dictDatas.isEmpty() ? Collections.emptyList() : dictDatas;
    }
}
