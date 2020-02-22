package cn.cl.detect.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATEKEY_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * start时间的下几（minute）分钟
     * @param start
     * @param minute:=4，当前时间区间的结束时间，=5：下一个时间区间的起始时间
     * @return
     */
    public static Date getNextTimeMinute(Date start,int minute){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        calendar.add(Calendar.MINUTE,minute);
        return calendar.getTime();
    }


    /**
     * t1是否在t2前，t1<t2
     * @param t1
     * @param t2
     * @return
     */
    public static Boolean after(Date t1,Date t2){
        Calendar c1 = Calendar.getInstance();
        c1.setTime(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(t2);
        return t1.after(t2);
    }

    public static Date getDate(String t){
        try {
            return TIME_FORMAT.parse(t);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTodayDate() {
        return DATE_FORMAT.format(new Date());
    }
    /**
     * 判断一个时间是否在另一个时间之前
     * @param time1 第一个时间
     * @param time2 第二个时间
     * @return 判断结果
     */
    public static boolean before(String time1, String time2) {
        try {
            Date dateTime1 = TIME_FORMAT.parse(time1);
            Date dateTime2 = TIME_FORMAT.parse(time2);

            if(dateTime1.before(dateTime2)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *  获取年月日和小时
     * @param time:yyyy-MM-dd HH:mm:ss
     * @return:yyyy-MM-dd_HH
     */
    public static String getDateHour(String time) {
        String[] dateTime = time.split(" ");
        String date = dateTime[0];
        String hour = dateTime[1].split(":")[0];
        return date+"_"+hour;

    }


}
