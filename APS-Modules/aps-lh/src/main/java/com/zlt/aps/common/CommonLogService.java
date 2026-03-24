package com.zlt.aps.common;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 提供日志打印相关方法
 *
 * @author Nick
 */
@Component("CommonLogService")
@Slf4j
public class CommonLogService {

    /**
     * 排查逻辑日志保存
     **/
    public final StringBuilder scheduleLog = new StringBuilder();


    /**
     * 拼接日志的方法
     *
     * @param entity 打印实体
     * @return 返回日志字符串
     */
    public static String buildStringFromEntity(Object entity) {
        StringBuilder sb = new StringBuilder();
        buildStringFromEntityRecursive(entity, sb, "");
        return sb.toString();
    }

    /**
     * 递归拼接日志的方法
     *
     * @param msg
     */
    public void logInfo(String msg) {
        log.info(msg);
        scheduleLog.append(msg).append(ApsConstant.DIVISION);
    }

    /**
     * 打印警告日志
     *
     * @param msg
     */
    public void logWarn(String msg) {
        log.warn(msg);
        scheduleLog.append(msg).append(ApsConstant.DIVISION);
    }

    /**
     * 打印调试日志
     *
     * @param msg
     */
    public void logDebug(String msg) {
        log.debug(msg);
        scheduleLog.append(msg).append(ApsConstant.DIVISION);
    }

    /**
     * 打印错误日志
     *
     * @param msg
     * @param throwable
     */
    public void logError(String msg, Throwable throwable) {
        log.error(msg, throwable);
        scheduleLog.append(msg).append(ApsConstant.DIVISION);
        if (throwable != null) {
            scheduleLog.append(throwable.toString()).append(ApsConstant.DIVISION);
        }
    }

    /**
     * 打印信息日志
     *
     * @param format
     * @param args
     */
    public void logInfo(String format, Object... args) {
        log.info(format, args);
        // 使用 SLF4J 的格式化工具解析 {} 占位符
        String formattedMessage = MessageFormatter.arrayFormat(format, args).getMessage();
        scheduleLog.append(String.format(formattedMessage, args)).append(ApsConstant.DIVISION);
    }

    /**
     * 打印警告日志
     *
     * @param format
     * @param args
     */
    public void logWarn(String format, Object... args) {
        log.warn(format, args);
        // 使用 SLF4J 的格式化工具解析 {} 占位符
        String formattedMessage = MessageFormatter.arrayFormat(format, args).getMessage();
        scheduleLog.append(String.format(formattedMessage, args)).append(ApsConstant.DIVISION);
    }

    /**
     * 打印调试日志
     *
     * @param format
     * @param args
     */
    public void logDebug(String format, Object... args) {
        log.debug(format, args);
        // 使用 SLF4J 的格式化工具解析 {} 占位符
        String formattedMessage = MessageFormatter.arrayFormat(format, args).getMessage();
        scheduleLog.append(String.format(formattedMessage, args)).append(ApsConstant.DIVISION);
    }

    /**
     * 打印错误日志
     *
     * @param format 格式
     * @param args   参数
     */
    public void logError(String format, Object... args) {
        log.error(format, args);
        // 使用 SLF4J 的格式化工具解析 {} 占位符
        String formattedMessage = MessageFormatter.arrayFormat(format, args).getMessage();
        scheduleLog.append(String.format(formattedMessage, args)).append(ApsConstant.DIVISION);
    }

    /**
     * 构建日志格式
     *
     * @param entity 打印实体
     * @param sb     字符串 StringBuilder 类
     * @param indent 分割号
     */
    private static void buildStringFromEntityRecursive(Object entity, StringBuilder sb, String indent) {
        if (entity == null) {
            sb.append(indent).append("null").append(ApsConstant.DIVISION);
            return;
        }

        sb.append(indent).append(entity.getClass().getSimpleName()).append(" 详细信息：").append(ApsConstant.DIVISION);
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
            String fieldName = apiModelProperty != null ? apiModelProperty.value() : field.getName();
            Object fieldValue;
            try {
                fieldValue = field.get(entity);
            } catch (IllegalAccessException e) {
                fieldValue = "访问失败";
            }

            if (fieldValue != null && fieldValue.getClass().getName().startsWith(String.valueOf(entity.getClass().getPackage()))) {
                // 如果字段值是内部类对象，递归处理
                sb.append(indent).append(fieldName).append(": \n");
                buildStringFromEntityRecursive(fieldValue, sb, indent + "  ");
            } else {
                sb.append(indent).append(fieldName).append(": ").append(fieldValue).append(" ");
            }
        }
    }

    /**
     * 重置日志记录器
     */
    public void resetScheduleLogger() {
        scheduleLog.delete(0, scheduleLog.length());
        logDebug("排程日志记录器已重置");
    }

}
