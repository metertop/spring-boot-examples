package com.hyx.Thread.demo;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020-08-02 16:57
 */
public class StringUtil {

    /**
     * 根据查询字段，获取带有别名的字段, a.id, a,name
     */
    public static String getRefTableFields(String tableFields) {
        StringBuilder sb = new StringBuilder();
        String[] strArr = tableFields.split(",");

        for (String str: strArr) {
            sb.append(",a." + str);
        }

        return sb.toString().substring(1);
    }
}
