package cn.cl.detect.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;


public class ActionDnscat2 extends ActionBase{

    public ActionDnscat2() {
    }

    public ActionDnscat2(Integer id, String transactionId, String queryDomain, String srcAddr, String dstAddr, String qType, String qr, Date time, Integer rdataLength, String mainDomain, Integer andataLength) {
        super(id, transactionId, queryDomain, srcAddr, dstAddr, qType, qr, time, rdataLength, mainDomain, andataLength);
    }
}