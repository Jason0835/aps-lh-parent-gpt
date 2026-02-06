package com.zlt.aps.maindata.utils;

import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.utils.MessageUtils;
import com.zlt.aps.maindata.service.impl.MdmMsgTemplateUserRelServiceImpl;
import com.zlt.msg.message.domain.vo.MessageContext;
import com.zlt.msg.message.enums.MsgChannelEnums;
import com.zlt.msg.message.enums.MsgTypeEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务适配层
 *
 * <p>封装底层消息工具，提供业务友好的接口</p>
 * @author Nick
 */
@Slf4j
@Service
public class MessageServiceUtils {

    @Autowired
    private MessageUtils msgUtils;

    @Autowired
    private MdmMsgTemplateUserRelServiceImpl mdmMsgTemplateUserRelService;

    // 常量定义
    private static final String USER_NAME_SEPARATOR = ",";

    // ==================== 基础消息发送方法 ====================

    /**
     * 快速发送通知消息（类型：0-通知消息）
     *
     * <p>如果未指定接收人，则自动获取模板关联的用户</p>
     *
     * @param templateCode 消息模板代码
     * @param receiver 接收人用户名（可选，如果为空则获取模板关联用户）
     * @param params 模板参数
     */
    public void sendNotice(String templateCode, String receiver, Object... params) {
        String[] receivers = determineReceivers(templateCode, receiver, null);
        if (receivers.length == 0) {
            log.warn("未找到消息接收人，消息发送取消 - 模板: {}", templateCode);
            return;
        }

        sendMessage(templateCode, MsgTypeEnums.NOTICE.getCode(),
                MsgChannelEnums.SYSTEM.getCode(), receivers, null, params);
    }

    /**
     * 快速批量发送通知消息（类型：0-通知消息）
     *
     * @param templateCode 消息模板代码
     * @param receivers 接收人用户名数组
     * @param params 模板参数
     */
    public void sendNotice(String templateCode, String[] receivers, Object... params) {
        if (receivers == null || receivers.length == 0) {
            sendNotice(templateCode, (String) null, params);
            return;
        }

        sendMessage(templateCode, MsgTypeEnums.NOTICE.getCode(),
                MsgChannelEnums.SYSTEM.getCode(), receivers, null, params);
    }

    /**
     * 快速发送工作任务消息（类型：1-工作任务）
     *
     * <p>如果未指定接收人，则自动获取模板关联的用户</p>
     *
     * @param templateCode 消息模板代码
     * @param receiver 接收人用户名（可选，如果为空则获取模板关联用户）
     * @param billId 单据ID
     * @param billNo 单据号
     * @param url 单据跳转URL
     * @param params 模板参数
     */
    public void sendTask(String templateCode, String receiver,
                         Long billId, String billNo, String url, Object... params) {
        String[] receivers = determineReceivers(templateCode, receiver, null);
        if (receivers.length == 0) {
            log.warn("未找到工作任务接收人，消息发送取消 - 模板: {}, 单据: {}", templateCode, billNo);
            return;
        }

        MessageContext context = buildSimpleMessageContext(billId, billNo, url);
        sendMessage(templateCode, MsgTypeEnums.TASK.getCode(),
                MsgChannelEnums.SYSTEM.getCode(), receivers, context, params);
    }

    /**
     * 快速发送预警消息（类型：2-预警消息）
     *
     * <p>如果未指定接收人，则自动获取模板关联的用户</p>
     *
     * @param templateCode 消息模板代码
     * @param receiver 接收人用户名（可选，如果为空则获取模板关联用户）
     * @param params 模板参数
     */
    public void sendWarning(String templateCode, String receiver, Object... params) {
        String[] receivers = determineReceivers(templateCode, receiver, null);
        if (receivers.length == 0) {
            log.warn("未找到预警消息接收人，消息发送取消 - 模板: {}", templateCode);
            return;
        }

        sendMessage(templateCode, MsgTypeEnums.WARNING.getCode(),
                MsgChannelEnums.SYSTEM.getCode(), receivers, null, params);
    }

    /**
     * 批量发送预警消息（类型：2-预警消息）
     *
     * @param templateCode 消息模板代码
     * @param receivers 接收人用户名数组
     * @param params 模板参数
     */
    public void sendWarning(String templateCode, String[] receivers, Object... params) {
        if (receivers == null || receivers.length == 0) {
            sendWarning(templateCode, (String) null, params);
            return;
        }

        sendMessage(templateCode, MsgTypeEnums.WARNING.getCode(),
                MsgChannelEnums.SYSTEM.getCode(), receivers, null, params);
    }

    // ==================== 核心消息发送方法 ====================

    /**
     * 发送完善的指定消息到指定渠道（完整参数）
     *
     * @param templateCode 消息模板代码
     * @param msgType 消息类型
     * @param channel 消息渠道
     * @param receivers 接收人用户名数组
     * @param context 消息上下文
     * @param params 模板参数
     */
    public void sendMessage(String templateCode, String msgType, String channel,
                            String[] receivers, MessageContext context, Object... params) {
        if (receivers == null || receivers.length ==0){
            receivers = determineReceivers(templateCode, null, null);
        }
        try {
            if (StringUtils.isEmpty(templateCode) || receivers == null || receivers.length == 0) {
                log.warn("消息发送参数不完整: 模板编码templateCode={}, 接收人receivers={}", templateCode,
                        receivers != null ? Arrays.toString(receivers) : "null");
                return;
            }

            String[] strParams = convertParams(params);
            msgUtils.addInnerSiteMessageByTempalte(templateCode, channel, strParams,
                    receivers, context, msgType);

            log.debug("消息发送成功 - 模板: {}, 类型: {}, 接收人: {}, 渠道: {}",
                    templateCode, msgType, Arrays.toString(receivers), channel);
        } catch (Exception e) {
            log.error("消息发送失败 - 模板: {}, 类型: {}, 接收人: {}", templateCode, msgType,
                    receivers != null ? Arrays.toString(receivers) : "null", e);
        }
    }

    /**
     * 发送完善的指定消息到指定渠道（完整参数）
     *
     * @param templateCode 消息模板代码
     * @param msgType 消息类型
     * @param channel 消息渠道
     * @param receivers 接收人用户名数组
     * @param context 消息上下文
     */
    public void sendBatchMessage(String templateCode, String msgType, String msgContent,String channel,
                            String[] receivers, MessageContext context) {
        if (receivers == null || receivers.length ==0){
            receivers = determineReceivers(templateCode, null, null);
        }
        try {
            if (StringUtils.isEmpty(templateCode) || receivers == null || receivers.length == 0) {
                log.warn("消息发送参数不完整: 模板编码templateCode={}, 接收人receivers={}", templateCode,
                        receivers != null ? Arrays.toString(receivers) : "null");
                return;
            }

            msgUtils.addInnerSiteMessageByTemplateContent(templateCode, channel,
                    receivers, context, msgType,msgContent);

            log.debug("消息发送成功 - 模板: {}, 类型: {}, 接收人: {}, 渠道: {}",
                    templateCode, msgType, Arrays.toString(receivers), channel);
        } catch (Exception e) {
            log.error("消息发送失败 - 模板: {}, 类型: {}, 接收人: {}", templateCode, msgType,
                    receivers != null ? Arrays.toString(receivers) : "null", e);
        }
    }

    // ==================== 接收人处理逻辑 ====================

    /**
     * 确定消息接收人
     *
     * <p>逻辑：优先使用指定的接收人，如果未指定则获取模板关联用户</p>
     *
     * @param templateCode 消息模板代码
     * @param receiver 指定的接收人（单个）
     * @param defaultReceivers 默认接收人数组（备用）
     * @return 接收人数组
     */
    private String[] determineReceivers(String templateCode, String receiver, String[] defaultReceivers) {
        // 1. 如果有指定接收人，直接使用
        if (StringUtils.isNotEmpty(receiver)) {
            log.debug("使用指定接收人 - 模板: {}, 接收人: {}", templateCode, receiver);
            return new String[]{receiver};
        }

        // 2. 获取模板关联用户
        List<String> templateUsers = getTemplateAssociatedUsers(templateCode);
        if (!CollectionUtils.isEmpty(templateUsers)) {
            log.debug("使用模板关联用户 - 模板: {}, 用户数: {}", templateCode, templateUsers.size());
            return templateUsers.toArray(new String[0]);
        }

        // 3. 使用默认接收人
        if (defaultReceivers != null && defaultReceivers.length > 0) {
            log.debug("使用默认接收人 - 模板: {}, 用户数: {}", templateCode, defaultReceivers.length);
            return defaultReceivers;
        }

        // 4. 没有找到任何接收人
        log.warn("未找到任何接收人 - 模板: {}", templateCode);
        return new String[0];
    }

    /**
     * 确定批量消息接收人
     *
     * <p>逻辑：优先使用指定的接收人数组，如果为空则获取模板关联用户</p>
     *
     * @param templateCode 消息模板代码
     * @param receivers 指定的接收人数组
     * @param defaultReceivers 默认接收人数组（备用）
     * @return 接收人数组
     */
    private String[] determineBatchReceivers(String templateCode, String[] receivers, String[] defaultReceivers) {
        // 1. 如果有指定接收人数组，直接使用
        if (receivers != null && receivers.length > 0) {
            log.debug("使用指定接收人数组 - 模板: {}, 用户数: {}", templateCode, receivers.length);
            return receivers;
        }

        // 2. 获取模板关联用户
        List<String> templateUsers = getTemplateAssociatedUsers(templateCode);
        if (!CollectionUtils.isEmpty(templateUsers)) {
            log.debug("使用模板关联用户 - 模板: {}, 用户数: {}", templateCode, templateUsers.size());
            return templateUsers.toArray(new String[0]);
        }

        // 3. 使用默认接收人
        if (defaultReceivers != null && defaultReceivers.length > 0) {
            log.debug("使用默认接收人 - 模板: {}, 用户数: {}", templateCode, defaultReceivers.length);
            return defaultReceivers;
        }

        // 4. 没有找到任何接收人
        log.warn("未找到任何接收人 - 模板: {}", templateCode);
        return new String[0];
    }

    /**
     * 获取模板关联用户
     *
     * @param templateCode 消息模板代码
     * @return 模板关联的用户列表（逗号分隔的字符串转换为列表）
     */
    private List<String> getTemplateAssociatedUsers(String templateCode) {
        try {
            if (StringUtils.isEmpty(templateCode)) {
                return Collections.emptyList();
            }

            // 获取模板关联的用户映射（返回的是 Map<String, String>，value 是逗号分隔的用户名）
            Map<String, String> templateUsersMap =
                    mdmMsgTemplateUserRelService.batchGetAssociatedUsers(Collections.singletonList(templateCode));

            // 获取当前模板关联的用户字符串
            String usersStr = templateUsersMap.get(templateCode);

            // 将逗号分隔的字符串转换为列表
            return convertUserStringToList(usersStr);

        } catch (Exception e) {
            log.error("获取模板关联用户失败 - 模板: {}", templateCode, e);
            return Collections.emptyList();
        }
    }

    /**
     * 将逗号分隔的用户名字符串转换为列表
     *
     * @param usersStr 逗号分隔的用户名字符串
     * @return 用户列表
     */
    private List<String> convertUserStringToList(String usersStr) {
        if (StringUtils.isEmpty(usersStr)) {
            return Collections.emptyList();
        }

        // 分割字符串并过滤空值和空白字符
        return Arrays.stream(usersStr.split(USER_NAME_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .distinct() // 去重
                .collect(Collectors.toList());
    }

    /**
     * 获取多个模板关联用户
     *
     * @param templateCodes 消息模板代码列表
     * @return 模板代码到用户列表的映射
     */
    public Map<String, List<String>> getTemplateAssociatedUsers(List<String> templateCodes) {
        try {
            if (CollectionUtils.isEmpty(templateCodes)) {
                return Collections.emptyMap();
            }

            // 获取原始的 Map<String, String> 结果
            Map<String, String> rawResult = mdmMsgTemplateUserRelService.batchGetAssociatedUsers(templateCodes);

            // 转换为 Map<String, List<String>>
            return rawResult.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> convertUserStringToList(entry.getValue())
                    ));

        } catch (Exception e) {
            log.error("获取多个模板关联用户失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取多个模板关联用户（返回逗号分隔的字符串）
     *
     * @param templateCodes 消息模板代码列表
     * @return 模板代码到逗号分隔用户字符串的映射
     */
    public Map<String, String> getTemplateAssociatedUsersAsString(List<String> templateCodes) {
        try {
            if (CollectionUtils.isEmpty(templateCodes)) {
                return Collections.emptyMap();
            }

            return mdmMsgTemplateUserRelService.batchGetAssociatedUsers(templateCodes);

        } catch (Exception e) {
            log.error("获取多个模板关联用户失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取模板关联用户数组（直接返回数组，方便使用）
     *
     * @param templateCode 消息模板代码
     * @return 用户数组
     */
    public String[] getTemplateAssociatedUsersAsArray(String templateCode) {
        List<String> userList = getTemplateAssociatedUsers(templateCode);
        return userList.toArray(new String[0]);
    }

    // ==================== 便捷消息发送方法 ====================

    /**
     * 发送单据审批通知
     *
     * @param templateCode 消息模板代码
     * @param billId 单据ID
     * @param billNo 单据号
     * @param billType 单据类型
     * @param billUrl 单据跳转URL
     * @param params 模板参数
     */
    public void sendBillApprovalNotice(String templateCode, Long billId, String billNo,
                                       String billType, String billUrl, Object... params) {
        String[] receivers = determineReceivers(templateCode, null, null);
        if (receivers.length == 0) {
            log.warn("未找到单据审批通知接收人 - 模板: {}, 单据: {}", templateCode, billNo);
            return;
        }

        MessageContext context = buildMessageContext(
                billId, null, billType, billNo, billUrl, null, null, null
        );

        sendMessage(templateCode, MsgTypeEnums.NOTICE.getCode(),
                MsgChannelEnums.SYSTEM.getCode(), receivers, context, params);
    }

    /**
     * 发送用量偏差预警（使用模板关联用户）
     *
     * @param templateCode 消息模板代码
     * @param factoryCode 工厂代码
     * @param materialName 物料名称
     * @param deviationRate 偏差率
     * @param detailUrl 详情页URL
     */
    public void sendUsageDeviationWarning(String templateCode, String factoryCode,
                                          String materialName, String deviationRate,
                                          String detailUrl) {
        // 确定接收人（优先使用模板关联用户）
        String[] receivers = determineReceivers(templateCode, null, null);
        if (receivers.length == 0) {
            log.warn("未找到用量偏差预警接收人 - 模板: {}, 工厂: {}, 物料: {}",
                    templateCode, factoryCode, materialName);
            return;
        }

        // 构建消息上下文
        MessageContext context = buildSimpleMessageContext(null, null, detailUrl);

        // 发送预警消息
        sendWarning(templateCode, receivers, factoryCode, materialName, deviationRate);
    }

    /**
     * 发送消息到模板关联的所有用户
     *
     * @param templateCode 消息模板代码
     * @param msgType 消息类型
     * @param context 消息上下文
     * @param params 模板参数
     */
    public void sendMessageToTemplateUsers(String templateCode, String msgType,
                                           MessageContext context, Object... params) {
        // 获取模板关联用户
        String[] receivers = getTemplateAssociatedUsersAsArray(templateCode);
        if (receivers.length == 0) {
            log.warn("模板没有关联用户，消息发送取消 - 模板: {}", templateCode);
            return;
        }

        // 发送消息
        sendMessage(templateCode, msgType, MsgChannelEnums.SYSTEM.getCode(),
                receivers, context, params);
    }

    // ==================== 辅助方法 ====================

    /**
     * 将Object类型参数转换为String数组
     */
    private String[] convertParams(Object... params) {
        if (params == null || params.length == 0) {
            return new String[0];
        }

        return Arrays.stream(params)
                .map(param -> param == null ? "" : param.toString())
                .toArray(String[]::new);
    }

    /**
     * 构建简单消息上下文（仅包含基本信息）
     *
     * @param billId 单据ID
     * @param billNo 单据号
     * @param billUrl 单据跳转URL
     * @return 消息上下文
     */
    public MessageContext buildSimpleMessageContext(Long billId, String billNo, String billUrl) {
        MessageContext context = new MessageContext();
        context.setBillId(billId);
        context.setBillNo(billNo);
        context.setBillUrl(billUrl);
        return context;
    }

    /**
     * 构建完整消息上下文（包含所有字段）
     *
     * @param billId 单据ID
     * @param billTypeCode 单据类型代码
     * @param billTypeName 单据类型名称
     * @param billNo 单据号
     * @param billUrl 单据跳转URL
     * @param billContext 单据内容描述
     * @param sendBy 发送人
     * @param systemCode 系统代码
     * @return 配置好的MessageContext对象
     */
    public MessageContext buildMessageContext(Long billId, String billTypeCode,
                                              String billTypeName, String billNo,
                                              String billUrl, String billContext,
                                              String sendBy, String systemCode) {
        MessageContext context = new MessageContext();
        context.setBillId(billId);
        context.setBillTypeCode(billTypeCode);
        context.setBillTypeName(billTypeName);
        context.setBillNo(billNo);
        context.setBillUrl(billUrl);
        context.setBillContext(billContext);
        context.setSendBy(sendBy);
        context.setSystemCode(systemCode);
        return context;
    }

    // ==================== URL构建方法 ====================

    /**
     * 构建前端页面跳转URL
     *
     * @param pagePath 页面路径（如：/purchase/order/detail）
     * @param paramName 参数名
     * @param paramValue 参数值
     * @return 完整的URL字符串
     */
    public String buildFrontendUrl(String pagePath, String paramName, String paramValue) {
        return pagePath + "?" + paramName + "=" + paramValue;
    }

    /**
     * 构建带多个参数的跳转URL
     *
     * @param pagePath 页面路径
     * @param params 参数对，格式为：key1, value1, key2, value2, ...
     * @return 完整的URL字符串
     */
    public String buildFrontendUrl(String pagePath, String... params) {
        if (params == null || params.length == 0) {
            return pagePath;
        }

        StringBuilder url = new StringBuilder(pagePath);
        if (params.length > 0) {
            url.append("?");
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) url.append("&");
                url.append(params[i]).append("=");
                if (i + 1 < params.length) {
                    url.append(params[i + 1]);
                }
            }
        }
        return url.toString();
    }

    /**
     * 构建详情页跳转URL（使用ID作为参数）
     *
     * @param pagePath 详情页面路径
     * @param billId 单据ID
     * @return 完整的URL字符串
     */
    public String buildDetailUrl(String pagePath, Long billId) {
        return buildFrontendUrl(pagePath, "id", String.valueOf(billId));
    }

    /**
     * 构建详情页跳转URL（使用单据号作为参数）
     *
     * @param pagePath 详情页面路径
     * @param billNo 单据号
     * @return 完整的URL字符串
     */
    public String buildDetailUrlByBillNo(String pagePath, String billNo) {
        return buildFrontendUrl(pagePath, "billNo", billNo);
    }

    // ==================== 实用方法 ====================

    /**
     * 检查消息模板是否存在关联用户
     *
     * @param templateCode 模板代码
     * @return 是否存在关联用户
     */
    public boolean hasTemplateAssociatedUsers(String templateCode) {
        List<String> users = getTemplateAssociatedUsers(templateCode);
        return !CollectionUtils.isEmpty(users);
    }

    /**
     * 获取模板关联用户数量
     *
     * @param templateCode 模板代码
     * @return 关联用户数量
     */
    public int getTemplateAssociatedUserCount(String templateCode) {
        List<String> users = getTemplateAssociatedUsers(templateCode);
        return users.size();
    }

    /**
     * 发送消息并返回发送结果
     *
     * @param templateCode 模板代码
     * @param msgType 消息类型
     * @param receivers 接收人
     * @param params 参数
     * @return 发送是否成功
     */
    public boolean sendMessageWithResult(String templateCode, String msgType,
                                         String[] receivers, Object... params) {
        try {
            sendMessage(templateCode, msgType, MsgChannelEnums.SYSTEM.getCode(),
                    receivers, null, params);
            return true;
        } catch (Exception e) {
            log.error("消息发送失败 - 模板: {}", templateCode, e);
            return false;
        }
    }
}