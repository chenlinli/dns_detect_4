package cn.cl.detect.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Iodine {
    private Long id;

    private String transactionId;

    private String queryDomain;

    private String srcIp;

    private String dstIp;

    private String qtype;

    private String qr;

    private Date recordTime;

    private String anRdata;

  }