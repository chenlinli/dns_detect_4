package cn.cl.detect.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActionBase {
     Integer id;

     String transactionId;

     String queryDomain;

     String srcAddr;

     String dstAddr;

     String qType;

     String qr;

     Date time;

     Integer rdataLength;

     public String mainDomain;

     Integer andataLength;
}
