package com.udacity.securityservice.application;

import com.udacity.securityservice.data.AlarmStatus;

public interface StatusListener {
    void notify(AlarmStatus status);
    void catDetected(boolean catDetected);
    void sensorStatusChanged();
}
