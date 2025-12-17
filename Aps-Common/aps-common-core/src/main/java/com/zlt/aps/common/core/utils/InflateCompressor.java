package com.zlt.aps.common.core.utils;

import com.ruoyi.common.core.utils.sign.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.Inflater;

public class InflateCompressor {

    public static String uncompress(byte[] input) {
        Inflater inflater = new Inflater();
        inflater.setInput(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
        try {
            byte[] buff = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buff);
                baos.write(buff, 0, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        inflater.end();
        byte[] output = baos.toByteArray();
        try {
            return new String(output, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String uncompress2Base64(String jsonString) {
        if (jsonString == null) {
            return null;
        }

        byte[] decode = Base64.decode(jsonString);
        return uncompress(decode);
    }

    public static void main(String[] args) {

    }
}
