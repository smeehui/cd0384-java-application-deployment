package com.udacity.catpoint.service;

import com.udacity.catpoint.application.DisplayPanel;
import com.udacity.catpoint.application.ImagePanel;
import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.image.service.FakeImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class SecurityServiceTest {
  private FakeImageService imageService;
  private SecurityRepository securityRepository;
  private final StatusListener displayPanel = Mockito.mock(DisplayPanel.class);
  private final StatusListener imagePanel = Mockito.mock(ImagePanel.class);
  private SecurityService securityService;

  @BeforeEach
  public void setUp() {
    this.imageService = mock(FakeImageService.class);
    this.securityRepository = mock(SecurityRepository.class);
    this.securityService = new SecurityService(this.securityRepository, this.imageService);
    this.securityService.addStatusListener(displayPanel);
  }

  @ParameterizedTest
  @ValueSource(strings = {"DISARMED","ARMED_HOME","ARMED_AWAY"})
  public void setArmingStatus(String rawStatus) {
    var status = ArmingStatus.valueOf(rawStatus);
    securityService.setArmingStatus(status);
    verify(securityRepository, times(1)).setArmingStatus(status);
  }

  @Test
  public void catIsOnCamera_setArmedHomeStatus() throws IllegalAccessException {
    var status = ArmingStatus.ARMED_HOME;
    var alarmStatus = AlarmStatus.ALARM;
    var isCatOnCam = ReflectionUtils.findFields(securityService.getClass(), field -> field.getName().equals(
        "isCatOnCam"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    isCatOnCam.setAccessible(true);
    isCatOnCam.set(securityService,Boolean.TRUE);
    securityService.setArmingStatus(status);
    verify(securityRepository, times(1)).setArmingStatus(status);
    verify(securityRepository, times(1)).setAlarmStatus(alarmStatus);
  }

  @Test
  public void setArmingStatusWithDisarmed_shouldChangeToNoAlarmState() {
    var status = ArmingStatus.DISARMED;
    securityService.setArmingStatus(status);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.NO_ALARM);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void addStatusListener_shouldIncreaseNumberOfStatusListeners() throws NoSuchFieldException,
      IllegalAccessException {
    securityService.addStatusListener(imagePanel);
    var statusListeners = securityService.getClass().getDeclaredField("statusListeners");
    assertNotNull(statusListeners);
    statusListeners.setAccessible(true);
    assertEquals(2, ((Set<StatusListener>) statusListeners.get(securityService)).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void removeStatusListener_shouldDecreaseNumberOfStatusListeners() throws NoSuchFieldException,
      IllegalAccessException {
    securityService.removeStatusListener(displayPanel);
    var statusListeners = securityService.getClass().getDeclaredField("statusListeners");
    assertNotNull(statusListeners);
    statusListeners.setAccessible(true);
    assertEquals(0, ((Set<StatusListener>) statusListeners.get(securityService)).size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ALARM","NO_ALARM","PENDING_ALARM"})
  public void setAlarmStatus(String rawStatus) {
    var status = AlarmStatus.valueOf(rawStatus);
    securityService.setAlarmStatus(status);
    verify(securityRepository, times(1)).setAlarmStatus(status);
    verify(displayPanel, times(1)).notify(status);
  }

  @Test
  public void sensorActive_armingStatusDisarmed_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    mockSensor.setActive(false);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    securityService.changeSensorActivationStatus(mockSensor,true);
    verify(securityRepository,times(1)).getArmingStatus();
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void sensorInactive_armingStatusAlarm_armingStatusNull_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getArmingStatus()).thenReturn(null);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository,times(3)).getArmingStatus();
    verify(securityRepository, times(0)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    verify(displayPanel, times(0)).notify(AlarmStatus.PENDING_ALARM);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void sensorInactive_armingStatusNoAlarm_armingStatusArmedAway_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository,times(2)).getArmingStatus();
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.PENDING_ALARM);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void sensorInactive_armingStatusPendingAlarm_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository, times(0)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(0)).notify(AlarmStatus.ALARM);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void sensorInActive_armingStatusPendingAlarm_armingStatusArmedHome_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.ALARM);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void sensorInActive_armingStatusPendingAlarmAndArmedAway_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.ALARM);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void sensorInActive_armingStatusAlarmAndArmedAway_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository, times(0)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(0)).notify(AlarmStatus.ALARM);
    assertTrue(mockSensor.getActive());
  }


  @Test
  public void sensorIsActive_armingStatusPendingAlarm_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    mockSensor.setActive(true);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    securityService.changeSensorActivationStatus(mockSensor,false);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.NO_ALARM);
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertFalse(mockSensor.getActive());
  }

  @Test
  public void sensorIsActive_armingStatusAlarm_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    mockSensor.setActive(true);
    securityService.changeSensorActivationStatus(mockSensor,false);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository, times(1)).getAlarmStatus();
    verify(securityRepository, times(0)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    verify(displayPanel, times(0)).notify(AlarmStatus.PENDING_ALARM);
    assertFalse(mockSensor.getActive());
  }
  @Test
  public void sensorInActive_activeIsFalse_testChangeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    securityService.changeSensorActivationStatus(mockSensor,false);

    verify(securityRepository,times(0)).getArmingStatus();
    verify(securityRepository,times(0)).getAlarmStatus();
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertFalse(mockSensor.getActive());
  }

  @Test
  public void sensorInActive_activeIsFalse_havingActiveSensors_changeSensorActivationStatus() {
    var mockSensor =new Sensor("dummy",SensorType.DOOR);
    var mockActiveSensor =new Sensor("dummy2",SensorType.DOOR);
    mockSensor.setActive(true);
    mockActiveSensor.setActive(true);
    when(securityRepository.getSensors()).thenReturn(Set.of(mockSensor,mockActiveSensor));
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

    securityService.changeSensorActivationStatus(mockSensor,false);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository,times(1)).getAlarmStatus();
    verify(securityRepository,times(0)).getArmingStatus();
    assertFalse(mockSensor.getActive());
  }

  @Test
  public void sensorActive_activeIsTrue_changeSensorActivationStatus() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    mockSensor.setActive(true);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).updateSensor(mockSensor);
    verify(securityRepository,times(1)).getArmingStatus();
    verify(securityRepository,times(0)).getAlarmStatus();
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void imageContainsCat_armingStatusArmedHome_processImage() {
    var image = new BufferedImage(1, 2, 3);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

    securityService.processImage(image);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.ALARM);
    verify(displayPanel,times(1)).catDetected(true);
  }

  @Test
  public void imageDoesNotContainCat_armingStatusArmedHome_processImage() {
    var image = new BufferedImage(1, 2, 3);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(false);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

    securityService.processImage(image);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.NO_ALARM);
    verify(displayPanel,times(1)).catDetected(false);
  }

  @Test
  public void imageContainsCat_armingStatusNotArmedHome_processImage() {
    var image = new BufferedImage(1, 2, 3);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

    securityService.processImage(image);

    verify(securityRepository, times(0)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(0)).notify(AlarmStatus.NO_ALARM);
    verify(displayPanel,times(1)).catDetected(true);
  }

  @Test
  public void imageDoesNotContainsCat_havingActiveSensors_processImage() {
    var image = new BufferedImage(1, 2, 3);
    var mockSensor = Mockito.mock(Sensor.class);
    when(mockSensor.getActive()).thenReturn(true);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(false);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
    when(securityRepository.getSensors()).thenReturn(Set.of(mockSensor));

    securityService.processImage(image);

    verify(securityRepository, times(0)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(0)).notify(AlarmStatus.NO_ALARM);
    verify(displayPanel,times(1)).catDetected(false);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ALARM","NO_ALARM","PENDING_ALARM"})
  public void getAlarmStatus_shouldReturnAlarmStatus(String rawStatus) {
    var status = AlarmStatus.valueOf(rawStatus);
    when(securityRepository.getAlarmStatus()).thenReturn(status);
    assertEquals(securityService.getAlarmStatus(), status);
  }

  @Test
  public void getSensors_shouldReturnAllSensors() {
    var mock = new Sensor("dummy",SensorType.DOOR);
    var mockSensors = Set.of(mock);
    when(securityRepository.getSensors()).thenReturn(mockSensors);
    var result = securityService.getSensors();
    assertEquals(result.size(), mockSensors.size());
    assertTrue(result.contains(mock));
  }

  @Test
  public void addSensor_shouldIncreaseNumberOfSensors() {
    var mock = new Sensor("dummy",SensorType.DOOR);
    securityService.addSensor(mock);
    verify(securityRepository,times(1)).addSensor(mock);
  }

  @Test
  public void removeSensor_shouldDecreaseNumberOfSensors() {
    var mock = new Sensor("dummy",SensorType.DOOR);
    securityService.removeSensor(mock);
    verify(securityRepository,times(1)).removeSensor(mock);
  }

  @ParameterizedTest
  @ValueSource(strings = {"DISARMED","ARMED_HOME","ARMED_AWAY"})
  public void getArmingStatus_shouldReturnArmingStatus(String rawStatus) {
    var status = ArmingStatus.valueOf(rawStatus);
    when(securityRepository.getArmingStatus()).thenReturn(status);
    assertEquals(securityService.getArmingStatus(), status);
  }
}