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
package iot.agile.devicemanager;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iot.agile.Device;
import iot.agile.DeviceManager;
import iot.agile.DeviceFactory;
import iot.agile.exception.AgileDeviceNotFoundException;
import iot.agile.object.AbstractAgileObject;
import iot.agile.object.DeviceComponent;
import iot.agile.object.DeviceDefinition;
import iot.agile.object.DeviceOverview;

/**
 * @author dagi
 *
 *         Agile Device manager implementation
 *
 */
public class DeviceManagerImp extends AbstractAgileObject implements DeviceManager {

	protected final Logger logger = LoggerFactory.getLogger(DeviceManagerImp.class);

	/**
	 * Bus name for the device manager
	 */
	private static final String AGILE_DEVICEMANAGER_MANAGER_BUS_NAME = "iot.agile.DeviceManager";
	/**
	 * Bus path for the device manager
	 */
	private static final String AGILE_DEVICEMANAGER_MANAGER_BUS_PATH = "/iot/agile/DeviceManager";

	/**
	 * registered devices
	 */
	protected final List<DeviceDefinition> devices = new ArrayList<DeviceDefinition>();

	public static void main(String[] args) throws DBusException {
		DeviceManager deviceManager = new DeviceManagerImp();
	}

	public DeviceManagerImp() throws DBusException {

		dbusConnect(AGILE_DEVICEMANAGER_MANAGER_BUS_NAME, AGILE_DEVICEMANAGER_MANAGER_BUS_PATH, this);
		logger.debug("Started Device Manager");
	}

	/**
	 *
	 *
	 * @see iot.agile.protocol.ble.devicemanager.DeviceManager#Find()
	 */
	@Override
	public String Find() {
		// TODO
		return null;
	}

	@Override
	public List<String> MatchingDeviceTypes(DeviceOverview deviceOverview) {
		List<String> ret = new ArrayList();
		try{
                    String objectName = "iot.agile.DeviceFactory";
                    String objectPath = "/iot/agile/DeviceFactory";
                    logger.info("Connection established: "+connection);
                    DeviceFactory factory = (DeviceFactory) connection.getRemoteObject(objectName, objectPath, DeviceFactory.class);
                    ret=factory.MatchingDeviceTypes(deviceOverview);
                }
                catch (Exception e) {
                    logger.error("Can not connect to the DeviceFactory DBus object: {}", e.getMessage());
                    e.printStackTrace();
                    }
		return ret; 
	}


	@Override
	public DeviceDefinition Register(DeviceOverview deviceOverview, String deviceType) {
		Device device = getDevice(deviceOverview);
		DeviceDefinition registeredDev = null;
		if (device != null) {
			registeredDev = device.Definition();
			logger.info("Device already registered:  {}", device.Id());
		} else {
			try {
				logger.info("HEXIWEAR - Checking device type: " + deviceType + " and overview " + deviceOverview);

				String objectName = "iot.agile.DeviceFactory";
				String objectPath = "/iot/agile/DeviceFactory";
				logger.info("Connection established: " + connection);
				DeviceFactory factory = (DeviceFactory) connection.getRemoteObject(objectName, objectPath,
						DeviceFactory.class);
				device = factory.getDevice(deviceType, deviceOverview);
				logger.info("Creating new device: {}", deviceType);
				if (device != null) {
					registeredDev = device.Definition();
					devices.add(registeredDev);
					logger.info("Created new device: {}", devices);
				}
			} catch (Exception e) {
				logger.error("Can not register device: {}", e.getMessage());
				e.printStackTrace();
			}
		}
		// connect device
		if (device != null) {
			final Device dev = device;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						dev.Connect();
						logger.info("Device connected");
					} catch (Exception e) {
						logger.error("Error encountered while attempting to connect: {}", e.getMessage());
					}
				}
			}).start();
		}
		return registeredDev;
	}

	/**
	 *
	 *
	 * @see iot.agile.protocol.ble.devicemanager.DeviceManager#Read(java.lang.
	 *      String)
	 */
	@Override
	public DeviceDefinition Read(String id) {
		for (DeviceDefinition dd : devices) {
			if (dd.deviceId.trim().equals(id)) {
				return dd;
			}
		}
		throw new AgileDeviceNotFoundException("Device not found");
	}

	/**
	 *
	 *
	 * @see iot.agile.protocol.ble.devicemanager.DeviceManager#Update(java.lang.
	 *      String, java.lang.String)
	 */
	@Override
	public void Update(String id, DeviceDefinition definition) {
		logger.debug("DeviceManager.Update not implemented");
	}

	/**
	 *
	 *
	 * @see iot.agile.protocol.ble.devicemanager.DeviceManager#Devices()
	 */
	@Override
	public List<DeviceDefinition> Devices() {
		return devices;
	}

	/**
	 *
	 *
	 * @see iot.agile.protocol.ble.devicemanager.DeviceManager#Delete(java.lang.
	 *      String, java.lang.String)
	 */
	@Override
	public void Delete(String id) {
		DeviceDefinition devDefn = Read(id);
		if (devDefn != null) {
			Device device = getDevice(devDefn);
			if (device != null) {
				try {
					device.Stop();
					connection.unExportObject(devDefn.path);
					devices.remove(devDefn);
					logger.info("Device deleted: {}", id);
					} catch (Exception e) {
				    logger.error("Unable to delete device: {}", id);
				    e.printStackTrace();
				}
			}
		}
	}

	/**
	 *
	 *
	 * @see iot.agile.protocol.ble.devicemanager.DeviceManager#Batch(java.lang.
	 *      String, java.lang.String)
	 */
	@Override
	public void Batch(String operation, String arguments) {
		logger.debug("DeviceManager.Batch not implemented");
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see org.freedesktop.dbus.DBusInterface#isRemote()
	 */
	@Override
	public boolean isRemote() {
		return false;
	}

	/**
	 * Get device based on {@code DeviceDefinition}
	 *
	 * @param devDef
	 *            Device definition
	 * @return
	 */
	private Device getDevice(DeviceDefinition devDef) {
		String objectName = "iot.agile.Device";
    String objectPath = "/iot/agile/Device/"+devDef.getProtocol().replace("iot.agile.protocol.", "").toLowerCase() + devDef.address.replace(":", "");
     try {
			Device device = (Device) connection.getRemoteObject(objectName, objectPath);
			return device;
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Get device based on {@code DeviceDefinition}
	 *
	 * @param devDef
	 *            Device definition
	 * @return
	 */
	private Device getDevice(DeviceOverview devOverview) {
		String objectName = "iot.agile.Device";
		String objectPath = "/iot/agile/Device/"+devOverview.getProtocol().replace("iot.agile.protocol.", "").toLowerCase() + devOverview.id.replace(":", "");
 		try {
			Device device = (Device) connection.getRemoteObject(objectName, objectPath);
			return device;
		} catch (Exception e) {
			return null;
		}

	}

}
