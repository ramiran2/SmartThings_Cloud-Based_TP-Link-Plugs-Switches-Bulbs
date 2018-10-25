/*
TP-Link Plug and Switch Device Handler, 2018, Version 3

	Copyright 2018 Dave Gutheinz, Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific
language governing permissions and limitations under the
License.

Discalimer: This Service Manager and the associated Device
Handlers are in no way sanctioned or supported by TP-Link.
All development is based upon open-source data on the
TP-Link devices; primarily various users on GitHub.com.

	======== Device Type Identifier - Do Not Change These Values =========================================================*/
	def deviceType()	{ return "Plug" }																				//	Plug
//	def deviceType()	{ return "Switch" }																				//	Switch
//	def deviceType()	{ return "Dimming Switch" }																		//	HS220 Only
//	======== Device Handler Icon =========================================================================================
	def deviceIcon()	{ return (deviceType() == "Plug") ? "st.Appliances.appliances17" : "st.Home.home30" }			//	Device Handler Icon
//	======== TP-Link Account or Local Server Installation ================================================================
//	def installType()	{ return "Cloud" }																				//	Davegut: Cloud
//	def installType()	{ return "Hub" }																				//	Davegut: Hub
	def installType()	{ return "Kasa Account" }																		//	Ramiran2: Kasa Account
//	def installType()	{ return "Node Applet" }																		//	Ramiran2: Node Applet
//	======== Developer Namespace =========================================================================================
//	def devNamespace()	{ return "davegut" }																			//	Davegut: Developer Namespace
	def devNamespace()	{ return "ramiran2" }																			//	Ramiran2: Developer Namespace
//	======== Repository Name =============================================================================================
//	def gitName()	{ return "SmartThings_Cloud-Based_TP-Link-Plugs-Switches-Bulbs" }									//	Davegut: Repository Name
	def gitName()	{ return "TP-Link-SmartThings" }																	//	Ramiran2: Repository Name
//	======== Device Name =================================================================================================
//	def devName()	{ return "(${installType()}) TP-Link ${deviceType()}" }												//	Davegut: Device Name
	def devName()	{ return "TP-Link Smart ${deviceType()} - ${installType()}" }										//	Ramiran2: Device Name
//	======== Other System Values =========================================================================================
	def devAuthor()	{ return "Dave Gutheinz, Anthony Ramirez" }															//	Device Handler Author
	def devVer()	{ return "3.4.0" }																					//	Device Handler Version
	def ocfValue()	{ return "oic.d.smartplug" }																		//	Open Connectivity Foundation Device Type: Smart Plug
//	def ocfValue()	{ return "oic.d.switch" }																			//	Open Connectivity Foundation Device Type: Switch
	def vidValue()	{ return "generic-switch-power" }																	//	Vendor ID: Switch
//	def vidValue()	{ return "generic-dimmer-power" }																	//	Vendor ID: Dimmer
//	======================================================================================================================

metadata {
	definition (name: "${devName()}", namespace: "${devNamespace()}", author: "${devAuthor()}", ocfDeviceType: "${ocfValue()}", mnmn: "SmartThings", vid: "${vidValue()}") {
		capability "Switch"
		capability "refresh"
		capability "Health Check"
		if (deviceType() == "Dimming Switch") {
			capability "Switch Level"
		}
		attribute "devVer", "string"
		attribute "devTyp", "string"
	}
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action: "switch.off", icon: "${deviceIcon()}", backgroundColor: "#00a0dc",
				nextState: "waiting"
				attributeState "off", label:'${name}', action: "switch.on", icon: "${deviceIcon()}", backgroundColor: "#ffffff",
				nextState: "waiting"
				attributeState "waiting", label:'${name}', action: "switch.on", icon: "${deviceIcon()}", backgroundColor: "#15EE10",
				nextState: "waiting"
				attributeState "Unavailable", label:'Unavailable', action: "switch.on", icon: "${deviceIcon()}", backgroundColor: "#e86d13",
				nextState: "waiting"
			}
			tileAttribute ("deviceError", key: "SECONDARY_CONTROL") {
				attributeState "deviceError", label: '${currentValue}'
			}
		}
		standardTile("refresh", "capability.refresh", width: 2, height: 1, decoration: "flat") {
			state "default", label: "Refresh", action: "refresh.refresh"
		}
		main("switch")
		details("switch", "refresh")
	}
	preferences {
		if (installType() == "Node Applet") {
			input ("deviceIP", "text", title: "Device IP", required: true, image: getDevImg("samsunghub.png"))
			input ("gatewayIP", "text", title: "Gateway IP", required: true, image: getDevImg("router.png"))
		}
		input ("refreshRate", "enum", title: "Device Refresh Rate", options: ["1" : "Refresh every minute", "5" : "Refresh every 5 minutes", "10" : "Refresh every 10 minutes", "15" : "Refresh every 15 minutes", "30" : "Refresh every 30 minutes"], image: getDevImg("refresh.png"))
	}
}

//	===== Update when installed or setting changed =====
def initialize() {
	log.info "Initialized ${device.label}..."
	sendEvent(name: "devVer", value: devVer(), displayed: false)
	sendEvent(name: "devTyp", value: deviceType(), displayed: false)
	sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson(["protocol" : "cloud", "scheme" : "untracked"]), displayed: false)
}

def ping() {
	refresh()
}

def installed() {
	updated()
}

def updated() {
	log.info "Updated ${device.label}..."
	unschedule()
	//	Update Refresh Rate Preference
	if (refreshRate) {
		setRefreshRate(refreshRate)
	} else {
		setRefreshRate(30)
	}
	sendEvent(name: "devVer", value: devVer(), displayed: false)
	sendEvent(name: "devTyp", value: deviceType(), displayed: false)
	runIn(2, refresh)
	runIn( 5, "initialize")
}

void uninstalled() {
	if (installType() == "Kasa Account") {
		def alias = device.label
		log.debug "Removing device ${alias} with DNI = ${device.deviceNetworkId}"
		parent.removeChildDevice(alias, device.deviceNetworkId)
	}
}

//	===== Basic Plug Control/Status =====
def on() {
	sendCmdtoServer('{"system" :{"set_relay_state" :{"state" : 1}}}', "deviceCommand", "commandResponse")
}

def off() {
	sendCmdtoServer('{"system" :{"set_relay_state" :{"state" : 0}}}', "deviceCommand", "commandResponse")
}

def setLevel(percentage) {
	if (percentage < 0 || percentage > 100) {
		log.error "$device.name $device.label: Entered brightness is not from 0...100"
		percentage = 50
	}
	percentage = percentage as int
	sendCmdtoServer("""{"smartlife.iot.dimmer" :{"set_brightness" :{"brightness" :${percentage}}}}""", "deviceCommand", "commandResponse")
}

def refresh(){
	sendCmdtoServer('{"system" :{"get_sysinfo" :{}}}', "deviceCommand", "refreshResponse")
}

def refreshResponse(cmdResponse){
	def onOff = cmdResponse.system.get_sysinfo.relay_state
	if (onOff == 1) {
		onOff = "on"
	} else {
		onOff = "off"
	}
	sendEvent(name: "switch", value: onOff)
	if (deviceType() == "Dimming Switch") {
		def level = cmdResponse.system.get_sysinfo.brightness
	 	sendEvent(name: "level", value: level)
		log.info "${device.name} ${device.label}: Power: ${onOff} / Dimmer Level: ${level}%"
	} else {
		log.info "${device.name} ${device.label}: Power: ${onOff}"
	}
}

//	===== Send the Command =====
private sendCmdtoServer(command, hubCommand, action) {
	try {
		if (installType() == "Node Applet") {
			sendCmdtoHub(command, hubCommand, action)
		} else {
			sendCmdtoCloud(command, hubCommand, action)
		}
	} catch (ex) {
		log.error "Sending Command Exception: ", ex
	}
}

private sendCmdtoCloud(command, hubCommand, action){
	def appServerUrl = getDataValue("appServerUrl")
	def deviceId = getDataValue("deviceId")
	def cmdResponse = parent.sendDeviceCmd(appServerUrl, deviceId, command)
	String cmdResp = cmdResponse.toString()
	if (cmdResp.substring(0,5) == "ERROR"){
		def errMsg = cmdResp.substring(7,cmdResp.length())
		log.error "${device.name} ${device.label}: ${errMsg}"
		sendEvent(name: "switch", value: "commsError", descriptionText: errMsg)
		sendEvent(name: "deviceError", value: errMsg)
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
		action = ""
	} else {
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
		sendEvent(name: "deviceError", value: "OK")
	}
	actionDirector(action, cmdResponse)
}

private sendCmdtoHub(command, hubCommand, action){
	def headers = [:]
	headers.put("HOST", "$gatewayIP:8082")	//	Same as on Hub.
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("action", action)
	headers.put("command", hubCommand)
	sendHubCommand(new physicalgraph.device.HubAction([headers: headers], device.deviceNetworkId, [callback: hubResponseParse]))
}

def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR at hubResponseParse TCP Timeout")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
	} else {
		sendEvent(name: "deviceError", value: "OK")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
		actionDirector(action, cmdResponse)
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse" :
			refresh()
			break
		case "refreshResponse" :
			refreshResponse(cmdResponse)
			break
		default:
			log.debug "Interface Error.	See SmartApp and Device error message."
	}
}

//	===== Child / Parent Interchange =====
def syncAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
	log.info "Updated appServerUrl for ${device.name} ${device.label}"
}

def setLightTransTime(lightTransTime) {
	return
}

def setRefreshRate(refreshRate) {
	switch(refreshRate) {
		case "1" :
			runEvery1Minute(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every minute"
			break
		case "5" :
			runEvery5Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 5 minutes"
			break
		case "10" :
			runEvery10Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 10 minutes"
			break
		case "15" :
			runEvery15Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 15 minutes"
			break
		case "30" :
			runEvery30Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 30 minutes"
			break
		default:
			runEvery30Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 30 minutes"
	}
}

//	======== GitHub Values =====================================================================================================================================================================================
	def getDevImg(imgName)	{ return "https://raw.githubusercontent.com/${gitPath()}/images/$imgName" }
	def gitBranch()	{ return "master" }
	def gitRepo()		{ return "${devNamespace()}/${gitName()}" }
	def gitPath()		{ return "${gitRepo()}/${gitBranch()}"}
//	============================================================================================================================================================================================================