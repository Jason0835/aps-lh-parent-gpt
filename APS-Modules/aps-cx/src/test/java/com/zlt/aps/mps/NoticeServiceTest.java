package com.zlt.aps.mps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.mail.api.EmailMessage;
import com.ruoyi.mail.api.service.IEmailRemoteService;
import com.zlt.aps.mps.service.INoticeService;

@SpringBootTest
public class NoticeServiceTest {

    @Autowired
    private IEmailRemoteService iEmailRemoteService;

    @Autowired
    private INoticeService iNoticeService;
    
    @Test
    public void noticeTest() throws IOException {
        iNoticeService.unfinishedSchedule();
    }

//    @Test
    public void test() throws IOException {
        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setReceiverEmail("283051485@qq.com");
        emailMessage.setTitle("测试邮件");
        emailMessage.setContent("测试内容g");

        emailMessage.setAttachmentNameList(Collections.singletonList("附件1.txt"));
        InputStream inputStream = NoticeServiceTest.class.getResourceAsStream("/1.txt");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024]; // 缓冲区大小

        // 读取输入流并写入缓冲区
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        emailMessage.setAttachmentList(Collections.singletonList(buffer.toByteArray()));
        AjaxResult result = iEmailRemoteService.sendMail(emailMessage);
        result.toString();
    }

}
