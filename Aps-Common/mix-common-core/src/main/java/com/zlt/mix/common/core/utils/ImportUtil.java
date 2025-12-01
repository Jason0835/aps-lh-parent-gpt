package com.zlt.mix.common.core.utils;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.enums.ImportErrorValueEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * 导入工具类
 */
@Slf4j
public class ImportUtil {

    /**
     * 导出时字典缓存
     */
    private static ThreadLocal<Map<String, String>> dictDataCach = new ThreadLocal<>();

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
        for (int i = 0; i < fields.length; i++) {//遍历
            try {
                //得到属性
                Field field = fields[i];
                //打开私有访问
                field.setAccessible(true);
                //获取属性值
                Object value = field.get(obj);
                String valueStr = (value == null ? "" : String.valueOf(value));
                ImportValidated validated = field.getAnnotation(ImportValidated.class);
                if (validated == null) {
                    continue;
                }
                Excel excel = field.getAnnotation(Excel.class);
                String result ="";
                Class<?> fieldType = field.getType();
                if (Date.class == fieldType) {
                    Date date=(Date)value;
                    result = validatedDateValue(row, date, validated, excel, obj);
                }else{
                    //一些过长数字读取时会转为科学计数显示，在此转换为正常的数字
                    if((Double.class == fieldType || Integer.class == fieldType || Long.class == fieldType || BigDecimal.class == fieldType)
                        && valueStr.indexOf("E")>=0){
                        //当valueStr值为最大值时，说明解析失败不应转为正常数值，在次进一步过滤
                        if(!(valueStr.equals(ImportErrorValueEnum.DOUBLE_VALUE.getNumber().toString()) ||
                                valueStr.equals(ImportErrorValueEnum.INTEGER_VALUE.getNumber().toString()) ||
                                valueStr.equals(ImportErrorValueEnum.LONG_VALUE.getNumber().toString()) ||
                                valueStr.equals(ImportErrorValueEnum.FLOAT_VALUE.getNumber().toString()) ||
                                valueStr.equals(ImportErrorValueEnum.BIGDECIMAL_VALUE.getNumber().toString()))){
                            BigDecimal realValue=  new BigDecimal(valueStr);
                            valueStr=realValue.toPlainString();
                        }
                    }
                     result = validatedValue(row, valueStr, validated, excel, obj);
                }

                if (StringUtils.isNotBlank(result)) {
                    list.add(new ImportErrorLog(importLogId, row, result));
                }
            } catch (Exception e) {
                log.error("验证导入属性错误", e.getMessage());
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
    private static String validatedValue(Integer row, String value, ImportValidated validated, Excel excel, Object obj) throws ParseException {
        row = (row == null ? 0 : row);
        value = (StringUtils.isBlank(value)) ? "" : value.trim();
        String name = validated.name();
        String excelName = excel.name();
        String importName = excel.importName();
        if (StringUtils.isEmpty(name)) {
            name = StringUtils.isEmpty(importName) ? excelName : importName;
        }
        name = (StringUtils.isBlank(name) ? "" : I18nUtil.getMessage(name));

        //不能为空
        if (validated.required() && StringUtils.isBlank(value) ) {
            //无法解析输入的值
            String dictType = "".equals(validated.dictType()) ? excel.dictType() : validated.dictType();
            if (StringUtils.isNotEmpty(dictType)){
                String message = I18nUtil.getMessage("import.validated.notDict");
                return String.format(message, row, name);
            }
            // 必须输入整数
            if (validated.digits()) {
                String message = I18nUtil.getMessage("import.validated.digits");
                return String.format(message, row, name);
            }
            // 输入的值不是数字类型
            if (validated.number()) {
                String message = I18nUtil.getMessage("import.errorValueEnum.message.doubleValue");
                return String.format(message, row, name);
            }
            // 必须输入0/整数
            if (validated.isInteger()) {
                String message = I18nUtil.getMessage("import.validated.isInteger");
                return String.format(message, row, name);
            }
            String message = I18nUtil.getMessage("import.validated.required");
            return String.format(message, row, name);
        }
        //必须输入字母、数字以及英文字符
        if (validated.isCode() && !MixCommonUtil.isCode(value) && StringUtils.isNotBlank(value)) {
            String message = I18nUtil.getMessage("import.validated.isCode");
            return String.format(message, row, name);
        }
        //输入的值不是数字类型
        if ((validated.number() && !MixCommonUtil.isNumber(value) && StringUtils.isNotBlank(value)) || (validated.number() && isMaxValue(value))) {
            String message = I18nUtil.getMessage("import.errorValueEnum.message.doubleValue");
            return String.format(message, row, name);
        }
        //请输入非负整数
        if ((validated.digits() && !MixCommonUtil.isInteger(value) && StringUtils.isNotBlank(value)) || (validated.digits() && isMaxValue(value))) {
            String message = I18nUtil.getMessage("import.validated.digits");
            return String.format(message, row, name);
        }
        // 必须输入0/整数
        if ((validated.isInteger() && !MixCommonUtil.isInteger(value) && StringUtils.isNotBlank(value)) || (validated.isInteger() && isMaxValue(value))) {
            String message = I18nUtil.getMessage("import.validated.isInteger");
            return String.format(message, row, name);
        }
        //输入的值必须符合颜色表达式格式，例：#000000
        if (validated.colorCode() && !MixCommonUtil.isColorCode(value)) {
            String message = I18nUtil.getMessage("import.validated.colorCode");
            return String.format(message, row, name);
        }
        //大于最大长度
        int maxLength = validated.maxLength();
        if (value.length() > maxLength) {
            String message = I18nUtil.getMessage("import.validated.maxLength");
            String valueStr=maxLength+"";
            if(valueStr.indexOf("E")>=0){
                BigDecimal realValue=  new BigDecimal(maxLength);
                valueStr=realValue.toPlainString();
            }
            return String.format(message, row, name, valueStr);
        }
        //小于最小长度
        int minLength = validated.minLength();
        if (value.length() < minLength) {
            String message = I18nUtil.getMessage("import.validated.minLength");
            return String.format(message, row, name, minLength);
        }
        //大于最大值
        double max = validated.max();
        if (max != Double.MAX_VALUE && StringUtils.isNotBlank(value)) {
            if (!MixCommonUtil.isNumber(value)) {
                String message = I18nUtil.getMessage("import.validated.number");
                return String.format(message, row, name);
            } else if (MixCommonUtil.isNumber(value) && Double.parseDouble(value) > max) {
                String message = I18nUtil.getMessage("import.validated.max");
                String valueStr=max+"";
                if(valueStr.indexOf("E")>=0){
                    BigDecimal realValue=  new BigDecimal(max);
                    valueStr=realValue.toPlainString();
                }
                return String.format(message, row, name, valueStr);
            }
        }
        //小于最小值
        double min = validated.min();
        if (min != Double.MIN_VALUE && StringUtils.isNotBlank(value)) {
            if (!MixCommonUtil.isNumber(value)) {
                String message = I18nUtil.getMessage("import.validated.number");
                return String.format(message, row, name);
            } else if (MixCommonUtil.isNumber(value) && Double.parseDouble(value) < min) {
                String message = I18nUtil.getMessage("import.validated.min");
                return String.format(message, row, name, min);
            }
        }
        //字典项翻译
        String dictType = "".equals(validated.dictType()) ? excel.dictType() : validated.dictType();
        if (StringUtils.isNotEmpty(dictType)) {
            try {
                ExcelUtil.convertByDictValueUseMap4ValueCheck(dictDataCach, dictType);
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
     * @param value
     * @return
     */
    private static boolean isMaxValue(String value){
        boolean isMaxValue=false;
        if (value.equals(ImportErrorValueEnum.INTEGER_VALUE.getNumber().toString())) {
            return isMaxValue=true;
        }
        if (value.equals(ImportErrorValueEnum.DOUBLE_VALUE.getNumber().toString())) {
            return isMaxValue=true;
        }
        if (value.equals(ImportErrorValueEnum.LONG_VALUE.getNumber().toString())) {
            return isMaxValue=true;
        }
        if (value.equals(ImportErrorValueEnum.FLOAT_VALUE.getNumber().toString())) {
            return isMaxValue=true;
        }
        if (value.equals(ImportErrorValueEnum.BIGDECIMAL_VALUE.getNumber().toString())) {
            return isMaxValue=true;
        }
        return isMaxValue;
    }


    /**
     * @param row       行数
     * @param value     字段值
     * @param validated
     * @return
     */
    private static String validatedDateValue(Integer row, Date value, ImportValidated validated, Excel excel, Object obj) throws ParseException {
        row = (row == null ? 0 : row);
        String name = validated.name();
        String excelName = excel.name();
        String importName = excel.importName();
        if (StringUtils.isEmpty(name)) {
            name = StringUtils.isEmpty(importName) ? excelName : importName;
        }
        name = (StringUtils.isBlank(name) ? "" : I18nUtil.getMessage(name));
        if (validated.required() && value==null ) {
            String message = I18nUtil.getMessage("import.validated.required");
            return String.format(message, row, name);
        }
        if(ObjectUtils.isNotEmpty(value)){
            String dates="";
            try{
                dates= DateUtils.parseDateToStr("yyyy-MM-dd",value);
            }catch (Exception e){ }
            if("1970-01-01".equals(dates)){
                String msgKey = "import.validated.date";
                //判断是否精确到毫秒
                if ("yyyy-MM-dd HH:mm:ss".equals(excel.dateFormat())) {
                    msgKey = "import.validated.date.second";
                }
                String message = I18nUtil.getMessage(msgKey);
                return String.format(message, row, name);
            }
        }
        if (validated.required() && validated.date() ) {
            String dates = "";
            try {
                dates = DateUtils.parseDateToStr("yyyy-MM-dd", value);
            } catch (Exception e) {
            }
            if ("1970-01-01".equals(dates)) {
                String msgKey = "import.validated.date";
                //判断是否精确到毫秒
                if ("yyyy-MM-dd HH:mm:ss".equals(excel.dateFormat())) {
                    msgKey = "import.validated.date.second";
                }
                String message = I18nUtil.getMessage(msgKey);
                return String.format(message, row, name);
            }
            try {
                String datestr = DateUtils.parseDateToStr("yyyy/MM/dd", value);
                if (datestr.split("/")[0].length() > 4) {
                    String msgKey = "import.validated.date";
                    //判断是否精确到毫秒
                    if ("yyyy-MM-dd HH:mm:ss".equals(excel.dateFormat())) {
                        msgKey = "import.validated.date.second";
                    }
                    String message = I18nUtil.getMessage(msgKey);
                    return String.format(message, row, name);
                }
            } catch (Exception e) {
                String msgKey = "import.validated.date";
                //判断是否精确到毫秒
                if ("yyyy-MM-dd HH:mm:ss".equals(excel.dateFormat())) {
                    msgKey = "import.validated.date.second";
                }
                String message = I18nUtil.getMessage(msgKey);
                return String.format(message, row, name);
            }
        }
        return null;
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
        importErrorLogs.add(importErrorLog);
    }

}
