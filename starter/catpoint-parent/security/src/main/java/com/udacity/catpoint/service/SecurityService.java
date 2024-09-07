package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.image.service.FakeImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 * <p>
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

  private final FakeImageService imageService;
  private final SecurityRepository securityRepository;
  private final Set<StatusListener> statusListeners = new HashSet<>();
  private boolean isCatOnCam = false;

  public SecurityService(SecurityRepository securityRepository, FakeImageService imageService) {
    this.securityRepository = securityRepository;
    this.imageService = imageService;
  }

  /**
   * Sets the current arming status for the system. Changing the arming status
   * may update both the alarm status.
   *
   * @param armingStatus
   */
  public void setArmingStatus(ArmingStatus armingStatus) {
    switch (armingStatus) {
      case DISARMED:
//      9. If the system is disarmed, set the status to no alarm.
        setAlarmStatus(AlarmStatus.NO_ALARM);
        break;
      case ARMED_HOME: {
        if (isCatOnCam) {
          setAlarmStatus(AlarmStatus.ALARM);
        }
      }
      case ARMED_AWAY:
        var refreshingSensors = new HashSet<>(getSensors());
        refreshingSensors.forEach(s -> changeSensorActivationStatus(s, false));
        break;
    }
    securityRepository.setArmingStatus(armingStatus);
  }

  /**
   * Internal method that handles alarm status changes based on whether
   * the camera currently shows a cat.
   *
   * @param cat True if a cat is detected, otherwise false.
   */
  private void catDetected(Boolean cat) {
    if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
//    7. If the image service identifies an image containing a cat while the system is armed-home, put the system into
//    alarm status.
      setAlarmStatus(AlarmStatus.ALARM);
    } else if (!anySensorActivated() && !cat) {
//    8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as
//    the sensors are not active.
      setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    statusListeners.forEach(sl -> sl.catDetected(cat));
    isCatOnCam = cat;
  }

  /**
   * Register the StatusListener for alarm system updates from within the SecurityService.
   *
   * @param statusListener
   */
  public void addStatusListener(StatusListener statusListener) {
    statusListeners.add(statusListener);
  }

  public void removeStatusListener(StatusListener statusListener) {
    statusListeners.remove(statusListener);
  }

  /**
   * Change the alarm status of the system and notify all listeners.
   *
   * @param status
   */
  public void setAlarmStatus(AlarmStatus status) {
    securityRepository.setAlarmStatus(status);
    statusListeners.forEach(sl -> sl.notify(status));
  }

  /**
   * Internal method for updating the alarm status when a sensor has been activated.
   */
  private void handleSensorActivated() {

    if (ArmingStatus.DISARMED.equals(getArmingStatus())) {
      return; //no problem if the system is disarmed
    }

    if (ArmingStatus.ARMED_AWAY.equals(getArmingStatus()) || ArmingStatus.ARMED_HOME.equals(getArmingStatus())) {
      switch (getAlarmStatus()) {
//    1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
        case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
//    2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
        case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
//    4. If alarm is active, change in sensor state should not affect the alarm state.
//        Do nothing when status is ALARM
      }
    }
  }

  /**
   * Internal method for updating the alarm status when a sensor has been deactivated
   */
  private void handleSensorDeactivated() {
//    3. If pending alarm and all sensors are inactive, return to no alarm state.
    if (AlarmStatus.PENDING_ALARM.equals(getAlarmStatus()) && !anySensorActivated()) {
      setAlarmStatus(AlarmStatus.NO_ALARM);
    }
  }

  private boolean anySensorActivated() {
    return securityRepository.getSensors().stream().anyMatch(Sensor::getActive);
  }

  /**
   * Change the activation status for the specified sensor and update alarm status if necessary.
   *
   * @param sensor
   * @param active
   */
  public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
    var isSensorActivated = sensor.getActive();

    sensor.setActive(active);
    securityRepository.updateSensor(sensor);
    statusListeners.forEach(StatusListener::sensorStatusChanged);

//    5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
//    6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    if (active) {
      handleSensorActivated();
    } else if (isSensorActivated) {
      handleSensorDeactivated();
    }

  }

  /**
   * Send an image to the SecurityService for processing. The securityService will use its provided
   * ImageService to analyze the image for cats and update the alarm status accordingly.
   *
   * @param currentCameraImage
   */
  public void processImage(BufferedImage currentCameraImage) {
    catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
  }

  public AlarmStatus getAlarmStatus() {
    return securityRepository.getAlarmStatus();
  }

  public Set<Sensor> getSensors() {
    return securityRepository.getSensors();
  }

  public void addSensor(Sensor sensor) {
    securityRepository.addSensor(sensor);
  }

  public void removeSensor(Sensor sensor) {
    securityRepository.removeSensor(sensor);
  }

  public ArmingStatus getArmingStatus() {
    return securityRepository.getArmingStatus();
  }
}
