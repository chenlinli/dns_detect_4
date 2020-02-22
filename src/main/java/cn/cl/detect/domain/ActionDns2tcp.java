package cn.cl.detect.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

public class ActionDns2tcp extends ActionBase{
    public ActionDns2tcp(Integer id, String transactionId, String queryDomain, String srcAddr, String dstAddr, String qType, String qr, Date time, Integer rdataLength, String mainDomain, Integer andataLength) {
        super(id, transactionId, queryDomain, srcAddr, dstAddr, qType, qr, time, rdataLength, mainDomain, andataLength);
    }

    public ActionDns2tcp() {
    }
}