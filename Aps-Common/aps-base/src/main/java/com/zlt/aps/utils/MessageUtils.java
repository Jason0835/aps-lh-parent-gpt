package com.zlt.aps.utils;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.msg.message.api.IMessageCenterRemoteService;
import com.zlt.msg.message.api.IMsgBillTemplateRefRemoteService;
import com.zlt.msg.message.api.IMsgTemplateRemoteService;
import com.zlt.msg.message.domain.entity.MsgMessage;
import com.zlt.msg.message.domain.entity.MsgTemplate;
import com.zlt.msg.message.domain.vo.MessageContext;
import com.zlt.msg.message.enums.MsgChannelEnums;
import com.zlt.msg.message.enums.MsgSourceEnums;
import com.zlt.msg.message.enums.MsgStatusEnums;
import com.zlt.msg.message.enums.MsgTypeEnums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息工具类
 *
 * <p>提供消息发送的核心功能，支持多种消息类型和渠道</p>
 *
 * @author 16799
 * @since 1.0.0
 */
@Component
public class MessageUtils {

    private static final String Z_K_H = "\\{}";

    @Autowired
    private IMessageCenterRemoteService messageCenterRemoteService;
    @Autowired
    private IMsgBillTemplateRefRemoteService billTemplateRefRemoteService;
    @Autowired
    private IMsgTemplateRemoteService templateRemoteService;

    /**
     * 添加站内消息（根据单据类型）
     *
     * <p>使用默认消息类型：NOTICE(0-通知消息)</p>
     *
     * @param billTypeCode 单据类型编码
     * @param msgChannel 消息渠道
     * @param paramValues 消息参数值
     * @param receivedBys 接收人数组
     * @param context 单据上下文
     */
    public void addInnerSiteMessageByType(String billTypeCode, String msgChannel, String[] paramValues,
                                          String[] receivedBys, MessageContext context) {
        addInnerSiteMessageByType(billTypeCode, msgChannel, paramValues, receivedBys, context,
                MsgTypeEnums.NOTICE.getCode());
    }

    /**
     * 添加站内消息（根据消息模板）
     *
     * <p>使用默认消息类型：NOTICE(0-通知消息)</p>
     *
     * @param templateCode 消息模板编码
     * @param msgChannel 消息渠道
     * @param paramValues 消息参数值
     * @param receivedBys 接收人数组
     * @param context 单据上下文
     */
    public void addInnerSiteMessageByTempalte(String templateCode, String msgChannel, String[] paramValues,
                                              String[] receivedBys, MessageContext context) {
        addInnerSiteMessageByTempalte(templateCode, msgChannel, paramValues, receivedBys, context,
                MsgTypeEnums.NOTICE.getCode());
    }


    /**
     * 添加自定义消息（根据单据类型，支持自定义消息类型）
     *
     * @param billTypeCode 单据类型编码
     * @param msgChannel 消息渠道
     * @param paramValues 消息参数值
     * @param receivedBys 接收人数组
     * @param context 单据上下文
     * @param msgType 消息类型（支持：0-通知消息, 1-工作任务, 2-预警消息）
     */
    public void addInnerSiteMessageByType(String billTypeCode, String msgChannel, String[] paramValues,
                                          String[] receivedBys, MessageContext context, String msgType) {
        if (StringUtils.isEmpty(billTypeCode)
                || StringUtils.isEmpty(paramValues) || StringUtils.isEmpty(receivedBys)) {
            throw new CustomException("单据类型或消息渠道或参数值或接收人员为空！");
        }
        if (StringUtils.isEmpty(msgChannel)) {
            msgChannel = MsgChannelEnums.SYSTEM.getCode();
        }

        // 验证消息类型是否有效
        if (StringUtils.isEmpty(msgType)) {
            msgType = MsgTypeEnums.NOTICE.getCode();
        } else {
            validateMsgType(msgType);
        }

        String templateCode = billTemplateRefRemoteService.selectMsgTemplateCode(billTypeCode, msgChannel);
        addInnerSiteMessageByTempalte(templateCode, msgChannel, paramValues, receivedBys, context, msgType);
    }

    /**
     * 添加自定义消息（根据消息模板，支持自定义消息类型）,已自定义好消息内容
     *
     * @param templateCode 消息模板编码
     * @param msgChannel 消息渠道
     * @param receivedBys 接收人数组
     * @param context 单据上下文
     * @param msgType 消息类型（支持：0-通知消息, 1-工作任务, 2-预警消息）
     */
    public void addInnerSiteMessageByTemplateContent(String templateCode, String msgChannel,
                                              String[] receivedBys, MessageContext context, String msgType, String msgContent) {
        if (StringUtils.isEmpty(templateCode)
                || StringUtils.isEmpty(receivedBys)) {
            throw new CustomException("消息模板或接收人员为空！");
        }

        // 验证消息类型是否有效
        if (StringUtils.isEmpty(msgType)) {
            msgType = MsgTypeEnums.NOTICE.getCode();
        } else {
            validateMsgType(msgType);
        }

        // 1. 获取系统模板
        MsgTemplate template = templateRemoteService.getTemplateInfo(templateCode);
        if (template == null) {
            throw new CustomException("消息模板不存在：" + templateCode);
        }


        // 2. 替换消息内容参数
        //String msgContent = buildMessageContent(template.getContent(), paramValues);

        // 3. 获取系统编码和发送人员
        String systemCode = getSystemCode(context);
        String sendBy = getSendBy(context);

        // 4. 根据接收人组装消息列表
        List<MsgMessage> messageList = buildMessageList(
                template, msgContent, msgChannel, msgType,
                systemCode, sendBy, receivedBys, context
        );

        if (StringUtils.isNotEmpty(messageList)) {
            messageCenterRemoteService.saveMessage(messageList);
        }
    }

    /**
     * 添加自定义消息（根据消息模板，支持自定义消息类型）
     *
     * @param templateCode 消息模板编码
     * @param msgChannel 消息渠道
     * @param paramValues 消息参数值
     * @param receivedBys 接收人数组
     * @param context 单据上下文
     * @param msgType 消息类型（支持：0-通知消息, 1-工作任务, 2-预警消息）
     */
    public void addInnerSiteMessageByTempalte(String templateCode, String msgChannel, String[] paramValues,
                                              String[] receivedBys, MessageContext context, String msgType) {
        if (StringUtils.isEmpty(templateCode) || StringUtils.isEmpty(paramValues)
                || StringUtils.isEmpty(receivedBys)) {
            throw new CustomException("消息模板或参数值或接收人员为空！");
        }

        // 验证消息类型是否有效
        if (StringUtils.isEmpty(msgType)) {
            msgType = MsgTypeEnums.NOTICE.getCode();
        } else {
            validateMsgType(msgType);
        }

        // 1. 获取系统模板
        MsgTemplate template = templateRemoteService.getTemplateInfo(templateCode);
        if (template == null) {
            throw new CustomException("消息模板不存在：" + templateCode);
        }


        // 2. 替换消息内容参数
        String msgContent = buildMessageContent(template.getContent(), paramValues);

        // 3. 获取系统编码和发送人员
        String systemCode = getSystemCode(context);
        String sendBy = getSendBy(context);

        // 4. 根据接收人组装消息列表
        List<MsgMessage> messageList = buildMessageList(
                template, msgContent, msgChannel, msgType,
                systemCode, sendBy, receivedBys, context
        );

        if (StringUtils.isNotEmpty(messageList)) {
            messageCenterRemoteService.saveMessage(messageList);
        }
    }

    /**
     * 构建消息内容（替换模板中的占位符）
     *
     * @param templateContent 模板内容
     * @param paramValues 参数值数组
     * @return 替换后的消息内容
     */
    private String buildMessageContent(String templateContent, String[] paramValues) {
        if (StringUtils.isEmpty(templateContent)) {
            return "";
        }

        String msgContent = templateContent;
        if (StringUtils.isNotEmpty(paramValues) && StringUtils.isNotEmpty(msgContent)) {
            for (String oneValue : paramValues) {
                msgContent = msgContent.replaceFirst(Z_K_H, oneValue);
            }
        }
        return msgContent;
    }

    /**
     * 构建消息列表
     *
     * @param template 消息模板
     * @param msgContent 消息内容
     * @param msgChannel 消息渠道
     * @param msgType 消息类型
     * @param systemCode 系统编码
     * @param sendBy 发送人
     * @param receivedBys 接收人数组
     * @param context 消息上下文
     * @return 消息列表
     */
    private List<MsgMessage> buildMessageList(MsgTemplate template, String msgContent,
                                              String msgChannel, String msgType,
                                              String systemCode, String sendBy,
                                              String[] receivedBys, MessageContext context) {
        List<MsgMessage> messageList = new ArrayList<>();

        for (String receivedBy : receivedBys) {
            MsgMessage message = new MsgMessage();

            // 设置消息基本信息
            message.setMsgSource(MsgSourceEnums.INNER_SITE.getCode());
            // 使用传入的消息类型
            message.setMsgType(msgType);
            message.setMsgTitle(template.getTitle());
            message.setMsgContent(msgContent);
            message.setMsgStatus(MsgStatusEnums.NO_READ.getCode());
            message.setMsgChannel(StringUtils.isEmpty(msgChannel) ? MsgChannelEnums.SYSTEM.getCode() : msgChannel);
            message.setSendBy(StringUtils.isEmpty(sendBy) ? SecurityUtils.getUsername() : sendBy);
            message.setSendTime(DateUtils.getNowDate());
            message.setReceivedBy(receivedBy);
            message.setCreateBy(SecurityUtils.getUsername());
            message.setUpdateBy(message.getCreateBy());
            message.setSystemCode(systemCode);

            // 设置单据相关信息
            if (context != null) {
                message.setBillTypeCode(StringUtils.isEmpty(context.getBillTypeCode()) ? null : context.getBillTypeCode());
                message.setBillTypeName(StringUtils.isEmpty(context.getBillTypeName()) ? null : context.getBillTypeName());
                message.setBillUrl(StringUtils.isEmpty(context.getBillUrl()) ? null : context.getBillUrl());
                message.setBillNo(StringUtils.isEmpty(context.getBillNo()) ? null : context.getBillNo());
                message.setBillId(context.getBillId() == null ? null : context.getBillId());
                message.setBillContext(StringUtils.isEmpty(context.getBillContext()) ? null : context.getBillContext());
            }

            messageList.add(message);
        }

        return messageList;
    }

    /**
     * 验证消息类型是否有效
     *
     * @param msgType 消息类型
     * @throws CustomException 如果消息类型无效
     */
    private void validateMsgType(String msgType) {
        boolean isValid = false;

        for (MsgTypeEnums typeEnum : MsgTypeEnums.values()) {
            if (typeEnum.getCode().equals(msgType)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new CustomException("无效的消息类型：" + msgType +
                    "，有效值为：" + getValidMsgTypes());
        }
    }

    /**
     * 获取有效的消息类型列表
     *
     * @return 有效消息类型字符串
     */
    private String getValidMsgTypes() {
        StringBuilder sb = new StringBuilder();
        for (MsgTypeEnums typeEnum : MsgTypeEnums.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(typeEnum.getCode()).append("(").append(typeEnum.getInfo()).append(")");
        }
        return sb.toString();
    }

    /**
     * 获取发送人员
     *
     * @param context 消息上下文
     * @return 发送人
     */
    private String getSendBy(MessageContext context) {
        String sendBy;
        if (context != null && StringUtils.isNotEmpty(context.getSendBy())) {
            sendBy = context.getSendBy();
        } else {
            sendBy = SecurityUtils.getUsername();
        }
        return sendBy;
    }

    /**
     * 获取系统编码
     *
     * @param context 消息上下文
     * @return 系统编码
     */
    private String getSystemCode(MessageContext context) {
        String systemCode;
        if (context != null && StringUtils.isNotEmpty(context.getSystemCode())) {
            systemCode = context.getSystemCode();
        } else {
            // 注：配置信息获取先绕到消息中心后台再转到配置取数，解决系统管理作为发起方不能自己调自己的问题
            systemCode = messageCenterRemoteService.selectConfigByKey("sys.system.code");
        }
        return systemCode;
    }
}