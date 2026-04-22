package com.gorani.ecodrive.common.constants;

import java.time.ZoneId;

public final class TimeZoneConstants {

    public static final String ASIA_SEOUL = "Asia/Seoul";
    public static final ZoneId KST = ZoneId.of(ASIA_SEOUL);

    private TimeZoneConstants() {
    }
}
