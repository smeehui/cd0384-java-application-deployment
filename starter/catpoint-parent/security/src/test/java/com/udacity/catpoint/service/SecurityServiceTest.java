package com.udacity.catpoint.service;

import com.udacity.catpoint.application.DisplayPanel;
import com.udacity.catpoint.application.ImagePanel;
import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.image.service.FakeImageService;
import junit.framework.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.mockito.Mockito.*;

public class SecurityServiceTest extends TestCase {
  private FakeImageService imageService;
  private SecurityRepository securityRepository;
  private final StatusListener displayPanel = Mockito.mock(DisplayPanel.class);
  private final StatusListener imagePanel = Mockito.mock(ImagePanel.class);
  private SecurityService securityService;

  @BeforeEach
  public void setUp() throws Exception {
    this.imageService = mock(FakeImageService.class);
    this.securityRepository = mock(SecurityRepository.class);
    this.securityService = new SecurityService(this.securityRepository, this.imageService);
    this.securityService.addStatusListener(displayPanel);
  }

  public void tearDown() throws Exception {
  }

  @Test
  public void testSetArmingStatus() {
    var status = ArmingStatus.ARMED_HOME;
    securityService.setArmingStatus(status);
    verify(securityRepository, times(1)).setArmingStatus(status);
  }

  @Test
  public void testSetArmingStatusWithDisarmedStatus() {
    var status = ArmingStatus.DISARMED;
    securityService.setArmingStatus(status);
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.NO_ALARM);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddStatusListener() throws NoSuchFieldException, IllegalAccessException {
    securityService.addStatusListener(imagePanel);
    var statusListeners = securityService.getClass().getDeclaredField("statusListeners");
    assertNotNull(statusListeners);
    statusListeners.setAccessible(true);
    assertEquals(2, ((Set<StatusListener>) statusListeners.get(securityService)).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRemoveStatusListener() throws NoSuchFieldException, IllegalAccessException {
    securityService.removeStatusListener(displayPanel);
    var statusListeners = securityService.getClass().getDeclaredField("statusListeners");
    assertNotNull(statusListeners);
    statusListeners.setAccessible(true);
    assertEquals(0, ((Set<StatusListener>) statusListeners.get(securityService)).size());
  }

  @Test
  public void testSetAlarmStatus() {
    var status = AlarmStatus.ALARM;
    securityService.setAlarmStatus(status);
    verify(securityRepository, times(1)).setAlarmStatus(status);
    verify(displayPanel, times(1)).notify(status);
  }

  @Test
  public void testChangeSensorActivationStatusWhenSensorIsNotActiveAndArmingStatusIsDisarmed() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    mockSensor.setActive(false);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    securityService.changeSensorActivationStatus(mockSensor,true);
    verify(securityRepository,times(1)).getArmingStatus();
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void testChangeSensorActivationStatusWhenSensorIsNotActiveAndArmingStatusIsNoAlarm() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(1)).getArmingStatus();
    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.PENDING_ALARM);
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void testChangeSensorActivationStatusWhenSensorIsNotActiveAndArmingStatusIsPendingAlarm() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.ALARM);
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void testChangeSensorActivationStatusWhenSensorIsActiveAndArmingStatusIsPendingAlarm() {
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
  public void testChangeSensorActivationStatusWhenSensorIsActiveAndArmingStatusIsAlarm() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    mockSensor.setActive(true);
    securityService.changeSensorActivationStatus(mockSensor,false);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.PENDING_ALARM);
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertFalse(mockSensor.getActive());
  }
  @Test
  public void testChangeSensorActivationStatusWhenSensorIsNotActiveAndActiveIsFalse() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    mockSensor.setActive(false);
    securityService.changeSensorActivationStatus(mockSensor,false);

    verify(securityRepository,times(0)).getArmingStatus();
    verify(securityRepository,times(0)).getAlarmStatus();
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertFalse(mockSensor.getActive());
  }

  @Test
  public void testChangeSensorActivationStatusWhenSensorActiveAndActiveIsTrue() {
    var mockSensor = new Sensor("dummy",SensorType.DOOR);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    mockSensor.setActive(true);
    securityService.changeSensorActivationStatus(mockSensor,true);

    verify(securityRepository,times(0)).getArmingStatus();
    verify(securityRepository,times(0)).getAlarmStatus();
    verify(securityRepository,times(1)).updateSensor(mockSensor);
    assertTrue(mockSensor.getActive());
  }

  @Test
  public void testProcessImageWhenImageContainsCatAndArminStatusIsArmedHome() {
    var image = new BufferedImage(1, 2, 3);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

    securityService.processImage(image);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.ALARM);
    verify(displayPanel,times(1)).catDetected(true);
  }

  @Test
  public void testProcessImageWhenImageDoesNotContainCatAndArminStatusIsArmedHome() {
    var image = new BufferedImage(1, 2, 3);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(false);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

    securityService.processImage(image);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.NO_ALARM);
    verify(displayPanel,times(1)).catDetected(false);
  }

  @Test
  public void testProcessImageWhenImageContainsCatAndArminStatusIsNotArmedHome() {
    var image = new BufferedImage(1, 2, 3);
    when(imageService.imageContainsCat(any(image.getClass()),anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

    securityService.processImage(image);

    verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    verify(displayPanel, times(1)).notify(AlarmStatus.NO_ALARM);
    verify(displayPanel,times(1)).catDetected(true);
  }

  @Test
  public void testGetAlarmStatus() {
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    assertEquals(securityService.getAlarmStatus(), AlarmStatus.NO_ALARM);
  }

  @Test
  public void testGetSensors() {
    var mock = new Sensor("dummy",SensorType.DOOR);
    var mockSensors = Set.of(mock);
    when(securityRepository.getSensors()).thenReturn(mockSensors);
    var result = securityService.getSensors();
    assertEquals(result.size(), mockSensors.size());
    assertTrue(result.contains(mock));
  }

  @Test
  public void testAddSensor() {
    var mock = new Sensor("dummy",SensorType.DOOR);
    securityService.addSensor(mock);
    verify(securityRepository,times(1)).addSensor(mock);
  }

  @Test
  public void testRemoveSensor() {
    var mock = new Sensor("dummy",SensorType.DOOR);
    securityService.removeSensor(mock);
    verify(securityRepository,times(1)).removeSensor(mock);
  }

  @Test
  public void testGetArmingStatus() {
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    assertEquals(securityService.getArmingStatus(),ArmingStatus.ARMED_HOME);
  }
}