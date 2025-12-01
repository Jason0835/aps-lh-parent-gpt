package com.zlt.aps.itf.util;

import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 生成token的方法
 *
 * @author zlt
 * @see java.util.Calendar
 * @see java.util.TimeZone
 * @since 2024/2/26
 */
@RestController
@Slf4j
@RequestMapping("/api/token")
@Api(tags = "生成token")
public class GenerateTokenUtil {

    @Resource
    RedisService redisService;

    @ApiOperation("根据IP生成密钥")
    @GetMapping("/generateToken")
    public String generateToken(String ip) {
        String token = generateKey(ip);
        if (PubUtil.isNotEmpty(token)){
            redisService.setCacheObject(token, ip);
        }else {
            return  "生成token失败";
        }
        return token;
    }


    @ApiOperation("根据域名生成密钥")
    @GetMapping("/generateToken2")
    public String generateToken2(String domain) {
        String token = genDomainKey(domain);
        if (PubUtil.isNotEmpty(token)){
            redisService.setCacheObject(token, domain);
        }else {
            return  "生成token失败";
        }
        return token;
    }

    /**
     * 2024-03-19 Nick + 加密算法SHA-256S token生成
     */
    public static String generateKey(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            byte[] ipBytes = inetAddress.getAddress();

            if (ipBytes.length == 4) { // IPv4
                byte[] sortedBytes = new byte[4];
                System.arraycopy(ipBytes, 0, sortedBytes, 0, 4);
                java.util.Arrays.sort(sortedBytes);

                String ipString = new String(sortedBytes);

                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(ipString.getBytes());
                    byte[] digest = md.digest();

                    StringBuilder key = new StringBuilder();
                    for (byte b : digest) {
                        key.append(String.format("%02x", b));
                    }
                    return key.toString();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            } else if (ipBytes.length == 16) { // IPv6
                byte[] sortedBytes = new byte[16];
                System.arraycopy(ipBytes, 0, sortedBytes, 0, 16);
                java.util.Arrays.sort(sortedBytes);

                String ipString = new String(sortedBytes);

                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(ipString.getBytes());
                    byte[] digest = md.digest();

                    StringBuilder key = new StringBuilder();
                    for (byte b : digest) {
                        key.append(String.format("%02x", b));
                    }
                    return key.toString();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                throw new IllegalArgumentException("Invalid IP address");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     *  域名采md5 生成固定token
     */
    public static String genDomainKey(String domain) {
        StringBuilder tokenBuilder = new StringBuilder();
        tokenBuilder.append(domain).append(ApsConstant.ALPHABET);
        String token = DigestUtils.md5DigestAsHex(tokenBuilder.toString().getBytes());
        return token;
    }


    /**
     * 解密算法
     */
    public static boolean validateKey(String key, HttpServletRequest request ) {
        //先根据域名
        String domain = request.getRemoteHost();
        String expectedKey = genDomainKey(domain);
        Boolean result = expectedKey.equals(key);

        if(!result){
            //如果域名不匹配，则根据IP生成key
            String ip = request.getRemoteAddr();
            expectedKey = generateKey(ip);
            result = expectedKey.equals(key);
        }

        return result;
    }
}
