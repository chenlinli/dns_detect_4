package cn.cl.detect;

import static org.junit.Assert.assertTrue;

import cn.cl.detect.domain.NormalRecord;
import cn.cl.detect.util.DateUtil;
import cn.hutool.core.util.ReUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void testDistinctEntry(){
        NormalRecord t1 = new NormalRecord(1l, "tet");
        NormalRecord t2 = new NormalRecord(1l, "tet");

        HashSet<NormalRecord> normalRecords = new HashSet<>();
        normalRecords.add(t1);
        normalRecords.add(t2);
        System.out.println(normalRecords.size());
    }

    @Test
    public void testString(){
        String s = "rt.te.uy.";
        String substring = s.substring(0, s.length() - 1);
        System.out.println(substring);
        int index = substring.lastIndexOf(".");
        System.out.println(substring.substring(0,index));
    }
    @Test
    public void testRegex(){
        String s = "4+6\\5y/t-44.";
        String regex = "[^a-zA-Z0-9\\.\\-]";
        System.out.println(ReUtil.findAll("[a-z]",s,0));
        System.out.println(ReUtil.findAll("[^a-zA-Z0-9\\.\\-]",s,0));
        String s1 = ReUtil.replaceAll(s, "[^a-zA-Z0-9\\.\\-]", "");
        System.out.println(s1);


    }

    @Test
    public void testTrim(){
        String s = "gf   ";
        String trim = s.trim();
        System.out.println(s.length()+"  "+trim.length());
    }

    @Test
    public void testHex(){
        int num =  0xfd48;
        Integer.toHexString(num);
        System.out.println(num);
    }

    @Test
    public void testBoolean(){
        System.out.println(Boolean.valueOf("true"));
    }

    @Test
    public void testDateUtil(){
        Date date = new Date();
        System.out.println(date);
        System.out.println(DateUtil.getNextTimeMinute(date,5));
        Date d2 = date;
        System.out.println(DateUtil.after(date,d2));
    }

    @Test
    public void testSplit(){
        String a = "1";
        String[] split = a.split(",");
        System.out.println(split[0]);
    }

    @Test
    public void testSB(){
        String a = "a,b,";
        StringBuilder sv = new StringBuilder(a);
        String substring = sv.substring(0, sv.length()-1);
        System.out.println(sv.toString());
        System.out.println(substring);
    }

    @Test
    public void testClear(){
        HashSet<String> s = new HashSet<>();
        String a = "a,b,c,d";
        s.addAll(Arrays.asList(a.split(",")));
        System.out.println(s);
        s.clear();
        System.out.println(s);
    }
}
