package com.zlt.aps.common.utils;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自定义excel处理，主要处理周期日动态展示
 *
 * @author ZLT
 * @date 20250702
 */
@Slf4j
public class CustomerExcelUtils<T> extends ExcelUtil<T> {

    private List<Integer> dayList;

    private String startWithName;

    private Class handlerClass;

    private static final Integer MONTH_MAX_DAY = 31;

    /**
     * 需要对排产日期day1,day2......day31进行处理
     * 非自然月动态调整展现真实日期，不在是第几天
     *
     * @param clazz         导出的实体类型
     * @param dayList       周期日及其顺序
     * @param startWithName 日期国际化的key开头，需要日期开头key一样
     * @param handlerClass  需要出来的日期实体类型，clazz与handlerClass需保持一致
     */
    public CustomerExcelUtils(Class clazz, List<Integer> dayList, String startWithName, Class handlerClass) {
        super(clazz);
        this.dayList = dayList;
        this.startWithName = startWithName;
        this.handlerClass = handlerClass;
        if (clazz != handlerClass) {
            return;
        }
        int daySize = dayList.size() + BigDecimal.ONE.intValue();
        String dayFieldNameFormat = "day%s";
        for (int day = daySize; day <= MONTH_MAX_DAY; day++) {
            String fieldName = String.format(dayFieldNameFormat, day);
            addExceptField(fieldName);
        }
    }

    /**
     * 重写数据导出列头处理
     * 非自然月的日期对应
     *
     * @param attr
     * @param row
     * @param column
     * @return
     */
    @Override
    public Cell createCell(Excel attr, Row row, int column) {
        if (clazz != handlerClass) {
            return super.createCell(attr, row, column);
        }
        //处理标题
        Cell cell = row.createCell(column);
        String attrName = handlerHearTitle(attr.name());
        cell.setCellValue(attrName);
        this.setDataValidation(attr, row, column);
        cell.setCellStyle(getStyles().get("header"));
        return cell;
    }

    /**
     * 额外处理表格标题名称
     *
     * @param attrName 原有的名称
     * @return
     */
    private String handlerHearTitle(String attrName) {
        if (StringUtils.isEmpty(attrName)) {
            return attrName;
        }
        //去除前后规格
        attrName = attrName.replaceAll("\\{", "").replaceAll("\\}", "");
        if (!attrName.startsWith(startWithName)) {
            return I18nUtil.getMessage(attrName);
        }
        String lastIndex = attrName.replaceAll(startWithName, "");
        Integer lastValue = Integer.parseInt(lastIndex) - BigDecimal.ONE.intValue();
        if (lastValue < dayList.size()) {
            attrName = String.format("%s%s", startWithName, dayList.get(lastValue));
            return I18nUtil.getMessage(attrName);
        }
        return "";
    }

}
