package com.hyx.Thread.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Description:
 * @Author: haoyuexun
 * @Date: 2020/7/27 14:12
 */
public class DateUtil {
    private static SimpleDateFormat sf = null;

    /**
     * 获取系统时间
     */
    public static String getCurrentDate() {
        Date d = new Date();
        sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sf.format(d);
    }

    /**
     * 时间戳转换字符串  */
    public static String getDateToString(long time) {
        Date d = new Date(time);
        sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sf.format(d);
    }

    /**
     * 将字符娽转成غ时间戳
     */
    public static long getStringToDate(String time) {
        sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        try{
            date = sf.parse(time);
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    /**
     * 直接获取时间戳
     */
    public static String getTimeStamp() {
        String currentDate = getCurrentDate();
        sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        try{
            date = sf.parse(currentDate);
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return String.valueOf(date.getTime());
    }

}
