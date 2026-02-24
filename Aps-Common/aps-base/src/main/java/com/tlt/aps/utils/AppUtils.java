package com.tlt.aps.utils;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.StringUtil;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * app工具类
 */
public class AppUtils {
    private static final Logger logger = LoggerFactory.getLogger(AppUtils.class);

    /**
     * Ajax成功请求
     */
    public static final Integer AJAX_RESULT_SUCCESS = 200;

    /**
     * 默认编码最短长度
     */
    public static final int GENERATE_CODE_LENGTH = 2;

    /**
     * 默认编码的初始编码(单字符实现)
     */
    public static final String GENERATE_CODE_INIT_CODE = "1";

	private static final int BATCH_SIZE = 500;

    /**
     * 根据deptId获取部门名称
     * @param deptId
     * @param sysDeptMap
     * @return
     */
    public static String getSysDeptName(Long deptId, Map<Long, SysDept> sysDeptMap){
        if (deptId == null) {
            return "";
        }
        if (sysDeptMap.get(deptId) != null) {
            return sysDeptMap.get(deptId).getDeptName();
        } else {
            return "";
        }
    }

    /**
     *
     * @param username 用户名
     * @param sysUserMap 用户信息
     * @return
     */
    public static String getUserNickName(String username, Map<String, SysUser> sysUserMap){
        if (StringUtils.isBlank(username)) {
            return "";
        }
        if (sysUserMap.get(username) != null) {
            return sysUserMap.get(username).getNickName();
        } else {
            return "";
        }
    }

    /**
     * 获取用户手机
     *
     * @param username   用户名
     * @param sysUserMap 用户信息
     * @return
     */
    public static String getUserPhone(String username, Map<String, SysUser> sysUserMap) {
        if (StringUtils.isBlank(username)) {
            return "";
        }
        if (sysUserMap.get(username) != null) {
            return sysUserMap.get(username).getPhonenumber();
        } else {
            return "";
        }
    }

    /**
     * 字典List转为 String（dictValue）-String(dictLabel)映射
     * @param list 字典列表
     * @return String（dictValue）-String(dictLabel)映射
     */
    public static Map<String, String> DictListToDictMap(List<SysDictData> list) {
        if (list == null) {
            return new HashMap<>();
        }
        Map<String, String> categoryMap = new HashMap<>();
        for (SysDictData sysDictData : list) {
            if(StringUtils.isNotEmpty(sysDictData.getDictValue()) && !categoryMap.containsKey(sysDictData.getDictValue())){
                categoryMap.put(sysDictData.getDictValue(), sysDictData.getDictLabel());
            }
        }
        return  categoryMap;
    }


    /**
     * 计算时间差距（小时）
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 停机时长小时数
     */
    public static Double computeStopHour(Date beginTime, Date endTime) {
        Double minute = computeMinute(beginTime, endTime);
        return minute == null ? null : minute / 60;
    }

    /**
     * 计算时间差距（分钟）
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 差距的分钟数
     */
    public static Double computeMinute(Date beginTime, Date endTime) {
        Double second = computeSecond(beginTime, endTime);
        return second == null ? null : second / 60;
    }

    /**
     * 计算时间差距（秒）
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 差距的秒数
     */
    public static Double computeSecond(Date beginTime, Date endTime) {
        if (beginTime != null && endTime != null) {
            double mills = Math.abs(DateUtils.getDiffMillTime(endTime, beginTime));
            return mills / 1000;
        }
        return null;
    }

//    // 获取bean的某个属性值
//    public static String getStringProperty(R r, String fieldName) throws Exception {
//        // 获取Bean的某个属性的描述符
//        PropertyDescriptor proDescriptor = new PropertyDescriptor(fieldName, R.class);
//        // 获得用于读取属性值的方法
//        Method methodGet = proDescriptor.getReadMethod();
//        // 读取属性值
//        Object objValue = methodGet.invoke(r);
//        if (ObjectUtils.isEmpty(objValue) || "null".equals(objValue)){
//            return null;
//        }else {
//            return objValue.toString();
//        }
//    }

    /**
     * 判断Ajax请求是否成功
     *
     * @param ajaxResult Ajax请求
     * @return 结果
     */
    public static boolean checkAjaxSuccess(AjaxResult ajaxResult) {
        return ajaxResult != null && AJAX_RESULT_SUCCESS.equals(ajaxResult.get(AjaxResult.CODE_TAG));
    }

    /**
     * 根据结果数返回操作结果
     *
     * @param num 结果数
     * @return 结果
     */
    public static AjaxResult numToAjaxResult(int num) {
        return num > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 都不为空时计算两者差值，否则返回null
     * @param before 被减数
     * @param after 减数
     * @return 差值或null
     */
    public static Integer valueDiff(Integer before, Integer after) {
        if(before==null||after==null){
            return null;
        }
        return before-after;
    }

    /**
     * 获取包装类对象总和(空对象等于0)
     *
     * @param nums 包装类
     * @return 总和
     */
    public static Integer sum(Integer... nums){
        int sum=0;
        for (Integer num : nums) {
            if(num!=null){
                sum+=num;
            }
        }
        return sum;
    }

    /**
     * 比较前者是否大于后者
     *
     * @param before 前者
     * @param after 后者
     * @return 前者是否大于后者的结果
     */
    public static boolean compare(Integer before, Integer after) {
        Integer defaultA=before==null?0:before;
        Integer defaultB=after==null?0:after;
        return defaultA.compareTo(defaultB)>0;
    }

    /**
     * App通用回显字段
     *
     * @param list 列表数据
     * @param format 表达式数组
     */
    public static void formatData(List<?> list, String[] format) {
        try {
            QueryFormulaUtil.execFormula(list, format);
        } catch (QueryExprException e) {
            throw new RuntimeException(e);
        }
    }


	/**
	 * 分页回显字段
	 *
	 * @param list   列表数据
	 * @param format 表达式数据
	 */
	public static void formatPageData(List<?> list, String[] format) {
		if (CollectionUtils.isEmpty(list) || PubUtil.isEmpty(format)) {
			return;
		}
		try {
			int startIndex = 0;
			int endIndex;
			while (startIndex < list.size()) {
				endIndex = Math.min(startIndex + BATCH_SIZE, list.size());
				List<?> echoList = list.subList(startIndex, endIndex);
				QueryFormulaUtil.execFormula(echoList, format);
				startIndex = endIndex;
			}
		} catch (QueryExprException e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * 根据传入的code，补充0至length的长度
     *
     * @param code 编号
     * @param length 长度
     * @return
     */
    public static String getCodeByLength(Long code, Integer length) {
        if (code == null || length == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(code.toString());
        result.reverse();
        while (result.length() < length) {
            result.append("0");
        }
        return result.reverse().toString();
    }

    /**
     * 根据分钟数，生成对应 小时:分钟
     *
     * @param minute 分钟数
     * @return 小时:分钟
     */
    public static String getHourAndMinute(Long minute) {
        if (minute == null) {
            return "0:0";
        }
        return (minute / 60) + ":" + (minute % 60);
    }

    /**
     * 安全合并两个BigDecimal的值
     *
     * @param decimal1 decimal1
     * @param decimal2 decimal2
     * @return 合并结果
     */
    public static BigDecimal addBigDecimal(BigDecimal decimal1, BigDecimal decimal2) {
        if (decimal1 == null && decimal2 == null) {
            return null;
        }
        if (decimal1 != null && decimal2 != null) {
            return decimal1.add(decimal1);
        }
        return decimal1 == null ? decimal2 : decimal1;
    }

    /**
     * 获取最小的时间 忽略年月日，仅比较时分秒
     *
     * @param date1 时间1
     * @param date2 时间2
     * @return 最小的时间
     */
    public static Date minDateIgnoreYearMonthDay(Date date1, Date date2) {
        if (date1 == null) {
            return date2;
        }
        if (date2 == null) {
            return date1;
        }
        //时间1忽略年月日
        Calendar c1 = Calendar.getInstance();
        c1.setTime(date1);
        c1.set(0, 0, 0);

        //时间2忽略年月日
        Calendar c2 = Calendar.getInstance();
        c2.setTime(date2);
        c2.set(0, 0, 0);

        if (c1.getTime().before(c2.getTime())) {
            return date1;
        }
        return date2;
    }

    /**
     * 获取较大的字符串
     *
     * @param str1 字符1
     * @param str2 字符2
     * @return 较大的String类型
     */
    public static String maxString(String str1, String str2) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        return str1.compareTo(str2) > 0 ? str1 : str2;
    }

    /**
     * 根据已有编号的后续数字位，叠加编号，默认填充前置0到两位
     *
     * @param code 已有编号
     * @return 叠加后的结果
     */
    public static String generateCode(String code) {
        return generateCode(code, GENERATE_CODE_LENGTH, GENERATE_CODE_INIT_CODE);
    }

    /**
     * 根据已有编号的后续数字位，叠加编号，长度不足填充前置0到指定长度
     *
     * @param code 已有编号
     * @param len  长度
     * @param initCode 初始编码(目前仅单字符实现)
     * @return 结果
     */
    public static String generateCode(String code, int len, String initCode) {
        if (StringUtils.isBlank(code)) {
            StringBuilder builder = new StringBuilder();
            while (--len > 0) {
                builder.append('0');
            }
            builder.append(initCode);
            return builder.toString();
        }
        // 记录前置符号
        String prefix = "";
        // 从后往前取出数字位
        StringBuilder numberStr = new StringBuilder();
        char[] chars = code.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            int j = code.charAt(i);
            if ('0' <= j && '9' >= j) {
                numberStr.append(code.charAt(i));
            } else {
                prefix = code.substring(0, i + 1);
                break;
            }
        }

	    // 如果全是数字，总长度不应该小于已有串
	    if (numberStr.length() == code.length()) {
		    len = Math.max(code.length(), len);
	    }

        // 计算对应数值
        if (numberStr.length() <= 0) {
            numberStr.append(initCode);
        }
        long num = Long.parseLong(numberStr.reverse().toString());

        // 填充到指定长度
        String data = prefix + (num + 1);
        StringBuilder builder = new StringBuilder(data);
        while (builder.length() < len) {
            builder.insert(0, '0');
        }
        return builder.toString();
    }

    /**
     * 根据 lang_json回显列表的组织名称字段
     *
     * @param list 携带langJson、orgId、orgName字段的列表
     */
    public static void formatOrgNameByLangJson(List<?> list) {
	    formatByLangJson(list, "langJson", "orgId", "orgName");
    }

	/**
	 *
	 *
	 * @param list 携带langJson、orgId、orgName字段的列表
	 */
	/**
	 * 根据lang_json回显列表的名称字段
	 * @param list 携带对应字段的列表
	 * @param langJsonName langJson对应字段名
	 * @param source key对应的字段名称
	 * @param target value对应的字段名词
	 */
	public static void formatByLangJson(List<?> list, String langJsonName, String source, String target) {
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		if (StringUtils.isBlank(langJsonName) || StringUtils.isBlank(source) || StringUtils.isBlank(target)) {
			return;
		}
		Locale locale = SecurityUtils.getUserLang();
		if (locale == null) {
			return;
		}
		for (Object object : list) {
			try {
				Object langJson = ReflectUtils.getFieldValue(object, langJsonName);
				if (langJson == null) {
					continue;
				}
				String strLangJson = String.valueOf(langJson);
				if (StringUtils.isBlank(strLangJson)) {
					continue;
				}

				Object orgId = ReflectUtils.getFieldValue(object, source);
				if (orgId == null) {
					continue;
				}

				String localeName = com.ruoyi.common.utils.StringUtils.getLocaleName(String.valueOf(langJson), locale, orgId.toString());

				ReflectUtils.setFieldValue(object, target, localeName);
			} catch (Exception e) {
				logger.error("根据json回显名称报错");
			}
		}
	}

	/**
	 * app出入库，校验单据权限
	 *
	 * @param entity  对应实体
	 * @param wrapper 携带唯一键的wrapper
	 * @param mapper  对应mapper
	 * @param i18Msg  国际化错误提示
	 * @param <T>     对应泛型
	 */
	public static <T extends BaseEntity> void checkScope(BaseEntity entity, LambdaQueryWrapper<T> wrapper, CommBaseMapper<T> mapper, String i18Msg) {
		if (entity == null || entity.getParams() == null || wrapper == null || mapper == null || StringUtils.isBlank(i18Msg)) {
			return;
		}

		Object dataScope = entity.getParams().get("areaDataScope");
		if (dataScope instanceof String) {
			String scope = (String) dataScope;
			if(StringUtils.isBlank(scope)){
				return;
			}

			wrapper.apply(" 1=1 " + scope);
			if (mapper.selectCount(wrapper) <= 0) {
				throw new RuntimeException(I18nUtil.getMessage(i18Msg));
			}

		}
	}
	
    /**
     * 获取国际化标识
     * @return
     */
    public static Locale getLocale() {
        return org.springframework.util.StringUtils.parseLocale("zh_CN");
    }

    /**
     * 获取字典值
     * @param value 值
     * @param map  字典Map
     * @return label
     */
    public static String getDictLabel(String value,Map<String,String> map) {
        if (PubUtil.isEmpty(value)  || PubUtil.isEmpty(map)){
            return "";
        }

        if (!map.containsKey(value)){
            return "";
        }
        return map.get(value);
    }

    /**
     * 将驼峰字段转译成数据库字段
     * @param field 驼峰字段
     * @return 数据库字段
     */
    public static String transCamelCase(String field){
        if (StringUtil.isEmptyWithTrim(field)){
            return field;
        }
        return field.replaceAll("(.)(\\p{Upper})","$1_$2").toLowerCase();
    }

    /**
     * 将null 字符串转成 空
     * @param input
     * @return
     */
    public static String formatNull(String input) {
       return StringUtils.isNotEmpty(input) ? input : "";
    }
}
