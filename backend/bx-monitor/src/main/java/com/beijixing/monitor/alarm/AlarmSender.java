package com.beijixing.monitor.alarm;

import com.beijixing.monitor.entity.AlertRecord;

public interface AlarmSender {

    /**
     * 发送告警
     * @param alert 告警记录
     * @return 是否发送成功
     */
    boolean send(AlertRecord alert);

    /**
     * 获取发送器类型
     */
    String getType();

    /**
     * 检查是否启用
     */
    boolean isEnabled();
}
