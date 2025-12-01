package com.zlt.aps.ai;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.zlt.framework.utils.AuthorizationUtils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import static com.zlt.aps.ai.AiContents.DIFY_API_KEY;


@Slf4j
public class DifyFileUploader {


    /*public static void main(String[] args) {
        try {
            // 上传文件
            File file = new File("C:\\Users\\XUE\\Downloads\\微信图片_20250619153010.png");
            String fileId = uploadFile(file,DIFY_API_KEY);
            System.out.println("文件上传成功，File ID: " + fileId);

            // 使用上传的文件调用LLM
            String response = callLLMWithFile(fileId,null,DIFY_API_KEY);
            System.out.println("LLM处理结果: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /**
     * 上传文件到Dify平台
     */
    public static String uploadFile(File file,String apiKey) throws IOException {
        String boundary = "---------------------------" + UUID.randomUUID().toString();
        String lineFeed = "\r\n";

//        URL url = new URL(AiContents.UPLOAD_ENDPOINT);
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("POST");
//        connection.setDoOutput(true);
//        connection.setDoInput(true);
//        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
//        connection.setRequestProperty("Authorization", "Bearer " + AiContents.DIFY_API_KEY);

//        OutputStream outputStream = connection.getOutputStream();
        HashMap<String, Object> paramMap = new HashMap<>();
//文件上传只需将参数中的键指定（默认file），值设为文件对象即可，对于使用者来说，文件上传与普通表单提交并无区别
        paramMap.put("file", file);
        HttpRequest request = HttpUtil.createPost(AiContents.UPLOAD_ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data")
                .form(paramMap);

        String response = request.execute().body().toString();
        // 解析响应获取file_id
        JSONObject jsonResponse = JSONObject.parseObject(response.toString());
        return (String) jsonResponse.get("id");
    }

    public static File multipartFileToFile(MultipartFile multipartFile) throws IOException {
        // 创建临时文件
        File file = File.createTempFile("temp", null);
        try {
            // 将MultipartFile的内容写入临时文件
            multipartFile.transferTo(file);
            return file;
        } catch (IOException e) {
            // 写入失败时删除临时文件
            file.delete();
            throw e;
        }
    }

    /**
     * 使用上传的文件调用LLM并获取结果
     */
    public static String callLLMWithFile(String fileId,String workflowId,String apiKey) throws IOException {
        return callLLMWithFile(fileId,workflowId,apiKey,"image");
    }
    /**
     * 使用上传的文件调用LLM并获取结果
     */
    public static String callLLMWithFile(String fileId,String workflowId,String apiKey,String fileType) throws IOException {
        String userId = AuthorizationUtils.getLoginName();

        // 构建Workflow请求JSON
        JSONObject requestJson = new JSONObject();
        requestJson.put("workflow_id", workflowId);
        requestJson.put("user", userId);
        //streaming 流式模式（推荐）。基于 SSE（Server-Sent Events）实现类似打字机输出方式的流式返回。
        //blocking 阻塞模式，等待执行完毕后返回结果。（请求若流程较长可能会被中断）。 由于 Cloudflare 限制，请求会在 100 秒超时无返回后中断
        requestJson.put("response_mode", "blocking");
        requestJson.put("wait_for_completion", true); // 等待完成后返回

        JSONObject inputs = new JSONObject();

        // 添加文件作为输入（根据Workflow定义调整参数名）
        JSONArray fileInputs = new JSONArray();
        JSONObject fileInput = new JSONObject();
        fileInput.put("type", fileType);
        fileInput.put("transfer_method", "local_file");
        fileInput.put("upload_file_id", fileId);
//        fileInputs.add(fileInput);

        inputs.put("input_file", fileInput);
        requestJson.put("inputs", inputs);

        System.out.println("Workflow请求JSON: " + requestJson.toString());

        HttpResponse response  = HttpRequest.post(AiContents.CHAT_COMPLETION_ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestJson.toString())
                .execute();
        if (response.isOk()) {
            JSONObject jsonResponse = JSONObject.parseObject(response.body());
            log.info(jsonResponse.toString());
            JSONObject data = jsonResponse.getJSONObject("data");
            // 如果没有立即获得输出，轮询获取
            if (data.get("outputs") == null) {
                System.out.println("正在轮询获取Workflow输出结果...");
                JSONObject outputs = null;
//                try {
//                    outputs = pollWorkflowOutputs(jsonResponse.getString("workflow_run_id"), 10, 2);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
            }else {
                JSONObject outputsJson = data.getJSONObject("outputs");
                return outputsJson.get("out").toString();
            }
        } else {
            System.err.println("Workflow错误响应: " + response.body());
//            throw new RuntimeException("Workflow执行失败：" + response.status() + " - " + response.body());
        }
        return response.body();
    }
    /**
     * 轮询获取Workflow输出结果
     */
    public static JSONObject pollWorkflowOutputs(String workflowRunId, int maxAttempts, int intervalSeconds) throws InterruptedException {
        for (int i = 0; i < maxAttempts; i++) {
            HttpResponse response = HttpRequest.get(AiContents.RUN_STATUS_ENDPOINT + workflowRunId)
                    .header("Authorization", "Bearer " + DIFY_API_KEY)
                    .execute();

            if (response.isOk()) {
                JSONObject jsonResponse = JSONObject.parseObject(response.body());
                JSONObject data = jsonResponse.getJSONObject("data");
                String status = data.getString("status");

                if ("succeeded".equals(status)) {
                    JSONObject outputs = data.getJSONObject("outputs");
                    if (outputs != null) {
                        return outputs;
                    }
                } else if ("failed".equals(status)) {
                    throw new RuntimeException("Workflow执行失败: " + data.getString("error"));
                }
            } else {
                System.err.println("获取Workflow状态失败: " + response.body());
            }

            TimeUnit.SECONDS.sleep(intervalSeconds);
        }

        throw new RuntimeException("达到最大尝试次数，仍未获取到Workflow输出");
    }


}