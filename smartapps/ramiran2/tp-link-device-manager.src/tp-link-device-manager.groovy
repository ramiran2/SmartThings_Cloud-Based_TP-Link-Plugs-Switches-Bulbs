/*
TP-Link Connect Service Manager, 2018 Version 2

Copyright 2018 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

TP-Link Device Manager, 2018 Version 3

Copyright 2018 Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License"); you 
may not use this file except in compliance with the License. You may
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

##### Discalimer: This Service Manager and the associated Device
Handlers are in no way sanctioned or supported by TP-Link. All
development is based upon open-source data on the TP-Link Kasa Devices;
primarily various users on GitHub.com.

##### Notes #####
1.	This Service Manager is designed to install and manage TP-Link
	bulbs, plugs, and switches using their respective device handlers.
2.	Please direct comments to the SmartThings community thread
	'TP-Link Device Manager SmartThings Integration'.
*/

definition(
	name: "${appLabel()}",
	namespace: "${appNamespace()}",
	author: "${appAuthor()}",
	description: "${textDesc()}",
	category: "Convenience",
	iconUrl: "${getAppImg("kasa.png")}",
	iconX2Url: "${getAppImg("kasa.png")}",
	iconX3Url: "${getAppImg("kasa.png")}",
	singleInstance: true
)

def appVersion() { return "3.4.0" }
def appVerDate() { return "10-10-2018" }
def driverVersionsMin() {
	return [
		"colorbulbenergymonitor":["val":320, "desc":"3.2.0"],
		"colorbulb":["val":320, "desc":"3.2.0"],
		"dimmingswitch":["val":320, "desc":"3.2.0"],
		"energymonitorplug":["val":320, "desc":"3.2.0"],
		"plug":["val":320, "desc":"3.2.0"],
		"switch":["val":320, "desc":"3.2.0"],
		"softwhitebulbenergymonitor":["val":320, "desc":"3.2.0"],
		"softwhitebulb":["val":320, "desc":"3.2.0"],
		"tunablewhitebulbenergymonitor":["val":320, "desc":"3.2.0"],
		"tunablewhitebulb":["val":320, "desc":"3.2.0"]
	]
}

preferences {
	page(name: "startPage")
	page(name: "authPage")
	page(name: "mainPage")
	page(name: "selectDevices")
	page(name: "tokenPage")
	page(name: "devMode")
	page(name: "devModeTestingPage")
	page(name: "aboutPage")
	page(name: "changeLogPage")
	page(name: "uninstallPage")
}

def setInitialStates() {
	if (!state.TpLinkToken) {state.TpLinkToken = null}
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
}

//	----- START PAGE -----
def startPage() {
	setInitialStates()
	if ("${userName}" =~ null || "${userPassword}" =~ null ){
		return authPage()
	} else {
		return mainPage()
	}
}

//	----- AUTH PAGE -----
def authPage() {
	def authPageText = "If possible, open the IDE and select Live Logging. Then, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete. " + "Your current token:" + "${state.TpLinkToken}" +
		"\n\rAvailable actions:\n\r" +
		"Activate Account: Login into TP-Link Account and obtains token and adds devices.\n\r" +
		"Update Account: Updates the token and credentials or you can remove the token."
	def driverVersionText = "TP-Link Kasa Drivers for SmartThings:" + "${driverVersionsMin()}" + "\n" + "Note: Drivers from the old the original repository will not work with this version of the application."
	return dynamicPage(
		name: "authPage",
		title: "Login Page",
		install: false,
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Driver Version:", hideable: true, hidden: true) {
			paragraph title: "Information:", authPageText
			paragraph title: "Driver Version:", driverVersionText
		}
		section("Account Configuration:") {
			input(
				"userName", "email",
				title: "TP-Link Kasa Email Address",
				required: true,
				image: getAppImg("email.png")
			)
			input(
				"userPassword", "password",
				title: "TP-Link Kasa Account Password",
				required: true,
				image: getAppImg("password.png")
			)
		}
		section("User Configuration:") {
			input(
				"userSelectedDevMode", "bool",
				title: "Do you want to enable developer mode?",
				submitOnChange: true,
				image: getAppImg("developer.png")
			)
			input(
				"userSelectedOptionTwo", "enum",
				title: "What do you want to do?",
				required: true,
				multiple: false,
				submitOnChange: true,
				metadata: [values:["Activate Account", "Update Account"]],
				image: getAppImg("settings.png")
			)
		}
		section("Page Selector:") {
			if (userSelectedOptionTwo =~ "Activate Account" || userSelectedOptionTwo =~ "Update Account") {
				paragraph pageSelectorText(), image: getAppImg("pageselector.png")
			} else {
				paragraph pageSelectorNullText(), image: getAppImg("error.png")
			}
			if (userSelectedOptionTwo =~ "Activate Account") {
				href "selectDevices", title: "Device Manager Page", description: "Tap to view", image: getAppImg("selectdevices.png")
			}
			if (userSelectedOptionTwo =~ "Update Account") {
				href "tokenPage", title: "Token Manager Page", description: "Tap to view", image: getAppImg("tokenpage.png")
			}
		}
	}
}

//	----- MAIN PAGE -----
def mainPage() {
	def mainPageText = "Your current token:" + "${state.TpLinkToken}" +
		"\n\rAvailable actions:\n\r" +
		"Initial Install: Login into TP-Link Account and obtains token and adds devices.\n\r" +
		"Add/Remove Devices: Only Add/Remove Devices.\n\r" +
		"Update Token: Updates the token or you can remove the token."
	def errorMsgCom = "None"
	if (state.currentError != null){
		errorMsgCom = "Error communicating with cloud:\n\r" + "${state.currentError}" +
			"\n\rPlease resolve the error and try again."
	}
	return dynamicPage(
		name: "mainPage",
		title: "Settings Page",
		install: false,
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
        section("Information and Diagnostics:", hideable: true, hidden: true) {
			paragraph title: "Information:", mainPageText
			paragraph title: "Communication Error:", errorMsgCom
		}
		section("User Configuration:") {
			input ("appIcons", "bool", title: "Disable App Icons?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("noicon.png"))
			input(
				"userSelectedOptionOne", "enum",
				title: "What do you want to do?",
				required: true,
				multiple: false,
				submitOnChange: true,
				metadata: [values:["Initial Install", "Add/Remove Devices", "Update Token"]],
				image: getAppImg("settings.png")
			)
		}
		section("Page Selector:") {
			if (userSelectedOptionOne =~ "Initial Install" || userSelectedOptionOne =~ "Add/Remove Devices" || userSelectedOptionOne =~ "Update Token") {
				paragraph pageSelectorText(), image: getAppImg("pageselector.png")
			} else {
				paragraph pageSelectorNullText(), image: getAppImg("error.png")
			}
			if (userSelectedOptionOne =~ "Initial Install") {
				href "authPage", title: "Login Page", description: "Tap to view", image: getAppImg("authpage.png")
			}
			if (userSelectedOptionOne =~ "Add/Remove Devices") {
				href "selectDevices", title: "Device Manager Page", description: "Tap to view", image: getAppImg("selectdevices.png")
			}
			if (userSelectedOptionOne =~ "Update Token") {
				href "tokenPage", title: "Token Manager Page", description: "Tap to view", image: getAppImg("tokenpage.png")
			}
		}
		section("Help and Feedback:") {
			if (userSelectedDevMode){
				href "devMode", title: "Developer Page", description: "Tap to view", image: getAppImg("developer.png")
			}
			href url: getWikiPageUrl(), style:"embedded", required:false, title:"View the Projects Wiki", description:"Tap to open in browser", state: "complete", image: getAppImg("wiki.png")
			href url: getIssuePageUrl(), style:"embedded", required:false, title:"Report | View Issues", description:"Tap to open in browser", state: "complete", image: getAppImg("issue.png")
		}
		section("About and Changelog:") {
			href "aboutPage", title: "About Page", description: "Tap to view", image: getAppImg("aboutpage.png")
			href "changeLogPage", title: "Changelog Page", description: "Tap to view", image: getAppImg("changelogpage.png")
		}
		section("Uninstall:") {
			href "uninstallPage", title: "Uninstall Page", description: "Tap to view", image: getAppImg("uninstall.png")
		}
	}
}

//	----- SELECT DEVICES PAGE -----
def selectDevices() {
	if (userSelectedOptionTwo =~ "Activate Account") {
		getToken()
	}
	getDevices()
	def devices = state.devices
	def errorMsgDev = "None"
	def newDevices = [:]
	def oldDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (isChild) {
			oldDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
		if (!isChild) {
			newDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (devices == [:]) {
		errorMsgDev = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+
		"that all devices are in 'Local Control Only'. Correct them then " +
		"rerun the application."
	}
	if (newDevices == [:] && oldDevices == [:]) {
		errorMsgDev = "No new devices to add. Are you sure they are in Remote " +
		"Control Mode?"
	}
	if (oldDevices == [:] && userSelectedRemoveMode) {
		errorMsgDev = "No current devices to remove from smart things."
	}
	settings.userSelectedDevicesRemove = null
	settings.userSelectedDevicesAdd = null
	def TPLinkDevicesMsg = "TP-Link Token is ${state.TpLinkToken}\n\r" +
		"Devices that have not been previously installed and are not in 'Local " +
		"WiFi control only' will appear below. Tap below to see the list of " +
		"TP-Link Kasa Devices available select the ones you want to connect to " +
		"SmartThings.\n\r" + "Press Done when you have selected the devices you " +
		"wish to add, then press Done again to install the devices. Press < " +
		"to return to the previous page."
	def sendingDataSuccess()	{ return "Data Sent to All Devices" }
	def sendingDataFailed()	{ return "Ready to Send Data to All Devices" }
	return dynamicPage(
		name: "selectDevices",
		title: "Device Manager Page",
		install: true,
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics:", hideable: true, hidden: true) {
			paragraph title: "Information:", TPLinkDevicesMsg
			paragraph title: "Device Error:", errorMsgDev
		}
		section("Device Controller:") {
			input(
				"userSelectedRemoveMode", "bool",
				title: "Do you want to enable device removal mode?",
				defaultValue: false,
				submitOnChange: true,
				image: getAppImg("deviceremover.png")
			)
			if (userSelectedRemoveMode) {
				input(
					"userSelectedDevicesRemove", "enum",
					required: true,
					multiple: true,
					submitOnChange: true,
					title: "Select Devices (${oldDevices.size() ?: 0} found)",
					metadata: [values:oldDevices],
					image: getAppImg("devices.png")
				)
			} else {
				input(
					"userSelectedDevicesAdd", "enum",
					required: true,
					multiple: true,
					submitOnChange: true,
					title: "Select Devices (${newDevices.size() ?: 0} found)",
					metadata: [values:newDevices],
					image: getAppImg("devices.png")
				)
			}
		}
		section("Saving Settings:") {
			if (userLightTransTime != null || userRefreshRate != null){
				if (userLightTransTime) {
					sendEvent(name: "lightingTransitionTime", value: userLightTransTime)
				}
				if (userRefreshRate) {
					sendEvent(name: "deviceRefreshRate", value: userRefreshRate)
				}
				paragraph sendingDataSuccess(), image: getAppImg("send.png")
			} else {
				paragraph sendingDataFailed(), image: getAppImg("issue.png")
			}
		}
		section("Device Configuration:") {
			input(
				"userLightTransTime", "number",
				required: true,
				multiple: false,
				submitOnChange: true,
				title: "Lighting Transition Time",
				description: "0 to 60 seconds",
				image: getAppImg("transition.png")
			)
			input(
				"userRefreshRate", "enum",
				required: true,
				multiple: false,
				submitOnChange: true,
				title: "Device Refresh Rate",
				metadata: [values:["5" : "Refresh every 5 minutes", "10" : "Refresh every 10 minutes", "15" : "Refresh every 15 minutes", "30" : "Refresh every 30 minutes"]],
				image: getAppImg("refresh.png")
			)
		}
	}
}

//	----- TOKEN MANAGER PAGE -----
def tokenPage () {
	def mainPageText = "Available actions:\n\r" +
		"Update Token: Updates the token.\n\r" +
		"Remove Token: Removes the token."
		def errorMsgToken = "None"
		if (state.TpLinkToken == null){
			errorMsgToken = "You will be unable to control your devices until you get a new token."
		}
	dynamicPage(name: "tokenPage", title: "Token Manager Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics:", hideable: true, hidden: true) {
			paragraph title: "Information:", mainPageText
			paragraph title: "Account Error:", errorMsgToken
		}
		section("Account Status:") {
			if (state.TpLinkToken != null){
				paragraph tokenInfoOnline(), image: getAppImg("success.png")
			} else {
				paragraph tokenInfoOffline(), image: getAppImg("error.png")
			}
		}
		section("Account Configuration:") {
			input(
				"userSelectedOptionThree", "enum",
				title: "What do you want to do?",
				required: true,
				multiple: false,
				submitOnChange: true,
				metadata: [values:["Update Token", "Delete Token"]],
				image: getAppImg("token.png")
			)
			if (userSelectedOptionThree =~ "Update Token") {
				getToken()
			}
			if (userSelectedOptionThree =~ "Delete Token") {
				state.TpLinkToken = null
			}
		}
	}
}

//	----- DEVELOPER PAGE -----
def devMode() {
	getDevices()
	def devices = state.devices
	def newDevices = [:]
	def oldDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (isChild) {
			oldDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
		if (!isChild) {
			newDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	def hub = location.hubs[0]
	def hubId = hub.id
	return dynamicPage(
		name: "devMode",
		title: "Developer Page",
		install: false,
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Application Information:", hideable: true, hidden: true) {
			paragraph title: "TP-Link Token:", "${state.TpLinkToken}"
			paragraph title: "Hub:", "${hub}"
			paragraph title: "Hub ID:", "${hubId}"
			paragraph title: "Username:", "${userName}"
			paragraph title: "Password:", "${userPassword}"
			paragraph title: "Managed Devices:", "${oldDevices}"
			paragraph title: "New Devices:", "${newDevices}"
		}
		section("Page Selector:") {
			href "startPage", title: "Start Page", description: "Tap to view", image: getAppImg("startpage.png")
			href "authPage", title: "Login Page", description: "Tap to view", image: getAppImg("authpage.png")
			href "mainPage", title: "Settings Page", description: "Tap to view", image: getAppImg("mainpage.png")
			href "selectDevices", title: "Device Manager Page", description: "Tap to view", image: getAppImg("selectdevices.png")
			href "tokenPage", title: "Token Manager Page", description: "Tap to view", image: getAppImg("tokenpage.png")
			href "devMode", title: "Developer Page", description: "Tap to view", image: getAppImg("developer.png")
			if (devModeLoaded){
				href "devModeTestingPage", title: "Developer Testing Page", description: "Tap to view", image: getAppImg("testing.png")
			}
			href "aboutPage", title: "About Page", description: "Tap to view", image: getAppImg("aboutpage.png")
			href "changeLogPage", title: "Changelog Page", description: "Tap to view", image: getAppImg("changelogpage.png")
			href "uninstallPage", title: "Uninstall Page", description: "Tap to view", image: getAppImg("uninstallpage.png")
			href "forceUninstallPage", title: "Force Uninstall Page", description: "Tap to view", image: getAppImg("forceuninstallpage.png")
		}
		section("User Configuration:") {
			input (name: "userSelectedReload", type: "bool", title: "Do you want to resync your devices current state?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("sync.png"))
			if (userSelectedReload){
				setInitialStates()
			}
			input(
				"devModeLoaded", "bool",
				title: "Do you want to enable developer testing page?",
				submitOnChange: true,
				image: getAppImg("developer.png")
			)
		}
	}
}

//	----- DEVELOPER TESTING PAGE -----
def devModeTestingPage() {
	getDevices()
	def devices = state.devices
	def newDevices = [:]
	def oldDevices = [:]
	def errorMsgCom = "None"
	def errorMsgDev = "None"
	def errorMsgNew = "None"
	def errorMsgOld = "None"
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (isChild) {
			oldDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
		if (!isChild) {
			newDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (state.currentError != null){
		errorMsgCom = "Error communicating with cloud:\n\r" + "${state.currentError}" +
			"\n\rPlease resolve the error and try again."
	}
	if (devices == [:]) {
		errorMsgDev = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+
		"that all devices are in 'Local Control Only'. Correct them then " +
		"rerun the application."
	}
	if (newDevices == [:] && oldDevices == [:]) {
		errorMsgNew = "No new devices to add. Are you sure they are in Remote " +
		"Control Mode?"
	}
	if (oldDevices == [:] && userSelectedRemoveMode) {
		errorMsgOld = "No current devices to remove from smart things."
	}
	return dynamicPage(
		name: "devModeTestingPage",
		title: "Developer Testing Page",
		install: false,
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Application Information:", hideable: true, hidden: true) {
			paragraph title: "Communication Error:", errorMsgCom
			paragraph title: "Finding Devices Error:", errorMsgDev
			paragraph title: "New Devices Error:", errorMsgNew
			paragraph title: "Current Devices Error:", errorMsgOld
			paragraph title: "Error Count:", "${state.errorCount}"
			paragraph title: "Current Error:", "${state.currentError}"
			paragraph title: "Error Messages:", "${errMsg}"
		}
		section("User Configuration:") {
			input(
				"userSelectedDevMode", "bool",
				title: "Do you want to enable developer mode?",
				submitOnChange: true,
				image: getAppImg("developer.png")
			)
			input(
				"userSelectedOptionTwo", "enum",
				title: "What do you want to do?",
				required: false,
				multiple: false,
				submitOnChange: true,
				metadata: [values:["Activate Account", "Update Account"]],
				image: getAppImg("settings.png")
			)
			input(
				"userSelectedOptionOne", "enum",
				title: "What do you want to do?",
				required: false,
				multiple: false,
				submitOnChange: true,
				metadata: [values:["Initial Install", "Add/Remove Devices", "Update Token"]],
				image: getAppImg("settings.png")
			)
		}
		section("Account Configuration:") {
			input(
				"userName", "email",
				title: "TP-Link Kasa Email Address",
				required: false,
				image: getAppImg("email.png")
			)
			input(
				"userPassword", "password",
				title: "TP-Link Kasa Account Password",
				required: false,
				image: getAppImg("password.png")
			)
			input(
				"userSelectedOptionThree", "enum",
				title: "What do you want to do?",
				required: false,
				multiple: false,
				submitOnChange: true,
				metadata: [values:["Update Token", "Delete Token"]],
				image: getAppImg("token.png")
			)
		}
		section("Device Configuration:") {
			input(
				"userSelectedRemoveMode", "bool",
				title: "Do you want to enable device removal mode?",
				submitOnChange: true,
				image: getAppImg("deviceremover.png")
			)
			input(
				"userSelectedDevicesRemove", "enum",
				required: false,
				multiple: true,
				submitOnChange: true,
				title: "Select Devices (${oldDevices.size() ?: 0} found)",
				metadata: [values:oldDevices],
				image: getAppImg("devices.png")
			)
			input(
				"userSelectedDevicesAdd", "enum",
				required: false,
				multiple: true,
				submitOnChange: true,
				title: "Select Devices (${newDevices.size() ?: 0} found)",
				metadata: [values:newDevices],
				image: getAppImg("devices.png")
			)
			input(
				"userLightTransTime", "number",
				required: true,
				multiple: false,
				submitOnChange: true,
				title: "Lighting Transition Time",
				description: "0 to 60 seconds",
				image: getAppImg("transition.png")
			)
			input(
				"userRefreshRate", "enum",
				required: true,
				multiple: false,
				submitOnChange: true,
				title: "Device Refresh Rate",
				metadata: [values:["5" : "Refresh every 5 minutes", "10" : "Refresh every 10 minutes", "15" : "Refresh every 15 minutes", "30" : "Refresh every 30 minutes"]],
				image: getAppImg("refresh.png")
			)
		}
	}
}

//	----- ABOUT PAGE -----
def aboutPage() {
	dynamicPage(name: "aboutPage", title: "About Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png", true)
		}
		section("Donations:") {
			href url: textDonateLink(), style:"external", required: false, title:"Donations", description:"Tap to open in browser", state: "complete", image: getAppImg("donate.png")
		}
		section("Credits:") {
			paragraph title: "Creator:", "Dave G. (@DaveGut)", state: "complete"
			paragraph title: "Co-Author:", "Anthony R. (@ramiran2)", state: "complete"
			paragraph title: "Collaborator:", "Anthony S. (@tonesto7)", state: "complete"
		}
		section("App Change Details:") {
			href "changeLogPage", title: "View App Revision History", description: "Tap to view", image: getAppImg("changelogpage.png")
		}
		section("Licensing Info:") {
			paragraph "${textCopyright()}\n${textLicense()}"
		}
	}
}

//	----- CHANGELOG PAGE -----
def changeLogPage () {
	dynamicPage(name: "changeLogPage", title: "Changelog Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Changelog:") {
			paragraph title: "What's New in this Release...", "", state: "complete", image: getAppImg("new.png")
			paragraph appVerInfo()
		}
	}
}

//	----- UNINSTALL PAGE -----
def uninstallPage() {
	dynamicPage(name: "uninstallPage", title: "Uninstall Page", install: false, uninstall: true) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information:") {
			paragraph "This will uninstall the App, All Automation Apps and Child Devices.\n\nPlease make sure that any devices created by this app are removed from any routines/rules/smartapps before tapping Remove."
		}
		remove("Remove ${appLabel()} and Devices!", "WARNING!!!", "Last Chance to Stop!\nThis action is not reversible\n\nThis App, All Devices, and Automations will be removed")
	}
}

def getDevices() {
	def currentDevices = getDeviceData()
	state.devices = [:]
	def devices = state.devices
	currentDevices.each {
		def device = [:]
		device["deviceMac"] = it.deviceMac
		device["alias"] = it.alias
		device["deviceModel"] = it.deviceModel
		device["deviceId"] = it.deviceId
		device["appServerUrl"] = it.appServerUrl
		devices << ["${it.deviceMac}": device]
		def isChild = getChildDevice(it.deviceMac)
		if (isChild) {
			isChild.syncAppServerUrl(it.appServerUrl)
		}
		log.info "Device ${it.alias} added to devices array"
	}
}

def removeDevices() {
	userSelectedDevicesRemove.each { dni ->
		try{
			def isChild = getChildDevice(dni)
			if (isChild) {
				def delete = isChild
				delete.each { deleteChildDevice(it.deviceNetworkId, true) }
			}
		} catch (e) {
			log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
}

def getWebData(params, desc, text=true) {
	try {
		log.info "getWebData: ${desc} data"
		httpGet(params) { resp ->
			if(resp.data) {
				if(text) {
					return resp?.data?.text.toString()
				} else { return resp?.data }
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			log.warn "${desc} file not found"
		} else {
			log.error "getWebData(params: $params, desc: $desc, text: $text) Exception:", ex
		}
		return "${label} info not found"
	}
}

def addDevices() {
	def tpLinkModel = [:]
	//	Plug-Switch Devices (no energy monitor capability)
	tpLinkModel << ["HS100" : "TP-Link Smart Plug - Kasa Account"]						//	HS100
	tpLinkModel << ["HS103" : "TP-Link Smart Plug - Kasa Account"]						//	HS103
	tpLinkModel << ["HS105" : "TP-Link Smart Plug - Kasa Account"]						//	HS105
	tpLinkModel << ["HS200" : "TP-Link Smart Switch - Kasa Account"]					//	HS200
	tpLinkModel << ["HS210" : "TP-Link Smart Switch - Kasa Account"]					//	HS210
	tpLinkModel << ["KP100" : "TP-Link Smart Plug - Kasa Account"]						//	KP100
	//	Dimming Switch Devices
	tpLinkModel << ["HS220" : "TP-Link Smart Dimming Switch - Kasa Account"]			//	HS220
	//	Energy Monitor Plugs
	tpLinkModel << ["HS110" : "TP-Link Smart Energy Monitor Plug - Kasa Account"]		//	HS110
	tpLinkModel << ["HS115" : "TP-Link Smart Energy Monitor Plug - Kasa Account"]		//	HS110
	//	Soft White Bulbs
	tpLinkModel << ["KB100" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	KB100
	tpLinkModel << ["LB100" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	LB100
	tpLinkModel << ["LB110" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	LB110
	tpLinkModel << ["KL110" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	KL110
	tpLinkModel << ["LB200" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	LB200
	//	Tunable White Bulbs
	tpLinkModel << ["LB120" : "TP-Link Smart Tunable White Bulb - Kasa Account"]		//	LB120
	tpLinkModel << ["KL120" : "TP-Link Smart Tunable White Bulb - Kasa Account"]		//	KL120
	//	Color Bulbs
	tpLinkModel << ["KB130" : "TP-Link Smart Color Bulb - Kasa Account"]				//	KB130
	tpLinkModel << ["LB130" : "TP-Link Smart Color Bulb - Kasa Account"]				//	LB130
	tpLinkModel << ["KL130" : "TP-Link Smart Color Bulb - Kasa Account"]				//	KL130
	tpLinkModel << ["LB230" : "TP-Link Smart Color Bulb - Kasa Account"]				//	LB230

	def hub = location.hubs[0]
	def hubId = hub.id
	userSelectedDevicesAdd.each { dni ->
		try {
			def isChild = getChildDevice(dni)
			if (!isChild) {
				def device = state.devices.find { it.value.deviceMac == dni }
				def deviceModel = device.value.deviceModel.substring(0,5)
				addChildDevice(
					"ramiran2",
					tpLinkModel["${deviceModel}"],
					device.value.deviceMac,
					hubId, [
						"label": device.value.alias,
							"name": device.value.deviceModel,
						"data": [
							"deviceId" : device.value.deviceId,
							"appServerUrl": device.value.appServerUrl,
						]
					]
				)
				log.info "Installed TP-Link $deviceModel with alias ${device.value.alias}"
			}
		} catch (e) {
			log.debug "Error Adding ${deviceModel}: ${e}"
		}
	}
}

//	----- GET A NEW TOKEN FROM CLOUD -----
def getToken() {
	def hub = location.hubs[0]
	def cmdBody = [
		method: "login",
		params: [
			appType: "Kasa_Android",
			cloudUserName: "${userName}",
			cloudPassword: "${userPassword}",
			terminalUUID: "${hub.id}"
		]
	]
	def getTokenParams = [
		uri: "https://wap.tplinkcloud.com",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getTokenParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			state.TpLinkToken = resp.data.result.token
			log.info "TpLinkToken updated to ${state.TpLinkToken}"
			sendEvent(name: "TokenUpdate", value: "tokenUpdate Successful.")
			if (state.currentError != null) {
				state.currentError = null
			}
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		}
	}
}

//	----- GET DEVICE DATA FROM THE CLOUD -----
def getDeviceData() {
	def currentDevices = ""
	def cmdBody = [method: "getDeviceList"]
	def getDevicesParams = [
		uri: "https://wap.tplinkcloud.com?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getDevicesParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			currentDevices = resp.data.result.deviceList
			if (state.currentError != null) {
				state.currentError = null
			}
			return currentDevices
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
		}
	}
}

//	----- SEND DEVICE COMMAND TO CLOUD FOR DH -----
def sendDeviceCmd(appServerUrl, deviceId, command) {
	def cmdResponse = ""
	def cmdBody = [
		method: "passthrough",
		params: [
			deviceId: deviceId,
			requestData: "${command}"
		]
	]
	def sendCmdParams = [
		uri: "${appServerUrl}/?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(sendCmdParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			def jsonSlurper = new groovy.json.JsonSlurper()
			cmdResponse = jsonSlurper.parseText(resp.data.result.responseData)
			if (state.errorCount != 0) {
				state.errorCount = 0
			}
			if (state.currentError != null) {
				state.currentError = null
				sendEvent(name: "currentError", value: null)
			log.debug "state.errorCount = ${state.errorCount} //	state.currentError = ${state.currentError}"
			}
//			log.debug "state.errorCount = ${state.errorCount} //	state.currentError = ${state.currentError}"
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			cmdResponse = "ERROR: ${resp.statusLine}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			cmdResponse = "ERROR: ${resp.data.msg}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		}
	}
	return cmdResponse
}


def uninstManagerApp() {
	try {
		//Revokes Smartthings endpoint token
		revokeAccessToken()
		//Revokes TP-Link Auth Token
		state.TpLinkToken = null
	} catch (ex) {
		log.error "uninstManagerApp Exception:", ex
	}
}

//	----- INSTALL, UPDATE, INITIALIZE, UNINSTALLED -----
def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule()
	runEvery5Minutes(checkError)
	schedule("0 30 2 ? * WED", getToken)
	if (selectedDevices) {
		if (userSelectedRemoveMode){
			removeDevices()
		} else {
			addDevices()
		}
	}
}

def uninstalled() {
	uninstManagerApp()
}

//	----- PERIODIC CLOUD MX TASKS -----
def checkError() {
	if (state.currentError == null || state.currentError == "none") {
		log.info "TP-Link Connect did not have any set errors."
		if (state.currentError == "none") {
			state.currentError = null
		}
		return
	}
	def errMsg = state.currentError.msg
	log.info "Attempting to solve error: ${errMsg}"
	state.errorCount = state.errorCount +1
	if (errMsg == "Token expired" && state.errorCount < 6) {
		sendEvent (name: "ErrHandling", value: "Handle comms error attempt ${state.errorCount}")
		getDevices()
		if (state.currentError == null) {
			log.info "getDevices successful. apiServerUrl updated and token is good."
			return
		}
		log.error "${errMsg} error while attempting getDevices. Will attempt getToken"
		getToken()
		if (state.currentError == null) {
			log.info "getToken successful. Token has been updated."
			getDevices()
			return
		}
	} else {
		log.error "checkError: No auto-correctable errors or exceeded Token request count."
	}
	log.error "checkError residual: ${state.currentError}"
}

//	----- CHILD CALLED TASKS -----
def removeChildDevice(alias, deviceNetworkId) {
	try {
		deleteChildDevice(it.deviceNetworkId)
		sendEvent(name: "DeviceDelete", value: "${alias} deleted")
	} catch (Exception e) {
		sendEvent(name: "DeviceDelete", value: "Failed to delete ${alias}")
	}
}

def gitBranch() { return betaMarker() ? "beta" : "master"  }
def getAppImg(imgName, on = null)	{ return (!appIcons || on) ? "https://raw.githubusercontent.com/${gitPath()}/images/$imgName" : "" }
def getWikiPageUrl() { return "https://github.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/wiki" }
def getIssuePageUrl() { return "https://github.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/issues" }
def appLabel() { return "TP-Link Device Manager" }
def appNamespace() { return "ramiran2" }
def gitRepo()		{ return "ramiran2/TP-Link-Kasa-Device-Manager-SmartThings"}
def gitPath()		{ return "${gitRepo()}/${gitBranch()}"}
def betaMarker() { return false }
def tokenInfoOnline()	{ return "Online and Ready to Control Devices" }
def tokenInfoOffline()	{ return "Offline, Please Fix to Restore Control on Devices" }
def pageSelectorText()	{ return "Please tap below to continue" }
def pageSelectorNullText()	{ return "Please select a option to continue" }
def appInfoDesc()	{
	def str = ""
	str += "${appLabel()}"
	str += "\n" + "• ${textVersion()}"
	str += "\n" + "• ${textModified()}"
	return str
}
def appAuthor() { return "Anthony Ramirez" }
def textVersion()	{ return "Version: ${appVersion()}" }
def textModified()	{ return "Updated: ${appVerDate()}" }
def textVerInfo()	{ return "${appVerInfo()}" }
def appVerInfo()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/changelog.txt", contentType: "text/plain; charset=UTF-8"], "changelog") }
def textLicense()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/license.txt", contentType: "text/plain; charset=UTF-8"], "license") }
def textDonateLink(){ return "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=S2CJBWCJEGVJA" }
def textCopyright()	{ return "Copyright© 2018 - Dave Gutheinz, Anthony Ramirez" }
def textDesc() { return "A Service Manager for the TP-Link Kasa Devices connecting through the TP-Link Servers to SmartThings." }