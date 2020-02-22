package cn.cl.detect.domain;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActionTimeScope {
    private String source;

    private String name;
    private Date startTime;

    private Date endTime;

    private Date truncatedEndTime;

  }