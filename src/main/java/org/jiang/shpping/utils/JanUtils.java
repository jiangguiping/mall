package org.jiang.shpping.utils;



import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JanUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JanUtils.class);


    /**
     * unicode编码转换为汉字
     * @param unicodeStr 待转化的编码
     * @return 返回转化后的汉子
     */
    public static String UnicodeToCN(String unicodeStr) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(unicodeStr);
        char ch;
        while (matcher.find()) {
            //group
            String group = matcher.group(2);
            //ch:'李四'
            ch = (char) Integer.parseInt(group, 16);
            //group1
            String group1 = matcher.group(1);
            unicodeStr = unicodeStr.replace(group1, ch + "");
        }

        return unicodeStr.replace("\\", "").trim();
    }


    /**
     * 获取uuid
     * @author Mr.Zhang
     * @since 2020-05-06
     * @
     */
    public static String getUuid(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取当前日期,指定格式
     * 描述:<描述函数实现的功能>.
     *
     * @return
     */
    public static Date nowDateTime() {
        return strToDate(nowDateTimeStr(), Constants.DATE_FORMAT);
    }
    /**
     * 获取当前日期,指定格式
     * 描述:<描述函数实现的功能>.
     *
     * @return
     */
    public static String nowDateTimeStr() {
        return nowDate(Constants.DATE_FORMAT);
    }

    /**
     * 订单号生成
     * @param payType String 支付类型
     * @return 生成的随机码
     */
    public static String getOrderNo(String payType){
        return payType + randomCount(11111, 99999) + System.currentTimeMillis() + randomCount(11111, 99999);
    }
    /**
     * 根据长度生成随机数字
     * @param start 起始数字
     * @param end 结束数字
     * @return 生成的随机码
     */
    public static Integer randomCount(Integer start, Integer end){
        return (int)(Math.random()*(end - start +1) + start);
    }

    /**
     * 获取当前日期,指定格式
     * 描述:<描述函数实现的功能>.
     *
     * @return
     */
    public static Integer getNowTime() {
        long t = (System.currentTimeMillis()/1000L);
        return Integer.parseInt(String.valueOf(t));
    }

    public static Date strToDate(String dateStr, String DATE_FORMAT) {
        SimpleDateFormat myFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return myFormat.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 字符串分割，转化为数组
     * @param str 字符串
     * @author Mr.Zhang
     * @since 2020-04-22
     * @return List<Integer>
     */
    public static List<Integer> stringToArray(String str){
        return stringToArrayByRegex(str, ",");
    }

    /**
     * 字符串分割，转化为数组
     * @param str 字符串
     * @param regex 分隔符有
     * @author Mr.Zhang
     * @since 2020-04-22
     * @return List<Integer>
     */
    public static List<Integer> stringToArrayByRegex(String str, String regex ){
        List<Integer> list = new ArrayList<>();
        if (str.contains(regex)){

            String[] split = str.split(regex);

            for (String value : split) {
                if(!StringUtils.isBlank(value)){
                    list.add(Integer.parseInt(value.trim()));
                }
            }
        }else {
            list.add(Integer.parseInt(str));
        }
        return list;
    }
    //数组去重
    public static List<Integer> arrayUnique(Integer[] arr){
        List<Integer> list = new ArrayList<>();
        for (Integer integer : arr) {
            if (!list.contains(integer)) {
                list.add(integer);
            }
        }

        return list;
    }

    /**
     * 获取当前日期,指定格式
     * 描述:<描述函数实现的功能>.
     *
     * @return
     */
    public static Date nowDateTimeReturnDate(String DATE_FORMAT) {
        SimpleDateFormat dft = new SimpleDateFormat(DATE_FORMAT);
        return strToDate(dft.format(new Date()), DATE_FORMAT);
    }

    /**
     * map转对象
     * @param map map
     * @param clz 对象
     * @author Mr.Zhang
     * @since 2020-04-14
     * @return Map
     */
    public static <T> T mapToObj(HashMap<String,Object> map, Class<T> clz){
        if (map == null) return null;
        return JSONObject.parseObject(JSONObject.toJSONString(map), clz);
    }

    /**
     * json字符串转数组
     * @param str 字符串
     * @author Mr.Zhang
     * @since 2020-04-22
     * @return List<T>
     */
    public static <T> List<T> jsonToListClass(String str, Class<T> cls){
        try{
            return JSONObject.parseArray(str, cls);
        }catch (Exception e){
            return new ArrayList<>();
        }
    }


    /**
     * 获取fin_in_set拼装sql
     * @param field String 字段
     * @param list ArrayList<Integer> 值
     * @author Mr.Zhang
     * @since 2020-04-22
     * @return String
     */
//    public static String getFindInSetSql(String field, ArrayList<ArrayList<Integer>> list ){
//        ArrayList<String> sqlList = new ArrayList<>();
//        for (ArrayList<Integer> value: list) {
//            sqlList.add(getFindInSetSql(field, value));
//        }
//        return "( " + StringUtils.join(sqlList, " or ") + ")";
//    }

    /**
     * 检查是否可以转换int
     * @param str
     * @return
     */
    public static boolean isString2Num(String str){
        Pattern pattern = Pattern.compile("^[0-9]*$");
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 字符串分割，转化为数组
     * @param str 字符串
     * @author Mr.Zhang
     * @since 2020-04-22
     * @return List<String>
     */
    public static List<String> stringToArrayStr(String str){
        return stringToArrayStrRegex(str, ",");
    }

    /**
     * 字符串分割，转化为数组
     * @param str 字符串
     * @param regex 分隔符有
     * @author Mr.Zhang
     * @since 2020-04-22
     * @return List<String>
     */
    public static List<String> stringToArrayStrRegex(String str, String regex ){
        List<String> list = new ArrayList<>();
        if (str.contains(regex)){

            String[] split = str.split(regex);

            for (String value : split) {
                if(!StringUtils.isBlank(value)){
                    list.add(value);
                }
            }
        }else {
            list.add(str);
        }
        return list;
    }


    public static String dateToStr(Date date, String DATE_FORMAT) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat myFormat = new SimpleDateFormat(DATE_FORMAT);
        return myFormat.format(date);
    }

    /**
     * 获取当前日期,指定格式
     * 描述:<描述函数实现的功能>.
     *
     * @return
     */
    public static String nowDate(String DATE_FORMAT) {
        SimpleDateFormat dft = new SimpleDateFormat(DATE_FORMAT);
        return dft.format(new Date());
    }

    public static int compareDate(String date1, String date2, String pattern) {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(pattern);
        try {
            Date dt1 = DATE_FORMAT.parse(date1);
            Date dt2 = DATE_FORMAT.parse(date2);
            if (dt1.getTime() > dt2.getTime()) {
                return 1;
            } else if (dt1.getTime() < dt2.getTime()) {
                return -1;
            } else {
                return 0;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 指定日期加上天数后的日期
     *
     * @param num     为增加的天数
     * @param newDate 创建时间
     * @return
     */
    public static final String addDay(Date newDate, int num, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Calendar ca = Calendar.getInstance();
        ca.setTime(newDate);
        ca.add(Calendar.DATE, num);
        return format.format(ca.getTime());
    }

    /**
     * 指定日期加上天数后的日期
     *
     * @param num     为增加的天数
     * @param newDate 创建时间
     * @return
     */
    public static final String addDay(String newDate, int num, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            Date currdate = format.parse(newDate);
            Calendar ca = Calendar.getInstance();
            ca.setTime(currdate);
            ca.add(Calendar.DATE, num);
            return format.format(ca.getTime());
        } catch (ParseException e) {
            LOGGER.error("转化时间出错,", e);
            return null;
        }
    }


    public static Date addSecond(Date date, int num) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, num);
        return calendar.getTime();
    }
}
