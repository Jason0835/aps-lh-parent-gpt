package com.tlt.aps.utils;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.common.annotation.ImportExcelValidated;
import com.zlt.common.enums.ImportErrorValueEnum;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.UtilReflect;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 导入工具类
 */
@Slf4j
public class ImportExcelValidatedUtils {

    /**
     * 导出时字典缓存
     */
    private static ThreadLocal<Map<String, String>> dictDataCach = new ThreadLocal<>();

    /**
     * 每个月的合法天数
     */
    private static final int[] LEGAL_DAYS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    /**
     * 验证导入数据正确性，并返回导入错误日志列表
     *
     * @param importLogId 导入日志id
     * @param row         行数
     * @param obj         单元
     * @return
     */
    public static List<ImportErrorLog> validated(Long importLogId, Integer row, Object obj) {
        List<ImportErrorLog> list = new ArrayList<>();
        //得到class
        Class cls = obj.getClass();
        //得到所有属性
        Field[] fields = cls.getDeclaredFields();
        dictDataCach.set(new HashMap<>());
        String valueStr = null;
        Excel excelAnnotation;
        ImportExcelValidated validatedAnnotation = null;
        for (int i = 0; i < fields.length; i++) {//遍历
            try {
                //得到属性
                Field field = fields[i];
                //打开私有访问
                field.setAccessible(true);
                //获取属性值
                Object value = field.get(obj);
                validatedAnnotation = field.getAnnotation(ImportExcelValidated.class);
                if (validatedAnnotation == null) {
                    continue;
                }

                //增加类型判断 Chad 2022-09-28
                excelAnnotation = field.getAnnotation(Excel.class);
                if (PubUtil.isEmpty(value)) {
                    valueStr = "";
                } else {
                    if (field.getType() == Date.class && PubUtil.isNotEmpty(excelAnnotation) && PubUtil.isNotEmpty(excelAnnotation.dateFormat())) {
                        valueStr = DateUtils.parseDateToStr(excelAnnotation.dateFormat(), (Date) value);
                    } else {
                        valueStr = (value == null ? "" : String.valueOf(value));
                    }
                }

                Excel excel = field.getAnnotation(Excel.class);
                String result = validatedValue(row, valueStr, validatedAnnotation, excel, obj);
                if (StringUtils.isNotBlank(result)) {
                    list.add(new ImportErrorLog(importLogId, row, result));
                }
            } catch (Exception e) {
                log.error("验证导入属性错误" , e.getMessage());
            }
        }
        dictDataCach.remove();
        return list;
    }

    /**
     * @param row       行数
     * @param value     字段值
     * @param validated
     * @return
     */
    private static String validatedValue(Integer row, String value, ImportExcelValidated validated, Excel excel, Object obj) throws ParseException {
        row = (row == null ? 0 : row);
        value = (StringUtils.isBlank(value)) ? "" : value.trim();
        String name = validated.name();
        String excelName = excel.name();
        String importName = excel.importName();
        if (StringUtils.isEmpty(name)) {
            name = StringUtils.isEmpty(importName) ? excelName : importName;
        }
        name = (StringUtils.isBlank(name) ? "" : I18nUtil.getMessage(name));
        // 优先校验日期,格式不正确则实体属性为"1970-01-01"
        if (validated.date() && "1970-01-01".equals(value)) {
            // 必须输入日期格式
            // "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            // "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            // "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"
            String message = I18nUtil.getMessage("import.validated.date");
            return String.format(message, row, name);
        }
        // 日期年月日校验
        if (validated.date() && StringUtils.isNotBlank(value)) {
            String[] dateArr = value.split("-");
            // 年月日
            if (dateArr.length == 3) {
                String message = I18nUtil.getMessage("import.validated.date");
                // 年不能大于四位数
                int year = Integer.parseInt(dateArr[0]);
                if (year > 9999) {
                    return String.format(message, row, name);
                }
                // 月不能大于12
                int month = Integer.parseInt(dateArr[1]);
                if (month > 12) {
                    return String.format(message, row, name);
                }
                // 判断平年闰年
                if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                    // 闰年二月有29天
                    LEGAL_DAYS[1] = 29;
                } else {
                    // 平年二月有28天
                    LEGAL_DAYS[1] = 28;
                }
                int day = Integer.parseInt(dateArr[2]);
                // 天数是否合法
                if (day > LEGAL_DAYS[month - 1] || day < 0) {
                    return String.format(message, row, name);
                }
            }
        }
        if (validated.required() && StringUtils.isBlank(value)) {
            //校验不能为空
            String message = I18nUtil.getMessage("import.validated.required");
            return String.format(message, row, name);
        }
        if (validated.isCode() && !isCode(value)) {
            //必须输入字母、数字以及英文字符
            String message = I18nUtil.getMessage("import.validated.isCode");
            return String.format(message, row, name);
        }
        if ((validated.number() && !isNumber(value)) || (validated.number() && isMaxValue(value))) {
            // 必须输入合法的数字(负数，小数)
            String message = I18nUtil.getMessage("import.validated.number");
            if (value.equals(ImportErrorValueEnum.INTEGER_VALUE.getNumber().toString())) {
                message = I18nUtil.getMessage("import.errorValueEnum.message.integerValue");
            }
            if (value.equals(ImportErrorValueEnum.DOUBLE_VALUE.getNumber().toString())) {
                message = ImportErrorValueEnum.DOUBLE_VALUE.getErrorMessage();
            }
            if (value.equals(ImportErrorValueEnum.LONG_VALUE.getNumber().toString())) {
                message = ImportErrorValueEnum.LONG_VALUE.getErrorMessage();
            }
            if (value.equals(ImportErrorValueEnum.FLOAT_VALUE.getNumber().toString())) {
                message = ImportErrorValueEnum.FLOAT_VALUE.getErrorMessage();
            }
            if (value.equals(ImportErrorValueEnum.BIGDECIMAL_VALUE.getNumber().toString())) {
                message = ImportErrorValueEnum.BIGDECIMAL_VALUE.getErrorMessage();
            }
            return String.format(message, row, name);
        }
        if ((validated.digits() && !isInteger(value) || (validated.digits() && isMaxValue(value)))) {
            // 必须输入整数
            String message = I18nUtil.getMessage("import.validated.digits");
            return String.format(message, row, name);
        }

        if (validated.colorCode() && !isColorCode(value)) {
            // 必须符合颜色表达式格式 例：#000000
            String message = I18nUtil.getMessage("import.validated.colorCode");
            return String.format(message, row, name);
        }

        int maxLength = validated.maxLength();
        if (value.length() > maxLength) {
            //允许的最大长度
            String message = I18nUtil.getMessage("import.validated.maxLength");
            return String.format(message, row, name, maxLength);
        }

        //2023-11-28  允许的最大长度：中文检测 Nick+
        // int chineseNumber = countChineseCharacters(value);
        // if (StringUtils.isNotEmpty(value) && chineseNumber > 0) {
        //     //获取字符中文个数
        //     int realLength = value.length() - chineseNumber + (chineseNumber * 3);
        //     if (realLength > maxLength){
        //         String message = I18nUtil.getMessage("import.validated.maxLength");
        //         return String.format(message, row, name, maxLength);
        //     }
        // }

        int minLength = validated.minLength();
        if (value.length() < minLength) {
            //允许的最小长度
            String message = I18nUtil.getMessage("import.validated.minLength");
            return String.format(message, row, name, minLength);
        }

        double max = validated.max();
        if (max != Double.MAX_VALUE) {
            if (!isNumber(value)) {
                //不是数字
                String message = I18nUtil.getMessage("import.validated.number");
                return String.format(message, row, name);
            } else if (isNumber(value) && Double.parseDouble(value) > max) {
                //允许的最大值
                String message = I18nUtil.getMessage("import.validated.max");
                String valStr = max + "";
                if (valStr.indexOf("E") >= 0) {
                    BigDecimal realValue = new BigDecimal(valStr);
                    valStr = realValue.toPlainString();
                }
                return String.format(message, row, name, valStr);
            }
        }

        double min = validated.min();
        if (min != Double.MIN_VALUE) {
            if (!isNumber(value)) {
                //不是数字
                String message = I18nUtil.getMessage("import.validated.number");
                return String.format(message, row, name);
            } else if (isNumber(value) && Double.parseDouble(value) < min) {
                //允许的最小值
                String message = I18nUtil.getMessage("import.validated.min");
                return String.format(message, row, name, min);
            }
        }

        String dictType = "".equals(validated.dictType()) ? excel.dictType() : validated.dictType();
        if (StringUtils.isNotEmpty(dictType) && StringUtils.isNotEmpty(value)) {
            try {
                convertByDictValue(dictDataCach, dictType);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String val = dictDataCach.get().get(dictType + SecurityUtils.getUserLang() + value);
            if (StringUtils.isEmpty(val)) {
                String message = I18nUtil.getMessage("import.validated.notDict");
                String msg = String.format(message, row, name);
                if (value.contains(",")) {
                    String[] values = value.split(",");
                    for (String s : values) {
                        val = dictDataCach.get().get(dictType + SecurityUtils.getUserLang() + s);
                        msg = StringUtils.isEmpty(val) ? msg : "";
                    }
                }
                return StringUtils.isEmpty(msg) ? null : msg;
            }
        }
        return null;
    }


    /**
     * 是否是最大值
     *
     * @param value
     * @return
     */
    private static boolean isMaxValue(String value) {
        boolean isMaxValue = false;
        if (value.equals(Integer.MAX_VALUE + "")) {
            return isMaxValue = true;
        }
        if (value.equals(ImportErrorValueEnum.DOUBLE_VALUE.getNumber().toString())) {
            return isMaxValue = true;
        }
        if (value.equals(ImportErrorValueEnum.LONG_VALUE.getNumber().toString())) {
            return isMaxValue = true;
        }
        if (value.equals(ImportErrorValueEnum.FLOAT_VALUE.getNumber().toString())) {
            return isMaxValue = true;
        }
        if (value.equals(ImportErrorValueEnum.BIGDECIMAL_VALUE.getNumber().toString())) {
            return isMaxValue = true;
        }
        return isMaxValue;
    }

    /**
     * 将字典缓存到线程工具类里面
     *
     * @param dictDataCach
     * @param dictType
     * @return
     * @throws Exception
     */
    public static ThreadLocal<Map<String, String>> convertByDictValue(ThreadLocal<Map<String, String>> dictDataCach, String dictType) throws Exception {
        String dictDataStr = dictDataCach.get().get(dictType + "hasValue");
        if (StringUtils.isEmpty(dictDataStr)) {
            Locale userLang = SecurityUtils.getUserLang();
            List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
            for (SysDictData sysDictData : dictDatas) {
                dictDataCach.get().put(dictType + userLang + sysDictData.getDictValue(), sysDictData.getDictValue());
            }
            dictDataCach.get().put(dictType + "hasValue" , "hasValue");
        }
        return dictDataCach;
    }

    /**
     * 添加错误详细日志到集合
     *
     * @param importLogId     导入日志id
     * @param errorNum        错误行数
     * @param errorDetail     错误详细信息
     * @param importErrorLogs 详细日志集合
     */
    public static void addImportErrorLog(Long importLogId, Integer errorNum, String
            errorDetail, List<ImportErrorLog> importErrorLogs) {
        ImportErrorLog importErrorLog = new ImportErrorLog();
        importErrorLog.setCreateBy(SecurityUtils.getUsername());
        importErrorLog.setCreateTime(new Date());
        importErrorLog.setImportLogId(importLogId);
        importErrorLog.setErrorDetail(errorDetail);
        importErrorLog.setErrorRow(errorNum);
        importErrorLog.setDelFlag(ApsConstant.APS_YES_NO_0.toString());
        importErrorLogs.add(importErrorLog);
    }


    /**
     * 验证一个字符串是否为数字
     *
     * @param value
     * @return
     */
    public static boolean isNumber(String value) {
        if (value == null)
            return false;
        if ("".equals(value))
            return true;

        Pattern pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        if (!pattern.matcher(value).matches()) {
            Pattern pattern2 = Pattern.compile("^[+-]?\\d+\\.?\\d*[Ee][+-]?\\d+$");
            return pattern2.matcher(value).matches();
        } else {
            return true;
        }
    }

    /**
     * 判断一个字符的是否为整数
     *
     * @param value
     * @return
     */
    public static boolean isInteger(String value) {
        if (value == null)
            return false;
        if ("".equals(value))
            return true;

        Pattern pattern = Pattern.compile("^-?\\d+$");
        if (!pattern.matcher(value).matches()) {
            Pattern pattern2 = Pattern.compile("^[+-]?\\d+\\.?\\d*[Ee][+-]?\\d+$");
            return pattern2.matcher(value).matches();
        } else {
            return true;
        }
    }

    /**
     * 判断一个字符串是否为：字母、数字以及英文字符
     *
     * @param value
     * @return
     */
    public static boolean isCode(String value) {
        if (value == null)
            return false;
        if ("".equals(value))
            return true;
        Pattern pattern = Pattern.compile("^[\\x00-\\xff]*$");
        return pattern.matcher(value).matches();
    }

    /**
     * 判断一个字符的是否符合颜色表达式格式
     *
     * @param value 要验证的字符
     * @return 是否符合
     */
    public static boolean isColorCode(String value) {
        if (value == null)
            return false;
        if ("".equals(value))
            return true;
        Pattern pattern = Pattern.compile("^#[0-9a-fA-F]{6}$");
        return pattern.matcher(value).matches();
    }


    /**
     * Excel导入校验重复 Nick +
     * 2023-11-28 重新了框架的判断方法,扩展字段就是判断的属性,如果不传就比较全部字段是否重复
     * 修改建议：修改前注意是否有其它地方使用到这个方法
     *
     * 效果：当表格中存在重复数据只取最下面的一条进行导入，其余提示
     *
     * @param list            业务对象列表
     * @param currentObj      当前业务对象
     * @param currentIndex    当前业务对象位置
     * @param startRow        Excel开始行，正常是2
     * @param importLogId     日志ID
     * @param importErrorLogs 日志明细列表
     * @param arg 比较的属性名称
     */
    public static void validatedRepeat(List list, Object currentObj, int currentIndex, int startRow,
                                       Long importLogId, List<ImportErrorLog> importErrorLogs, String... arg) {
        String repeatMsg = I18nUtil.getMessage("import.validated.repeat");
        String errorMsg;
        int iSize = list.size();

        //比较对象没传入直接返回
        if (currentObj == null){
            return;
        }

        //如果没有指定比较的属性，则默认比较全部
        if (arg == null || arg.length <= 0){
            for (int j = currentIndex + 1; j < iSize; j++) {
                //对象比较，暂转换字符串比较
                if (currentObj.toString().equals(list.get(j).toString())) {
                    errorMsg = String.format(repeatMsg, currentIndex + startRow,j+startRow);
                    addImportErrorLog(importLogId,currentIndex + startRow,errorMsg,importErrorLogs);
                    break;
                }
            }
        }else {
            //先构建比较对象指定的属性
            StringBuilder currentObjStr = new StringBuilder();
            for (String key: arg) {
                currentObjStr.append(UtilReflect.getFieldValue(currentObj, key));
            }

            for (int j = currentIndex + 1; j < iSize; j++) {
                StringBuilder objStr = new StringBuilder();
                //对象比较，暂转换字符串比较
                for (String key: arg){
                    objStr.append(UtilReflect.getFieldValue(list.get(j), key));
                }
                if (currentObjStr.toString().equals(objStr.toString())) {
                    errorMsg = String.format(repeatMsg, currentIndex + startRow,j+startRow);
                    addImportErrorLog(importLogId,currentIndex + startRow,errorMsg,importErrorLogs);
                    break;
                }
            }
        }
    }

    /**
     * 判断字符串含有的中文个数
     * 2023-11-28 Nick+
     *
     * @param str
     * @return
     */
    public static int countChineseCharacters(String str) {
        //0.如果时空直接返回
        if (StringUtils.isEmpty(str)) {
            return 0;
        }
        //1.通过正则表达式判断中文个数
        String regEx = "[\\u4e00-\\u9fa5]" ;
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

}
