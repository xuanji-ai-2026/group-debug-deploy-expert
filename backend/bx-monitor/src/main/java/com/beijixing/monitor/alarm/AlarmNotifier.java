package com.beijixing.monitor.alarm;

import com.beijixing.monitor.entity.AlertRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AlarmNotifier {

    private final List<AlarmSender> senders = new ArrayList<>();

    public AlarmNotifier(List<AlarmSender> senderList) {
        this.senders.addAll(senderList);
    }

    public void notify(AlertRecord alert) {
        for (AlarmSender sender : senders) {
            if (sender.isEnabled()) {
                try {
                    boolean success = sender.send(alert);
                    if (success) {
                        log.info("Alert sent via {}: {}", sender.getType(), alert.getAlertName());
                    } else {
                        log.warn("Failed to send alert via {}: {}", sender.getType(), alert.getAlertName());
                    }
                } catch (Exception e) {
                    log.error("Error sending alert via {}: {}", sender.getType(), e.getMessage(), e);
                }
            }
        }
    }
}
