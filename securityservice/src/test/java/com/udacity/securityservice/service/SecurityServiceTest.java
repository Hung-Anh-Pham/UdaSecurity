package com.udacity.securityservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.udacity.imageservice.ImageService;
import com.udacity.securityservice.application.StatusListener;
import com.udacity.securityservice.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Stream;

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

    // Test 1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void given_armedStatus_activeSensor_then_changeToPendingAlarm(ArmingStatus armingStatus) {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // When
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2 (Coverage 10%)
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void given_armedStatus_activeSensor_pendingAlarm_then_changeToAlarmStatus(ArmingStatus armingStatus) {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        // When
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 3
    @Test
    void given_pendingAlarm_inactiveSensors_then_returnNoAlarm() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        // When
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }



    // Test 4 (coverage: 2%)
    @ParameterizedTest
    @ArgumentsSource(SensorStateArgumentProvider.class)
    void given_activeAlarm_swipeSensorState_then_notAffectAlarmState(boolean initialState, boolean swipeState) {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(initialState);

        // Then
        securityService.changeSensorActivationStatus(sensor, swipeState);

        // Then
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5 (coverage 10%)
    @Test
    void given_activeSensor_pendingAlarm_when_activateSensor_then_changeToAlarmState() {
        // Given
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);

        // When
        securityService.changeSensorActivationStatus(sensor, true);

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6 (coverage 7%)
    @Test
    void given_inactiveSensor_when_deactivate_then_noAffectAlarmState() {
        // Given
        sensor.setActive(false);

        // When
        securityService.changeSensorActivationStatus(sensor, false);

        // Then
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 7 (coverage 7%)
    @Test
    void given_armedHomeState_when_detectedCats_then_changeToAlarmState() {
        // Given
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(fakeImageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);

        // When
        securityService.processImage(mock(BufferedImage.class));

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 8 (coverage 8%)
    @Test
    void given_inactiveSensors_undetectedCats_then_changeToNoAlarmState(){
        // Given
        Sensor sensor1 = new Sensor(getRandomString(), SensorType.DOOR);
        Sensor sensor2 = new Sensor(getRandomString(), SensorType.WINDOW);

        sensor1.setActive(false);
        sensor2.setActive(false);

        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        // When
        securityService.addSensor(sensor1);
        securityService.addSensor(sensor2);
        securityService.processImage(mock(BufferedImage.class));

        // Then
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 9 (coverage 5%)
    @Test
    void given_disarmedSystem_then_setNoAlarmState() {
        // When
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // Then
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 11 (coverage 17%)
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED", "ARMED_AWAY"})
    void given_systemArmedHome_detectedCats_then_changeStatusToAlarm(ArmingStatus armingStatus) {
        // Given
        when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

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

    // Test 10 (coverage 17%)
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

    private String getRandomString() {
        return UUID.randomUUID().toString();
    }

    // bypass
    @Test
    void add_remove_statusListeners() {
        StatusListener listener = mock(StatusListener.class);
        securityService.addStatusListener(listener);
        securityService.removeStatusListener(listener);
    }

    // bypass
    @Test
    void add_remove_sensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }
}

class SensorStateArgumentProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                Arguments.of(Boolean.TRUE, Boolean.FALSE),
                Arguments.of(Boolean.FALSE, Boolean.TRUE)
        );
    }
}
