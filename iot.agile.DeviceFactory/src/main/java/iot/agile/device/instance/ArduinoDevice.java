package iot.agile.device.instance;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iot.agile.Device;
import iot.agile.Protocol;
import iot.agile.device.base.DeviceImp;
import iot.agile.object.DeviceComponent;
import iot.agile.object.DeviceDefinition;
import iot.agile.object.DeviceOverview;

public class ArduinoDevice extends DeviceImp implements Device {
	protected Logger logger = LoggerFactory.getLogger(ArduinoDevice.class);
	
	
	//private static final String ZB = "zb/";
	private static final String ZB = "";	
	private static final String ZB_PROTOCOL_ID = "iot.agile.Protocol";
	private static final String ZB_PROTOCOL_PATH = "/iot/agile/Protocol/XBee_ZigBee/socket0";
	

	public static boolean Matches(DeviceOverview d) {
		return d.name.contains("ARDUINO");
	}

	public static String deviceTypeName = "ARDUINO";

	public ArduinoDevice(DeviceOverview deviceOverview) throws DBusException {
		super(deviceOverview);
		
		this.protocol = ZB_PROTOCOL_ID;
		String devicePath = AGILE_DEVICE_BASE_BUS_PATH + ZB + deviceOverview.id.replace(":", "");
	
		profile.add( new DeviceComponent("temperature","ºC"));
		profile.add( new DeviceComponent("power","A"));
		profile.add( new DeviceComponent("presence","?"));
		profile.add( new DeviceComponent("stock","units"));
		profile.add( new DeviceComponent("light","?"));
		profile.add( new DeviceComponent("beacon","?"));

		dbusConnect(deviceAgileID, devicePath, this);
		deviceProtocol = (Protocol) connection.getRemoteObject(ZB_PROTOCOL_ID, ZB_PROTOCOL_PATH, Protocol.class);		
		logger.debug("Exposed device {} {}", deviceAgileID, devicePath);
	}

	
	public ArduinoDevice(DeviceDefinition devicedefinition) throws DBusException {
		super(devicedefinition);
		this.protocol = ZB_PROTOCOL_ID;
		String devicePath = AGILE_DEVICE_BASE_BUS_PATH + ZB + devicedefinition.address.replace(":", "");
	
		profile.add( new DeviceComponent("data","units"));

		dbusConnect(deviceAgileID, devicePath, this);		
		deviceProtocol = (Protocol) connection.getRemoteObject(ZB_PROTOCOL_ID, ZB_PROTOCOL_PATH, Protocol.class);
		logger.debug("Exposed device {} {}", deviceAgileID, devicePath);
	}
	
	@Override
	public void Connect() throws DBusException {
		try {
			deviceProtocol.Connect(address);
			logger.info("Device connect {}", deviceID);
		} catch (DBusException e) {
			logger.error("Failed to connect device {}", deviceID);
			throw new DBusException("Failed to connect device:" + deviceID);
		}
	}

	@Override
	public String DeviceRead(String sensorName) {
		try {
			byte[] data = deviceProtocol.Read(deviceID, sensorName);
			logger.info("Device read {}", deviceID);
			return new String(data);
		} catch (DBusException e) {
			logger.error("Failed to read device {} sensor {}", deviceID, sensorName);
			return null;
		}
	}

	public String NotificationRead(String componentName) {
		return null;
	}

	@Override
	public synchronized void Subscribe(String componentName) {
		logger.info("Subscribe to {}", componentName);

	}

	@Override
	public synchronized void Unsubscribe(String componentName) throws DBusException {
		logger.info("Unsubscribe from {}", componentName);

	}

	
	protected boolean hasotherActiveSubscription() {
		for (String componentName : subscribedComponents.keySet()) {
			if (subscribedComponents.get(componentName) > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected String getComponentName(Map<String, String> profile) {

		return null;
	}

	@Override
	public void Stop() throws DBusException {
		for (String component : subscribedComponents.keySet()) {
			if (subscribedComponents.get(component) > 0) {
				Unsubscribe(component);
			}
		}
		Disconnect();
	}

	@Override
	public void Disconnect() throws DBusException {
		try {
			deviceProtocol.Disconnect(address);
			logger.info("Device disconnected {}", deviceID);
		} catch (DBusException e) {
			logger.error("Failed to disconnect device {}", deviceID);
			throw new DBusException("Failed to disconnect device:" + deviceID);
		}
	}

	@Override
	protected void DeviceWrite(String componentName, String value) {
		try {
			deviceProtocol.Write(deviceID, value);
		} catch (DBusException e) {
			logger.error("Failed to write to device {}", deviceID);
		}
	}
	
 	@Override
	protected String getMeasurementUnit(String sensor) {
 		return "";
 	}
}
