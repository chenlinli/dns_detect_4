package cn.cl.detect.data.action;

import cn.cl.detect.dao.*;
import cn.cl.detect.domain.*;
import cn.cl.detect.util.DateUtil;
import cn.cl.detect.util.DomainUtil;
import cn.cl.detect.util.SqlSessionFactoryUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MysqlActionData2File {
//    private String dns2tcpKey = "1";
//    private String dns2tcpTXT = "2";
//    private String dnscat2 = "3";
//    private String iodineNULL = "4";
//    private String iodineTXT = "5";
//    private String iodineMX = "6";
//    private String iodineSRV = "7";
//    private String iodineCNAME = "8";
//    private String iodineA = "9";
//    private String normal = "10";

    private final String TXT = "16";
      private final String CNAME = "5";
    private final String A ="1";
    private final String AAAA = "28";



    /**
     *
     * @param source
     * @param q_type
     */
    public void dns2tcp2File(String source,String q_type,Boolean isFeature){
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        ActionDns2tcpMapper actionDns2tcpMapper= sqlSession.getMapper(ActionDns2tcpMapper.class);
        ActionTimeScopeMapper actionTimeScopeMapper = sqlSession.getMapper(ActionTimeScopeMapper.class);
        ActionTimeScope actionTimeScope = actionTimeScopeMapper.getTimeScopeBySource(source);
        Date start = actionTimeScope.getStartTime();
        Date end = actionTimeScope.getTruncatedEndTime();
        Date currentEnd = DateUtil.getNextTimeMinute(start,4);
        List<String> res = new ArrayList<>();
        while (!DateUtil.after(currentEnd,end)){
            List<ActionDns2tcp> actionDns2tcpList = actionDns2tcpMapper.selectByQtypeAndTimeScope(q_type,start,currentEnd);
            String sb = getMainDomainFeaturesFromList(actionDns2tcpList);
            if(sb!=null)
                res.add(sb);
            start = DateUtil.getNextTimeMinute(start,1);
            currentEnd = DateUtil.getNextTimeMinute(start,4);
        }
        sqlSession.close();
        String filename = "action_dns2tcp_"+source;
        write2File(filename,source,res,isFeature);

    }


    public void dnscat22File(String source,Boolean isFeature){
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        ActionDnscat2Mapper actionDnscat2Mapper= sqlSession.getMapper(ActionDnscat2Mapper.class);
        ActionTimeScopeMapper actionTimeScopeMapper = sqlSession.getMapper(ActionTimeScopeMapper.class);
        ActionTimeScope actionTimeScope = actionTimeScopeMapper.getTimeScopeBySource(source);
        Date start = actionTimeScope.getStartTime();
        Date end = actionTimeScope.getTruncatedEndTime();
        Date currentEnd = DateUtil.getNextTimeMinute(start,4);
        List<String> res = new ArrayList<>();
        while (!DateUtil.after(currentEnd,end)){
            List<ActionDnscat2> actionDns2tcpList = actionDnscat2Mapper.selectByTimeScope(start,currentEnd);
            String sb = getMainDomainFeaturesFromList(actionDns2tcpList);
            if(sb!=null)
                res.add(sb);
            start = DateUtil.getNextTimeMinute(start,1);
            currentEnd = DateUtil.getNextTimeMinute(start,4);
        }
        sqlSession.close();

        String filename = "action_dnscat2_"+source;
        write2File(filename,source,res,isFeature);

    }

    public void iodine2File(String source, String q_type, Boolean isFeature){
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        ActionIodineMapper actionIodineMapper= sqlSession.getMapper(ActionIodineMapper.class);
        ActionTimeScopeMapper actionTimeScopeMapper = sqlSession.getMapper(ActionTimeScopeMapper.class);
        ActionTimeScope actionTimeScope = actionTimeScopeMapper.getTimeScopeBySource(source);
        Date start = actionTimeScope.getStartTime();
        Date end = actionTimeScope.getTruncatedEndTime();
        Date currentEnd = DateUtil.getNextTimeMinute(start,4);
        List<String> res = new ArrayList<>();
        while (!DateUtil.after(currentEnd,end)){
            List<ActionIodine> actionIodineList = actionIodineMapper.selectByQtypeAndTimeScope(q_type,start,currentEnd);
            String sb = getMainDomainFeaturesFromList(actionIodineList);
            if(sb!=null)
                res.add(sb);
            start = DateUtil.getNextTimeMinute(start,1);
            currentEnd = DateUtil.getNextTimeMinute(start,4);
        }
        sqlSession.close();

        String filename = "action_iodine_"+source;
        write2File(filename,source,res,isFeature);

    }


    public void normal2File(String source,Boolean isFeature){
        SqlSessionFactory sqlSessionFactory = SqlSessionFactoryUtil.getInstance();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        ActionNormalMapper actionNormalMapper= sqlSession.getMapper(ActionNormalMapper.class);
        ActionTimeScopeMapper actionTimeScopeMapper = sqlSession.getMapper(ActionTimeScopeMapper.class);
        ActionTimeScope actionTimeScope = actionTimeScopeMapper.getTimeScopeBySource(source);
        Date start = actionTimeScope.getStartTime();
        Date end = actionTimeScope.getTruncatedEndTime();
        Date currentEnd = DateUtil.getNextTimeMinute(start,4);
        List<String> res = new ArrayList<>();
        while (!DateUtil.after(currentEnd,end)){
            List<ActionNormal> actionNormalList = actionNormalMapper.selectByTimeScope(start,currentEnd);
            HashMap<String, List<ActionNormal>> mainDomainMap = new HashMap<>();
            for(ActionNormal actionNormal:actionNormalList){
                mainDomainMap.putIfAbsent(actionNormal.getMainDomain(),new ArrayList<>());
                List<ActionNormal> actionNormals = mainDomainMap.get(actionNormal.getMainDomain());
                actionNormals.add(actionNormal);
            }
            for(Map.Entry<String,List<ActionNormal>> entry:mainDomainMap.entrySet()){
                String sb = getMainDomainFeaturesFromList(entry.getValue());
                res.add(sb);
            }
            start = DateUtil.getNextTimeMinute(start,1);
            currentEnd = DateUtil.getNextTimeMinute(start,4);
        }
        sqlSession.close();
        String filename = "action_normal_"+source;
        write2File(filename,source,res,isFeature);
    }

    /**
     * 获得一段时间内主域名的特征向量
     * @param actions：5min内同一主域名的记录集合
     * @return
     */
    private String getMainDomainFeaturesFromList(List<? extends ActionBase > actions) {
        if(actions.size()<=0){
            System.out.println("no packget in time.");
            return "0.0,0.0,0.0,0.0,0,0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0";
        }
        double packetSpeed=0.0;  //平均包速 ：个/min
        double resourceDataLength = 0.0, //资源记录总长
                answerDataLength=0.0,  //应答记录长度
                averageResourceDataLength=0.0;  //平均资源记录长度
        HashSet<String> hosts = new HashSet<>(); //不同的主机名
        HashSet<String> qTypes = new HashSet<>(); //查询的所有种类
        double aTypeCount=0.0, aaaaTypeCount=0.0,cnameTypeCount=0.0
                ,txtTypeCount=0.0,otherTypeCount=0.0;
        int replyCount = 0;  //应答包的个数
        double totalPacketCount = actions.size();
        packetSpeed = totalPacketCount/5.0;
        for(ActionBase actionBase:actions){
            String qType = actionBase.getQType();
            qTypes.add(qType);  //查询类型
            switch (qType) {
                case A:
                    aTypeCount += 1;
                    break;
                case AAAA:
                    aaaaTypeCount += 1;
                    break;
                case CNAME:
                    cnameTypeCount += 1;
                    break;
                case TXT:
                    txtTypeCount += 1;
                    break;
                default:
                    otherTypeCount += 1;
                    break;
            }
            String qr = actionBase.getQr();
            if(qr.equals("1")) {  //应答
                replyCount++;
                resourceDataLength += actionBase.getRdataLength();
                answerDataLength += actionBase.getAndataLength();
            }else{  //查询
                hosts.add(DomainUtil.getHostName(actionBase.getQueryDomain(),actionBase.getMainDomain()));
            }
        }
        StringBuilder sb = new StringBuilder();
        if(replyCount!=0) {
            averageResourceDataLength = resourceDataLength / replyCount;
        }
        // 8 attr
//        sb.append(packetSpeed).append(",")                                                 //0
//                .append(answerDataLength).append(",")                                      //1
//                .append(hosts.size()).append(",")                                          //2
//                .append(qTypes.size()).append(",")                                         //3
//                .append(aTypeCount).append(",")                                            //4
//                .append(aTypeCount/totalPacketCount).append(",")                           //5
//                .append(aaaaTypeCount).append(",")                                         //6
//                .append(aaaaTypeCount/totalPacketCount)                                    //7
//        ;


        //6 attr
        sb.append(packetSpeed).append(",")                                                 //0
                .append(answerDataLength).append(",")                                      //1
                .append(hosts.size()).append(",")                                          //2
                .append(qTypes.size()).append(",")                                         //3
                .append(aTypeCount/totalPacketCount).append(",")                           //4
                .append(aaaaTypeCount/totalPacketCount)                                    //5
        ;

        //16 attr
//        sb.append(packetSpeed).append(",")                                                 //0
//                .append(resourceDataLength).append(",")                                    //1
//                .append(answerDataLength).append(",")                                      //2
//                .append(averageResourceDataLength).append(",")                             //3
//                .append(hosts.size()).append(",")                                          //4
//                .append(qTypes.size()).append(",")                                         //5
//                .append(aTypeCount).append(",")                                            //6
//                .append(aTypeCount/totalPacketCount).append(",")                           //7
//                .append(aaaaTypeCount).append(",")                                         //8
//                .append(aaaaTypeCount/totalPacketCount).append(",")                        //9
//                .append(cnameTypeCount).append(",")                                        //10
//                .append(cnameTypeCount/totalPacketCount).append(",")                       //11
//                .append(txtTypeCount).append(",")                                          //12
//                .append(txtTypeCount/totalPacketCount).append(",")                         //13
//                .append(otherTypeCount).append(",")                                        //14
//                .append(otherTypeCount/totalPacketCount)                                   //15
//        ;
        return sb.toString();
    }

    /**
     *
     * @param filename :要写的文件名
     * @param label :   记录类型
     * @param res ：要写的特征集
     * @param isFeatures：true:按照spark要的格式（type 1:field1 2:field2……）写,false 直接写
     */
    private void write2File(String filename, String label, List<String> res, boolean isFeatures) {
        if(isFeatures){
            filename = "features_"+filename;
        }
        File file = new File("action_features/6attr/2types/"+filename);
        if(file.exists()){
            file.delete();
        }
        BufferedWriter out = null;
        try{
            file.createNewFile();
            out = new BufferedWriter(new FileWriter(file));
            if(!isFeatures){
                for(String r:res){
                    out.write(r+"\n");
                }
            }else{
                int type = Integer.parseInt(label);
//                //四分类
//                if(type>=9){
//                    label = "0";  //normal
//                }else if(type>=3){
//                    label="1"; //iodine
//                }else if(type>=2){ //dnscat2
//                    label="2";
//                }else{          //dns2tcp
//                    label = "3";
//                }
//                //二分类
                if(type>=9){
                    label="0";
                }else{
                    label="1";
                }

                //11类
//                if(type>=9){
//                    label = "0";
//                }else{
//                    label = type+1+"";
//                }
                for(String r:res){
                    String[] split = r.split(",");
                    StringBuilder sb = new StringBuilder(label+" ");
                    for(int i=0;i<split.length;i++){
                        sb.append((i+1)+":"+split[i]+" ");
                    }
                    //去除最后的空格
                    out.write(sb.toString().trim()+"\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public static void main(String[] args) {
        MysqlActionData2File mysqlActionData2File = new MysqlActionData2File();
//        String rest = mysqlActionData2File.getHostName("dns.lingchenlingchen.top","dns.lingchenlingchen.top");
//        System.out.println(rest.equals(""));
//        mysqlActionData2File.dns2tcp2File("0","25",false); //key
//        mysqlActionData2File.dns2tcp2File("1","16",false); //key
//        mysqlActionData2File.dnscat22File("2",false);
//        mysqlActionData2File.iodine2File("3","10",false); //null
//        mysqlActionData2File.iodine2File("4","16",false); //txt
//        mysqlActionData2File.iodine2File("5","15",false);//mx
//        mysqlActionData2File.iodine2File("6","33",false);//srv
//        mysqlActionData2File.iodine2File("7","5",false);//cname
//        mysqlActionData2File.iodine2File("8","1",false);//a
//        mysqlActionData2File.normal2File("9",false);
       // mysqlActionData2File.normal2File("10",false);
        //mysqlActionData2File.normal2File("11",false);

        //spark：训练的文件格式
        mysqlActionData2File.dns2tcp2File("0","25",true); //key
        mysqlActionData2File.dns2tcp2File("1","16",true); //key
        mysqlActionData2File.dnscat22File("2",true);
        mysqlActionData2File.iodine2File("3","10",true); //null
        mysqlActionData2File.iodine2File("4","16",true); //txt
        mysqlActionData2File.iodine2File("5","15",true);//mx
        mysqlActionData2File.iodine2File("6","33",true);//srv
        mysqlActionData2File.iodine2File("7","5",true);//cname
        mysqlActionData2File.iodine2File("8","1",true);//a
        mysqlActionData2File.normal2File("11",true);
//        mysqlActionData2File.normal2File("9",true);
//        mysqlActionData2File.normal2File("10",true);
    }
}
