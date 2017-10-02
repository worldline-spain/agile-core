/*******************************************************************************
 * Copyright (C) 2017 Create-Net / FBK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Create-Net / FBK - initial API and implementation
 ******************************************************************************/
package iot.agile.device.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iot.agile.Device;
import iot.agile.device.base.AgileBLEDevice;
import iot.agile.device.base.SensorUuid;
import iot.agile.exception.AgileNoResultException;
import iot.agile.object.DeviceDefinition;
import iot.agile.object.DeviceOverview;
import iot.agile.object.DeviceComponent;

public class TISensorTag extends AgileBLEDevice implements Device {
	protected Logger logger = LoggerFactory.getLogger(TISensorTag.class);
	protected static final Map<String, SensorUuid> sensors = new HashMap<String, SensorUuid>();	
	private static final byte[] TURN_ON_SENSOR = { 0X01 };
 	private static final byte[] TURN_OFF_SENSOR = { 0X00 };
	private static final String TEMPERATURE = "Temperature";
	private static final String ACCELEROMETER = "Accelerometer";
	private static final String HUMIDITY = "Humidity";
	private static final String MAGNETOMETER = "Magnetometer";
	private static final String PRESSURE = "Pressure";
	private static final String GYROSCOPE = "Gyroscope";
	private static final String OPTICAL = "Optical";

	{
		subscribedComponents.put(TEMPERATURE, 0);
		//subscribedComponents.put(ACCELEROMETER, 0);
		subscribedComponents.put(HUMIDITY, 0);
		subscribedComponents.put(PRESSURE, 0);
		//subscribedComponents.put(GYROSCOPE, 0);
		subscribedComponents.put(OPTICAL, 0);
	}
	
	{
		profile.add(new DeviceComponent(TEMPERATURE, "Degree celsius (°C)"));
		//profile.add(new DeviceComponent(ACCELEROMETER, ""));
		profile.add(new DeviceComponent(HUMIDITY, "Relative humidity (%RH)"));
		//profile.add(new DeviceComponent(MAGNETOMETER, ""));
		profile.add(new DeviceComponent(PRESSURE, "Hecto pascal (hPa)"));
		//profile.add(new DeviceComponent(GYROSCOPE, ""));
		profile.add(new DeviceComponent(OPTICAL, "Light intensity (W/sr)"));
	}

	static {
		sensors.put(TEMPERATURE,
				new SensorUuid("f000aa00-0451-4000-b000-000000000000", "f000aa01-0451-4000-b000-000000000000",
						"f000aa02-0451-4000-b000-000000000000", "f000aa03-0451-4000-b000-000000000000"));
		sensors.put(HUMIDITY,
				new SensorUuid("f000aa20-0451-4000-b000-000000000000", "f000aa21-0451-4000-b000-000000000000",
						"f000aa22-0451-4000-b000-000000000000", "f000aa23-0451-4000-b000-000000000000"));
		/*
		sensors.put(MAGNETOMETER,
				new SensorUuid("f000aa30-0451-4000-b000-000000000000", "f000aa31-0451-4000-b000-000000000000",
						"f000aa32-0451-4000-b000-000000000000", "f000aa33-0451-4000-b000-000000000000"));
		*/
		sensors.put(PRESSURE,
				new SensorUuid("f000aa40-0451-4000-b000-000000000000", "f000aa41-0451-4000-b000-000000000000",
						"f000aa42-0451-4000-b000-000000000000", "f000aa44-0451-4000-b000-000000000000"));
		/*
		sensors.put(GYROSCOPE,
				new SensorUuid("f000aa50-0451-4000-b000-000000000000", "f000aa51-0451-4000-b000-000000000000",
						"f000aa52-0451-4000-b000-000000000000", "f000aa53-0451-4000-b000-000000000000"));
		*/

		sensors.put(OPTICAL,
				new SensorUuid("f000aa70-0451-4000-b000-000000000000", "f000aa71-0451-4000-b000-000000000000",
						"f000aa72-0451-4000-b000-000000000000", "f000aa73-0451-4000-b000-000000000000"));
		/*
		sensors.put(ACCELEROMETER,
				new SensorUuid("f000aa80-0451-4000-b000-000000000000", "f000aa81-0451-4000-b000-000000000000",
						"f000aa82-0451-4000-b000-000000000000", "f000aa83-0451-4000-b000-000000000000"));
		*/
	}


	public static boolean Matches(DeviceOverview d) {
		return d.name.contains("SensorTag");
	}

	public static String deviceTypeName = "TI SensorTag";

	public TISensorTag(DeviceOverview deviceOverview) throws DBusException {
		super(deviceOverview);
	}

	/**
	 * 
	 * @param devicedefinition
	 * @throws DBusException
	 */
	public TISensorTag(DeviceDefinition devicedefinition) throws DBusException {
		super(devicedefinition);
	}

	@Override
	public void Connect() throws DBusException {
		super.Connect();
		for (String componentName : subscribedComponents.keySet()) {
			if (subscribedComponents.get(componentName) > 0) {
				logger.info("Resubscribing to {}", componentName);
				deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
				deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
			}
		}
	}

	@Override
	public String DeviceRead(String sensorName) {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(sensorName.trim())) {
					try {
						if (!hasOtherActiveSubscription(sensorName)) {
							// turn on sensor
							deviceProtocol.Write(address, getEnableSensorProfile(sensorName), TURN_ON_SENSOR);
						}
						/**
						 * The default read data period (frequency) of most of
						 * sensor tag sensors is 1000ms therefore the first data
						 * will be available to read after 1000ms for these we
						 * call Read method after 1 second
						 */
						Thread.sleep(1010);
						// read value
						byte[] readValue = deviceProtocol.Read(address, getReadValueProfile(sensorName));
						if (!hasOtherActiveSubscription(sensorName)) {
							deviceProtocol.Write(address, getTurnOffSensorProfile(sensorName), TURN_OFF_SENSOR);
						}
						return formatReading(sensorName, readValue);
					} catch (Exception e) {
						logger.debug("Error in reading value from Sensor {}", e);
						e.printStackTrace();
					}
				} else {
					logger.debug("Sensor not supported: {}", sensorName);
					return null;
				}
			} else {
				logger.debug("BLE Device not connected: {}", deviceName);
				return null;
			}
		} else {
			logger.debug("Protocol not supported:: {}", protocol);
			return null;
		}
		return null;
	}

	public String NotificationRead(String componentName){
 		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
		if (isConnected()) {
      if (isSensorSupported(componentName.trim())) {
					try {
						deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
						byte[] period = { 100 };
						deviceProtocol.Write(address, getFrequencyProfile(componentName), period);
						byte[] result = deviceProtocol.NotificationRead(address, getReadValueProfile(componentName));
						return formatReading(componentName, result);
					} catch (DBusException e) {
					e.printStackTrace();
				}
			} else {
        throw new AgileNoResultException("Sensor not supported:" + componentName);
			}
    } else {
      throw new AgileNoResultException("BLE Device not connected: " + deviceName);
    }
	} else {
    throw new AgileNoResultException("Protocol not supported: " + protocol);
	}
 		throw new AgileNoResultException("Unable to read "+componentName);
	}
	
	@Override
	public synchronized void Subscribe(String componentName) {
		logger.info("Subscribe to {}", componentName);
 		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
 					try {
						if (!hasOtherActiveSubscription()) {
							addNewRecordSignalHandler();
						}
						if (!hasOtherActiveSubscription(componentName)) {
  							deviceProtocol.Write(address, getEnableSensorProfile(componentName), TURN_ON_SENSOR);
							/* Setting the period on the Pressure sensor was not working. Since we are anyway using the default value, keep this disabled. TODO: verify pressure senosr.
							byte[] period = { 100 };
							deviceProtocol.Write(address, getFrequencyProfile(componentName), period);
							*/
							deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
						}
						subscribedComponents.put(componentName, subscribedComponents.get(componentName) + 1);
					} catch (DBusException e) {
						e.printStackTrace();
					}
				} else {
          throw new AgileNoResultException("Sensor not supported:" + componentName);
				}
			} else {
        throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
	}

@Override
	public synchronized void Unsubscribe(String componentName) throws DBusException {
		logger.info("Unsubscribe from {}", componentName);
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (isConnected()) {
				if (isSensorSupported(componentName.trim())) {
					try {
						subscribedComponents.put(componentName, subscribedComponents.get(componentName) - 1);
						if (!hasOtherActiveSubscription(componentName)) {
							// disable notification
							deviceProtocol.Unsubscribe(address, getReadValueProfile(componentName));
							// turn off sensor
							deviceProtocol.Write(address, getTurnOffSensorProfile(componentName), TURN_OFF_SENSOR);
						}
						if (!hasOtherActiveSubscription()) {
							removeNewRecordSignalHandler();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
          throw new AgileNoResultException("Sensor not supported:" + componentName);
				}
			} else {
        throw new AgileNoResultException("BLE Device not connected: " + deviceName);
			}
		} else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
		}
	}

	// =======================Utility methods===========================
	@Override
	protected boolean isSensorSupported(String sensorName) {
		return sensors.containsKey(sensorName);
	}

	private Map<String, String> getEnableSensorProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charConfigUuid);
		}
		return profile;
	}

	private Map<String, String> getReadValueProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charValueUuid);
		}
		return profile;
	}

	private Map<String, String> getTurnOffSensorProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charConfigUuid);
		}
		return profile;
	}

	private Map<String, String> getFrequencyProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charFreqUuid);
		}
		return profile;
	}

	/**
	 *
	 * The sensor service returns the data in an encoded format which can be
	 * found in the
	 * wiki(http://processors.wiki.ti.com/index.php/SensorTag_User_Guide#
	 * IR_Temperature_Sensor). Convert the raw sensor reading value format to
	 * human understandable value and print it.
	 * 
	 * @param sensorName
	 *            Name of the sensor to read value from
	 * @param readingValue
	 *            the raw value read from the sensor
	 * @return
	 */
	@Override
	protected String formatReading(String sensorName, byte[] readData) {
		float result;
		int rawData;
		if (sensorName.contains(TEMPERATURE)) {
			rawData = shortSignedAtOffset(readData, 2);
			result = convertCelsius(rawData);
		} else if (sensorName.contains(HUMIDITY)) {
			rawData = shortSignedAtOffset(readData, 2);
			result = convertHumidity(rawData);
		} else if (sensorName.contains(PRESSURE)) {
			int lowerByte = Byte.toUnsignedInt(readData[3]);
			int upperByte = Byte.toUnsignedInt(readData[4]);
			int upper = Byte.toUnsignedInt(readData[5]);
			int rawResult = (upperByte << 8) + (lowerByte & 0xff) + upper;
			float pressure = convertPressure(rawResult);

			int t_r; // Temperature raw value from sensor
			int p_r; // Pressure raw value from sensor
			Double t_a; // Temperature actual value in unit centi degrees
						// celsius
			Double S; // Interim value in calculation
			Double O; // Interim value in calculation
			Double p_a; // Pressure actual value in unit Pascal.

			t_r = shortSignedAtOffset(readData, 0);
			p_r = shortSignedAtOffset(readData, 3);

			t_a = (100 * (readData[0] * t_r / Math.pow(2, 8) + readData[1] * Math.pow(2, 6))) / Math.pow(2, 16);
			S = readData[3] + readData[4] * t_r / Math.pow(2, 17)
					+ ((readData[5] * t_r / Math.pow(2, 15)) * t_r) / Math.pow(2, 19);
			p_a = ((S * p_r) / Math.pow(2, 14));
			return Double.toString(p_a);
			// result = Float.toString(pressure);
		} else if (sensorName.equals(OPTICAL)) {
			rawData = shortSignedAtOffset(readData, 0);
			result = convertOpticalRead(rawData);
		} else {
			// TODO Other sensor values
			return readData.toString();
		}
		return Float.toString(result);
	}

	/**
	 * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's
	 * complement values in the format LSB MSB.
	 *
	 * This function extracts these 16 bit two's complement values.
	 */
	private static Integer shortSignedAtOffset(byte[] value, int offset) {
		Integer lowerByte = Byte.toUnsignedInt(value[offset]);
		Integer upperByte = Byte.toUnsignedInt(value[offset + 1]); // Note:
		return (upperByte << 8) + lowerByte;
	}

	/**
	 * Converts temperature into degree Celsius
	 *
	 * @param raw
	 * @return
	 */
	private float convertCelsius(int raw) {
		return raw / 128f;
	}

	/**
	 * Formats humidity value according to SensorTag WIKI
	 * 
	 * @param raw
	 * @return
	 */
	private float convertHumidity(int raw) {
		return (((float) raw) / 65536) * 100;
	}

	private float convertPressure(int raw) {
		return raw / 100;
	}

	private float convertOpticalRead(int raw) {
		int e = (raw & 0x0F000) >> 12; // Interim value in calculation
		int m = raw & 0x0FFF; // Interim value in calculation

		return (float) (m * (0.01 * Math.pow(2.0, e)));
	}

	/**
   * Given the profile of the component returns the name of the sensor
   * 
   * @param uuid
   * @return
   */
  @Override
  protected String getComponentName(Map<String, String> profile) {
    String serviceUUID = profile.get(GATT_SERVICE);
    String charValueUuid = profile.get(GATT_CHARACTERSTICS);
    for (Entry<String, SensorUuid> su : sensors.entrySet()) {
      if (su.getValue().serviceUuid.equals(serviceUUID) && su.getValue().charValueUuid.equals(charValueUuid)) {
        return su.getKey();
      }
    }
    return null;
  }

	@Override
	protected void DeviceWrite(String componentName, String value) {
		throw new UnsupportedOperationException();
	}
}
