package com.beijixing.social.crawl.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RiskControlAction {

    BLOCK_REQUEST("BLOCK", "Block current request immediately"),
    DELAY_AND_RETRY("DELAY", "Add delay and retry with backoff"),
    SWITCH_PROXY("PROXY", "Switch to different proxy IP"),
    ROTATE_ACCOUNT("ACCOUNT", "Rotate to different account/cookie"),
    REDUCE_RATE("REDUCE", "Reduce request rate by 50%"),
    ALERT_ONLY("ALERT", "Log alert but continue"),
    PAUSE_TASK("PAUSE", "Pause task for manual review");

    private final String code;
    private final String description;
}
