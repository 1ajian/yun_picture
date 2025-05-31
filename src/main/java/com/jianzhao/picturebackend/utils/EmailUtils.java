package com.jianzhao.picturebackend.utils;

import cn.hutool.core.collection.CollUtil;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: EmailUtils
 * Package: com.jianzhao.picturebackend.utils
 * Description:
 *  邮箱工具类
 * @Author 阿小健
 * @Create 2025/5/25 12:11
 * @Version 1.0
 */
public class EmailUtils {

    /**
     * 发送静态模板邮件
     * @param path 模板路径
     * @return
     */
    public static String emailContentTemplate(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        InputStream inputStream = null;
        BufferedReader fileReader = null;
        StringBuffer buffer = new StringBuffer();
        String line = "";
        try {
            inputStream = resource.getInputStream();
            fileReader = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = fileReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            throw new RuntimeException("邮件模板读取异常",e);
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

    /**
     * 获取模板内容 并修改
     * @param path
     * @param paramMap
     * @return
     */
    public static String emailContentTemplate(String path, Map<String, Object> paramMap) {
        return emailContentTemplate(path, "BOOT_", "_END", paramMap);
    }

    /**
     * 获取模板内容 并修改
     * @param path 模板路径
     * @param prefix 前缀
     * @param suffix 后缀
     * @param paramMap 参数
     * @return
     */
    public static String emailContentTemplate(String path, String prefix, String suffix, Map<String, Object> paramMap) {
        String content = emailContentTemplate(path);
        //获取所有占位字符
        List<String> targetList = getTargetString(content, prefix, suffix);
        if (targetList != null) {
            for (String target : targetList) {
                //例如是：code
                Object o = paramMap.get(target);
                if (o != null) {
                    //替换
                    content = content.replace(prefix + target + suffix, o.toString());
                } else {
                    throw new RuntimeException("模板邮箱内不存在占位字符");
                }
            }
        }
        return content;
    }

    /**
     * 获取占位字符
     * @param content 模板内容
     * @param prefix 前缀
     * @param suffix  后缀
     * @return
     */
    private static List<String> getTargetString(String content, String prefix, String suffix) {
        List<Integer> startStrIndexs = getTargetIndex(content, 0, prefix);
        List<Integer> suffixStrIndexs = getTargetIndex(content, 0, suffix);
        if (CollUtil.isNotEmpty(startStrIndexs) && CollUtil.isNotEmpty(suffixStrIndexs) && startStrIndexs.size() != suffixStrIndexs.size()) {
            return null;
        }
        ArrayList<String> strList = new ArrayList<>();
        for (int i = 0,num = startStrIndexs.size(); i < num; i++) {
            //例如 BOOT_code_END 取得code
            strList.add(content.substring(startStrIndexs.get(i) + prefix.length(), suffixStrIndexs.get(i)));
        }
        return strList;
    }

    /**
     * 找到所有目标字符串开始的位置
     * @param string 源字符串
     * @param index 开始的索引
     * @param target 目标字符串
     * @return
     */
    private static List<Integer> getTargetIndex(String string,int index,String target) {
        List<Integer> list = new ArrayList<>();
        if (index != -1) {
            //从指定的index开始，找到target出现的索引
            int findIndex = string.indexOf(target, index);
            //如果找不到，直接返回
            if (findIndex == -1) {
                return list;
            }
            //找到了加进去
            list.add(findIndex);
            //递归查找
            list.addAll(getTargetIndex(string, findIndex + 1, target));
        }

        return list;
    }
}
