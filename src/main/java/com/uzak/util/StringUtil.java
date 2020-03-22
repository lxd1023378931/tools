package com.uzak.util;

import java.util.regex.Pattern;

/**
 * @Descriptor: 字符串相关工具类
 * @Author: liangxiudou
 * @Date: 11:36 2020/03/22
 */
public class StringUtil {
    /**
     * @Descriptor: 判断字符串是否不为空
     * @Author: liaoningbo
     * @Date: 11:37 2017/10/24
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * @Descriptor: 判断字符串是否为空
     * @Author: liaoningbo
     * @Date: 11:39 2017/10/24
     */
    public static boolean isBlank(String str) {
        return str == null || "".equals(str.trim());
    }

    /**
     * 判断字符串是不是数字
     *
     * @param value
     * @return
     */
    public static boolean isDigital(String value) {
        if (value == null)
            return false;
        Pattern pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        return pattern.matcher(value).matches();
    }

}
