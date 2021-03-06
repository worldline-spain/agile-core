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
/*
 * Copyright 2016 CREATE-NET
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iot.agile.http.resource;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iot.agile.http.service.DbusClient;
import iot.agile.object.DeviceOverview;
import iot.agile.object.DiscoveryStatus;
import iot.agile.object.ProtocolOverview;

/**
 *
 * @author Luca Capra <lcapra@create-net.org>
 */
@Path("/protocols")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProtocolManager {

	protected Logger logger = LoggerFactory.getLogger(ProtocolManager.class);

	@Inject
	DbusClient client;

	protected iot.agile.ProtocolManager getProtocolManager() throws DBusException {
		return client.getProtocolManager();
	}

	@GET
	@Path("/devices")
	public List<DeviceOverview> Devices() throws DBusException {
		return getProtocolManager().Devices();
	}

	@POST
	@Path("/{id}")
	public void Add(@PathParam("id") String protocol) throws DBusException {
		getProtocolManager().Add(protocol);
	}

	@DELETE
	@Path("/{id}")
	public void Remove(@PathParam("id") String protocol) throws DBusException {
		getProtocolManager().Remove(protocol);
	}

	@GET
	public List<ProtocolOverview> Protocols() throws DBusException {
		return getProtocolManager().Protocols();
	}

	@GET
	@Path("/discovery")
	public List<DiscoveryStatus> DiscoveryStatus() throws DBusException {
		return getProtocolManager().DiscoveryStatus();
	}

	@POST
	@Path("/discovery")
	public void StartDiscovery() throws DBusException {
		getProtocolManager().StartDiscovery();
	}

	@DELETE
	@Path("/discovery")
	public void StopDiscovery() throws DBusException {
		getProtocolManager().StopDiscovery();
	}

}
