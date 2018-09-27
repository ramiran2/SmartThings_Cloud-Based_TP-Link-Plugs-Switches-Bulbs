/*
TP-Link Plug and Switch Device Handler, 2018, Version 2
Copyright 2018 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this  file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific 
language governing permissions and limitations under the 
License.

Discalimer:  This Service Manager and the associated Device 
Handlers are in no way sanctioned or supported by TP-Link.  
All  development is based upon open-source data on the 
TP-Link Kasa Devices; primarily various users on GitHub.com.

	===== History =============================================
2018-09-21	Update to Version 2.3.0
2018-08-11	Update to Version 2.1.1
		a.	Support for update from a repo on smartthings website
		b.	Improved driver names
		c.	Added driver version
2018-01-31	Update to Version 2
		a.	Common file content for all bulb implementations,
			using separate files by model only.
		b.	User file-internal selection of Energy Monitor
			function enabling.
	===== Plug/Switch Type.  DO NOT EDIT ====================*/
	def deviceType = "Plug and Switch"			//	Plug/Switch
//	def deviceType = "Dimming Switch"		//	HS220 Only
//	===== Hub or Cloud Installation =========================*/
	def installType = "Kasa Account"
	//def installType = "Node.js Applet"
//	===========================================================

import java.text.SimpleDateFormat
import groovy.time.*

def devVer() { return "2.3.0" }

metadata {
	definition (name: "TP-Link Smart ${deviceType} - ${installType}",
				namespace: "ramiran2",
				author: "xKiller Maverick",
				deviceType: "${deviceType}",
				energyMonitorMode: "Standard",
				ocfDeviceType: "oic.d.switch",
				mnmn: "SmartThings",
				vid: "generic-switch-power",
				installType: "${installType}") {
		capability "Switch"
		capability "refresh"
		capability "polling"
		capability "Sensor"
		capability "Actuator"
		capability "Health Check"
		attribute "devVer", "string"
		if (deviceType == "Dimming Switch") {
			capability "Switch Level"
		}
	}
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc",
				nextState:"waiting"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff",
				nextState:"waiting"
				attributeState "waiting", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#15EE10",
				nextState:"waiting"
				attributeState "commsError", label:'Comms Error', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#e86d13",
				nextState:"waiting"
            }
			if (deviceType == "Dimming Switch") {
				tileAttribute ("device.level", key: "SLIDER_CONTROL") {
					attributeState "level", label: "Brightness: ${currentValue}", action:"switch level.setLevel", range: "(1..100)"
				}
			}
 			tileAttribute ("deviceError", key: "SECONDARY_CONTROL") {
				attributeState "deviceError", label: '${currentValue}'
			}
		}
        
		standardTile("refresh", "capability.refresh", width: 2, height: 1,  decoration: "flat") {
			state "default", label:"Refresh", action:"refresh.refresh"
		}
        
		main("switch")
		details("switch", "refresh")
	}

	def rates = [:]
	rates << ["1" : "Refresh every minutes (Not Recommended)"]
	rates << ["5" : "Refresh every 5 minutes"]
	rates << ["10" : "Refresh every 10 minutes"]
	rates << ["15" : "Refresh every 15 minutes"]
	rates << ["30" : "Refresh every 30 minutes (Recommended)"]

	preferences {
		if (installType == "Node.js Applet") {
			input("deviceIP", "text", title: "Device IP", required: true, displayDuringSetup: true)
			input("gatewayIP", "text", title: "Gateway IP", required: true, displayDuringSetup: true)
		}
		input name: "refreshRate", type: "enum", title: "Refresh Rate", options: rates, description: "Select Refresh Rate", required: false
	}
}

def compileForC() {
	// if using C mode, set this to true so that enums and colors are correct (due to ST issue of compile time evaluation)
	return false
}

// ===== Logging =====
void Logger(msg, logType = "debug") {
	def smsg = state?.showLogNamePrefix ? "${device.displayName} (v${devVer()}) | ${msg}" : "${msg}"
	def theId = lastN(device.getId().toString(),5)
	if(state?.enRemDiagLogging) {
		parent.saveLogtoRemDiagStore(smsg, logType, "${deviceType}-${theId}")
	} else {
		switch (logType) {
			case "trace":
				log.trace "|| ${smsg}"
				break
			case "debug":
				log.debug "${smsg}"
				break
			case "info":
				log.info "||| ${smsg}"
				break
			case "warn":
				log.warn "|| ${smsg}"
				break
			case "error":
				log.error "| ${smsg}"
				break
			default:
				log.debug "${smsg}"
				break
		}
	}
}

// Local Application Logging
void LogAction(msg, logType = "debug") {
	if(state?.debug) {
		Logger(msg, logType)
	}
}

// This will Print logs from the parent app when added to parent method that the child calls
def log(message, level = "trace") {
	def smsg = "PARENT_Log>> " + message
	LogAction(smsg, level)
	return null // always child interface call with a return value
}

//	===== Device Health Check =====
def useTrackedHealth() { return state?.useTrackedHealth ?: false }

def getHcTimeout() {
	def to = state?.hcTimeout
	return ((to instanceof Integer) ? to.toInteger() : 45)*60
}

void verifyHC() {
	if(useTrackedHealth()) {
		def timeOut = getHcTimeout()
		if(!val || val.toInteger() != timeOut) {
			Logger("verifyHC: Updating Device Health Check Interval to $timeOut")
			sendEvent(name: "checkInterval", value: timeOut, data: [protocol: "cloud"], displayed: false)
		}
	} else {
		sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson(["protocol":"cloud", "scheme":"untracked"]), displayed: false)
	}
	repairHealthStatus(null)
}

def modifyDeviceStatus(status) {
	if(status == null) { return }
	def val = status.toString() == "offline" ? "offline" : "online"
	if(val != getHealthStatus(true)) {
		sendEvent(name: "DeviceWatch-DeviceStatus", value: val.toString(), displayed: false, isStateChange: true)
		Logger("UPDATED: DeviceStatus Event: '$val'")
	}
}

def ping() {
	Logger("ping...")
	keepAwakeEvent()
}

def keepAwakeEvent() {
	def lastDt = state?.lastUpdatedDtFmt
	if(lastDt) {
		def ldtSec = getTimeDiffSeconds(lastDt)
		//log.debug "ldtSec: $ldtSec"
		if(ldtSec < 1900) {
			poll()
		}
	}
}

def healthNotifyOk() {
	def lastDt = state?.lastHealthNotifyDt
	if(lastDt) {
		def ldtSec = getTimeDiffSeconds(lastDt)
		def t0 = state.healthMsgWait ?: 3600
		if(ldtSec < t0) {
			return false
		}
	}
	return true
}

void repairHealthStatus(data) {
	Logger("repairHealthStatus($data)")
	if(state?.hcRepairEnabled != false) {
		if(data?.flag) {
			sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
			state?.healthInRepair = false
		} else {
			state.healthInRepair = true
			sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
			runIn(7, repairHealthStatus, [data: [flag: true]])
		}
	}
}

def getTimeDiffSeconds(strtDate, stpDate=null, methName=null) {
	//LogTrace("[GetTimeDiffSeconds] StartDate: $strtDate | StopDate: ${stpDate ?: "Not Sent"} | MethodName: ${methName ?: "Not Sent"})")
	if(strtDate) {
		//if(strtDate?.contains("dtNow")) { return 10000 }
		def now = new Date()
		def stopVal = stpDate ? stpDate.toString() : formatDt(now)
		def startDt = Date.parse("E MMM dd HH:mm:ss z yyyy", strtDate)
		def stopDt = Date.parse("E MMM dd HH:mm:ss z yyyy", stopVal)
		def start = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(startDt)).getTime()
		def stop = Date.parse("E MMM dd HH:mm:ss z yyyy", stopVal).getTime()
		def diff = (int) (long) (stop - start) / 1000
		//LogTrace("[GetTimeDiffSeconds] Results for '$methName': ($diff seconds)")
		return diff
	} else { return null }
}

def checkHealth() {
	def isOnline = (getHealthStatus() == "ONLINE") ? true : false
	if(isOnline || state?.healthMsg != true || state?.healthInRepair == true) { return }
	if(healthNotifyOk()) {
		def now = new Date()
		parent?.deviceHealthNotify(this, isOnline)
		state.lastHealthNotifyDt = getDtNow()
	}
}

def getDtNow() {
	def now = new Date()
	return formatDt(now)
}

def getHealthStatus(lowerCase=false) {
	def res = device?.getStatus()
	if(lowerCase) { return res.toString().toLowerCase() }
	return res.toString()
}

def lastCheckinEvent(checkin, isOnline) {
	def formatVal = state?.useMilitaryTime ? "MMM d, yyyy - HH:mm:ss" : "MMM d, yyyy - h:mm:ss a"
	def tf = new SimpleDateFormat(formatVal)
	tf.setTimeZone(getTimeZone())

	def lastChk = device.currentState("lastConnection")?.value
	def prevOnlineStat = device.currentState("onlineStatus")?.value

	def hcTimeout = getHcTimeout()
	def curConn = checkin ? tf.format(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", checkin)) : "Not Available"
	def curConnFmt = checkin ? formatDt(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", checkin)) : "Not Available"
	def curConnSeconds = (checkin && curConnFmt != "Not Available") ? getTimeDiffSeconds(curConnFmt) : 3000

	def onlineStat = isOnline.toString() == "true" ? "online" : "offline"

	state?.lastConnection = curConn?.toString()
	if(isStateChange(device, "lastConnection", curConnFmt.toString())) {
		LogAction("UPDATED | Last Device Check-in was: (${curConnFmt}) | Previous Check-in: (${lastChk})")
		sendEvent(name: 'lastConnection', value: curConnFmt?.toString(), isStateChange: true)
	} else { LogAction("Last Device Check-in was: (${curConnFmt}) | Original State: (${lastChk})") }

	lastChk = device.currentState("lastConnection")?.value
	def lastConnSeconds = (lastChk && lastChk != "Not Available") ? getTimeDiffSeconds(lastChk) : 3000

	if(hcTimeout && isOnline.toString() == "true" && curConnSeconds > hcTimeout && lastConnSeconds > hcTimeout) {
		onlineStat = "offline"
		LogAction("lastCheckinEvent: UPDATED onlineStatus: $onlineStat")
		Logger("lastCheckinEvent($checkin, $isOnline) | onlineStatus: $onlineStat | lastConnSeconds: $lastConnSeconds | hcTimeout: ${hcTimeout} | curConnSeconds: ${curConnSeconds}")
	} else {
		LogAction("lastCheckinEvent($checkin, $isOnline) | onlineStatus: $onlineStat | lastConnSeconds: $lastConnSeconds | hcTimeout: ${hcTimeout} | curConnSeconds: ${curConnSeconds}")
	}

	state?.onlineStatus = onlineStat
	modifyDeviceStatus(onlineStat)
	if(isStateChange(device, "onlineStatus", onlineStat?.toString())) {
		Logger("UPDATED | Online Status is: (${onlineStat}) | Original State: (${prevOnlineStat})")
		sendEvent(name: "onlineStatus", value: onlineStat, descriptionText: "Online Status is: ${onlineStat}", displayed: true, isStateChange: true, state: onlineStat)
	} else { LogAction("Online Status is: (${onlineStat}) | Original State: (${prevOnlineStat})") }
}

def getTimeZone() {
	def tz = null
	if(location?.timeZone) { tz = location?.timeZone }
	if(!tz) { Logger("getTimeZone: Hub TimeZone is not found ...", "warn") }
	return tz
}

//	===== Update when installed or setting changed =====
def initialize() {
	Logger("Initialized...")
	if(state?.swVersion != devVer()) {
		state.swVersion = devVer()
	}	
	state?.healthInRepair = false
	if(!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
		state.updatedLastRanAt = now()
		verifyHC()
		state.isInstalled = true
	} else {
		log.trace "initialize(): Ran within last 2 seconds - SKIPPING"
	}
}

def installed() {
	Logger("Installed...", "trace")
	runIn( 5, "initialize", [overwrite: true] )
	update()
}

def updated() {
	Logger("Updated...", "trace")
	runIn( 5, "initialize", [overwrite: true] )
	runIn(2, update)
}

def update() {
	Logger("Update...", "trace")
	state.deviceType = metadata.definition.deviceType
	state.installType = metadata.definition.installType
	unschedule()
	switch(refreshRate) {
		case "1":
			runEvery1Minute(refresh)
			log.info "Refresh Scheduled for every minute"
			break
		case "5":
			runEvery5Minutes(refresh)
			log.info "Refresh Scheduled for every 5 minutes"
			break
		case "10":
			runEvery10Minutes(refresh)
			log.info "Refresh Scheduled for every 10 minutes"
			break
		case "15":
			runEvery15Minutes(refresh)
			log.info "Refresh Scheduled for every 15 minutes"
			break
		default:
			runEvery30Minutes(refresh)
			log.info "Refresh Scheduled for every 30 minutes"
	}
	runIn(5, refresh)
}

void uninstalled() {
	Logger("Uninstalled...", "trace")
	if (state.installType == "Kasa Account") {
		def alias = device.label
		log.debug "Removing device ${alias} with DNI = ${device.deviceNetworkId}"
		parent.removeChildDevice(alias, device.deviceNetworkId)
	}
}

//	===== Basic Plug Control/Status =====
def on() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "deviceCommand", "commandResponse")
}

def off() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 0}}}', "deviceCommand", "commandResponse")
}

def setLevel(percentage) {
	percentage = percentage as int
    if (percentage == 0) {
    	percentage = 1
    }
	sendCmdtoServer("""{"smartlife.iot.dimmer":{"set_brightness":{"brightness":${percentage}}}}""", "deviceCommand", "commandResponse")
}

def poll() {
	Logger("Polling parent...")
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "refreshResponse")
	refresh()
}

def refresh(){
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "refreshResponse")
}

def commandResponse(cmdResponse) {
}

def refreshResponse(cmdResponse){
	def onOff = cmdResponse.system.get_sysinfo.relay_state
	if (onOff == 1) {
		onOff = "on"
	} else {
		onOff = "off"
	}
	sendEvent(name: "switch", value: onOff)
	def level = "0"
	if (state.deviceType == "Dimming Switch") {
		level = cmdResponse.system.get_sysinfo.brightness
	 	sendEvent(name: "level", value: level)
	}
	log.info "${device.name} ${device.label}: Power: ${onOff} / Dimmer Level: ${level}%"
}

//	----- SEND COMMAND TO CLOUD VIA SM -----
private sendCmdtoServer(command, hubCommand, action) {
	if (state.installType == "Node.js Applet") {
		sendCmdtoHub(command, hubCommand, action)
	} else {
		sendCmdtoCloud(command, hubCommand, action)
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
		action = ""
	} else {
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
	sendHubCommand(new physicalgraph.device.HubAction([
		headers: headers],
		device.deviceNetworkId,
		[callback: hubResponseParse]
	))
}

def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR at hubResponseParse TCP Timeout")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
	} else {
		actionDirector(action, cmdResponse)
		sendEvent(name: "deviceError", value: "OK")
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse":
			commandResponse(cmdResponse)
			break

		case "refreshResponse":
			refreshResponse(cmdResponse)
			break

		default:
			log.debug "at default"
	}
}

//	----- CHILD / PARENT INTERCHANGE TASKS -----
def syncAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
		log.info "Updated appServerUrl for ${device.name} ${device.label}"
}
