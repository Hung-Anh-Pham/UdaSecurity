package com.udacity.securityservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.udacity.imageservice.ImageService;
import com.udacity.securityservice.application.StatusListener;
import com.udacity.securityservice.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor sensor;

    // Creates a mock instance of a class/interface.
    // It does not create an instance of the class under test.
    @Mock
    private ImageService fakeImageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;

    // Creates an instance of the class under test and injects the mocks into it.
    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, fakeImageService);
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }

    @Test
    void given_alarmIsArmed_sensorIsActivated_then_changeStatusToPending() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // When
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void given_sensorIsActivate_alarmIsPending_systemIsArmed_then_changeStatusToAlarm() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        // When
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void given_sensorIsInactive_alarmIsPending_then_returnNoAlarm() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // When
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void given_alarmIsActive_changeSensor_then_notAffectAlarmState() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Then
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        // Then
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void given_sensorIsActive_alamIsPending_then_changeStatusToAlarm() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // When
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // What is the never()
    @Test
    void given_inactiveSensor_when_activate_then_noAffectAlarmState() {
        // When
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        // Then
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void given_detectedCats_systemIsArmedHome_then_changeStatusToAlarm() {
        // Given
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(fakeImageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);

        // When
        securityService.processImage(mock(BufferedImage.class));

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Redundant
    @Test
    void given_inactiveSensors_undetectedCats_then_changeStatusToNoAlarm(){
        // Given
        Sensor sensor1 = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
        Sensor sensor2 = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);

        sensor1.setActive(false);
        sensor2.setActive(false);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);

        when(securityRepository.getSensors()).thenReturn(sensors);
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        // When
        securityService.processImage(mock(BufferedImage.class));

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void given_systemDisarmed_then_setNoAlarmState() {
        // When
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void given_systemArmedHome_detectedCats_then_changeStatusToAlarm() {
        // Given
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        // When
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void given_systemDisarmed_alarmState_then_changeStatusToPending() {
        // Given
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // When
        securityService.changeSensorActivationStatus(sensor);

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Redundant
    @ParameterizedTest
    @EnumSource(value =  ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void given_systemArmed_then_setSensorsToInactive(ArmingStatus status) {
        // Given
        Sensor sensor1 = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
        Sensor sensor2 = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);

        sensor1.setActive(true);
        sensor2.setActive(true);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);

        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // when
        securityService.setArmingStatus(status);

        // Then
        securityService.getSensors().forEach(
                s -> assertFalse(s.getActive())
        );
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void given_systemDisarmed_sensorIsActivated_then_noChangeToArmingState(AlarmStatus status) {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        // When
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }
}
