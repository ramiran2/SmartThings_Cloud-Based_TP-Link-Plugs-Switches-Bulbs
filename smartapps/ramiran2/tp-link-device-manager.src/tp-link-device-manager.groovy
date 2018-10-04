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

TP-Link Device Manager, 2018 Version 2

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
	'Cloud TP-Link Device SmartThings Integration'.

##### History #####
2018-10-04 Improved UI Elements with other small changes + Added a changelog page + Added a uninstall page + Added a about page + Improved logic
2018-10-01 Improved UI Elements with other small changes + Added a developer page
2018-09-28 Improved UI Elements with other small changes + Added a login page + Updated Driver Version Variables + Added a New Device Handler
2018-09-27 Improved UI Elements with other small changes + Updated for new Device Handlers + Add Support for the new Smart Thing Application
2018-08-28 Improved UI Elements with other small changes
2018-08-27 Improved UI Elements with other large changes
2018-08-22 Improved UI Elements and updated the app logo plus other small changes
2018-08-11 Updated for Added Support for update from a repo on smartthings website + Improved app name + Added app version
2018-01-31 Updated for new release of Device Handlers
*/

definition(
	name: "${appName()}",
	namespace: "${appNamespace()}",
	author: "${appAuthor()}",
	description: "${textDesc()}",
	category: "Convenience",
	iconUrl: "${getAppImg("kasa.png")}",
	iconX2Url: "${getAppImg("kasa.png")}",
	iconX3Url: "${getAppImg("kasa.png")}",
	singleInstance: true
)

{
	appSetting "clientId"
	appSetting "clientSecret"
	appSetting "devOpt"
}

def appVersion() { return "2.9.0" }
def appVerDate() { return "10-04-2018" }
def driverVersionsMin() {
	return [
		"colorbulbenergymonitor":["val":230, "desc":"2.3.0"],
		"colorbulb":["val":230, "desc":"2.3.0"],
		"dimmingswitch":["val":230, "desc":"2.3.0"],
		"energymonitorplug":["val":230, "desc":"2.3.0"],
		"plug":["val":230, "desc":"2.3.0"],
		"switch":["val":230, "desc":"2.3.0"],
		"softwhitebulbenergymonitor":["val":230, "desc":"2.3.0"],
		"softwhitebulb":["val":230, "desc":"2.3.0"],
		"tunablewhitebulbenergymonitor":["val":230, "desc":"2.3.0"],
		"tunablewhitebulb":["val":230, "desc":"2.3.0"]
	]
}

preferences {
	page(name: "oauthVerification")
	page(name: "startPage")
	page(name: "authPage")
	page(name: "mainPage")
	page(name: "selectDevices")
	page(name: "devMode")
	page(name: "aboutPage")
	page(name: "changeLogPage")
	page(name: "uninstallPage")
	page(name: "forceUninstallPage")
}

def setInitialStates() {
	if (!state.TpLinkToken) {state.TpLinkToken = null}
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
	if ("${userName}" =~ null || "${userPassword}" =~ null){
		settings.userSelectedOptionZero = "Initial Install"
		settings.userSelectedOptionOne = "Communication Error"
		settings.userSelectedOptionTwo = "Activate Account"
		settings.userSelectedOptionThree = "Update Token"
	} else {
		settings.userSelectedOptionZero = "Add/Remove Devices"
		settings.userSelectedOptionOne = "Communication Error"
		settings.userSelectedOptionTwo = "Update Account"
		settings.userSelectedOptionThree = "Update Token"
	}
	settings.userSelectedRemoveMode = "false"
	settings.selectedDevices = null
	settings.devModeLoaded = "false"
}

def oauthVerification() {
    if(!atomicState?.accessToken) { getAccessToken() }
	if(!atomicState?.accessToken) {
		return dynamicPage(name: "oauthVerification", title: "OAuth Verification Page", nextPage: "", install: false, uninstall: true) {
			section("") {
				paragraph appInfoDesc(), image: getAppImg("kasa.png")
			}
			section ("Application Information:") {
				def title = ""
                def desc = ""
				if(!atomicState?.accessToken) { title="OAuth Error"; desc = "OAuth is not Enabled for ${app?.label} application.  Please click remove and review the installation directions again"; }
				else { title="Unknown Error"; desc = "Application Status has not received any messages to display";	}
				log.warn "Status Message: $desc"
				paragraph title: "$title", "$desc", required: true, state: null
				href "devMode", title: "Developer Page", description: "Tap to view", image: getAppImg("developer.png")
			}
		}
	}
    else { return startPage() }
}

//This Page is used to load either parent or child app interface code
def startPage() {
	atomicState?.isParent = true
	setInitialStates()
	if ("${userName}" =~ null || "${userPassword}" =~ null){
		return authPage()
	} else {
		return mainPage()
	}
}

//	----- LOGIN PAGE -----
def authPage() {
	def authPageText = "If possible, open the IDE and select Live Logging. Then, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete. " + "Your current token:" + "${state.TpLinkToken}" +
		"\n\rAvailable actions:\n\r" +
		"Activate Account: Login into TP-Link Account and obtains token and adds devices.\n\r" +
		"Update Account: Updates the token and credentials."
	def driverVersionText = "TP-Link Kasa Drivers for SmartThings:" + "${driverVersionsMin}" + "\n" + "Note: Drivers from the old the original repository will not work with this version of the application."
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (state.TpLinkToken == null)
	return dynamicPage(
		name: "authPage",
		title: "Login Page",
		nextPage: "selectDevices",
		install: (atomicState?.isInstalled == "true" ? true : false),
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Driver Version:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
			paragraph title: "Information:", authPageText
			paragraph title: "Driver Version:", driverVersionText
		}
		section("Login Page:") {
			input(
				"userName", "email",
				title: "TP-Link Kasa Email Address",
				required: true,
				displayDuringSetup: true,
				image: getAppImg("email.png")
			)
			input(
				"userPassword", "password",
				title: "TP-Link Kasa Account Password",
				required: true,
				displayDuringSetup: true,
				image: getAppImg("password.png")
			)
		}
		section("Configuration Page:") {
			input(
				"userSelectedOptionTwo", "enum",
				title: "What do you want to do?",
				required: true,
				multiple: false,
				submitOnChange: true,
				options: ["Activate Account", "Update Account", "Developer Page"],
				image: getAppImg("settings.png")
			)
			input(
				"userSelectedDevMode", "bool",
				title: "Do you want to enable developer mode?",
				submitOnChange: true,
				image: getAppImg("developer.png")
			)
		}
	}
}

//	----- SETTINGS PAGE -----
def mainPage() {
	def mainPageText = "Your current token:" + "${state.TpLinkToken}" +
		"\n\rAvailable actions:\n\r" +
		"Initial Install: Login into TP-Link Account and obtains token and adds devices.\n\r" +
		"Add/Remove Devices: Only Add/Remove Devices.\n\r" +
		"Update Token: Updates the token.\n\r" +
		"Communication Error: Disables your capability to go the next page untill you fix the issue at hand."
	def errorRetuInfo = "We may not be unable to load Device Settings Page until you fix any error that show up in diagnostics.\n" + "Attempting to override this may end up in a blank screen."
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (state.currentError == null)
	def errorMsg = ""
	getDevices()
	def devices = state.devices
	def oldDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (isChild) {
			oldDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (state.currentError != null){
		errorMsg = "Error communicating with cloud:\n\r" + "${state.currentError}" +
			"\n\rPlease resolve the error and try again."
	}
	return dynamicPage(
		name: "mainPage",
		title: "Settings Page",
		nextPage: "selectDevices",
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
        section("Information and Diagnostics:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
			if (state.currentError =~ null || devModeLoaded){
				paragraph title: "Information:", mainPageText
			}
			if (state.currentError != null || devModeLoaded){
				paragraph title: "Communication Error:", errorMsg
			}
			if (state.currentError != null || devModeLoaded){
				paragraph title: "Loading Error:", errorRetuInfo
			}
		}
		section("Configuration Page:") {
			if (state.currentError != null && oldDevices != [:] && oldDevices != [:]) {
				input(
					"userSelectedOptionOne", "enum",
					title: "What do you want to do?",
					required: true,
					multiple: false,
					submitOnChange: true,
					options: ["Communication Error", "Reset Status", "Add/Remove Devices"],
					image: getAppImg("error.png")
				)
			} else {
				input(
					"userSelectedOptionZero", "enum",
					title: "What do you want to do?",
					required: true,
					multiple: false,
					submitOnChange: true,
					options: ["Initial Install", "Add/Remove Devices", "Update Token"],
					image: getAppImg("settings.png")
				)
			}
			input ("disAppIcons", "bool", title: "Disable App Icons?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("noicon.png"))
		}
		section("Help and Feedback:") {
			if (userSelectedDevMode){
				href "devMode", title: "Developer Page", description: "Tap to view", image: getAppImg("developer.png")
			}
			href url: getWikiPageUrl(), style:"embedded", required:false, title:"View the Projects Wiki", description:"Tap to open in browser", state: "complete", image: getAppImg("wiki.png")
			href url: getIssuePageUrl(), style:"embedded", required:false, title:"Report | View Issues", description:"Tap to open in browser", state: "complete", image: getAppImg("issue.png")
		}
		section("Uninstall:") {
			href "uninstallPage", title: "Uninstall Page", description: "Tap to view", image: getAppImg("uninstall.png")
		}
	}
}

//	----- SELECT DEVICES PAGE -----
def selectDevices() {
	if (userSelectedOptionZero =~ "Initial Install" || !devModeLoaded) {
		return authPage()
	}
	if (userSelectedOptionOne =~ "Communication Error" || !devModeLoaded) {
		return mainPage()
	}
	if (userSelectedOptionOne =~ "Reset Status" || !devModeLoaded) {
		setInitialStates()
		return mainPage()
	}
	if (userSelectedOptionTwo =~ "Developer Page" || !devModeLoaded) {
		return devMode()
	}
	if (userSelectedOptionZero =~ "Update Token" || userSelectedOptionTwo =~ "Activate Account" || userSelectedOptionTwo =~ "Update Account") {
		getToken()
	}
	getDevices()
	def devices = state.devices
	def errorMsg = ""
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
		errorMsg = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+
		"that all devices are in 'Local Control Only'. Correct them then " +
		"rerun the application."
	}
	if (newDevices == [:]) {
		errorMsg = "No new devices to add. Are you sure they are in Remote " +
		"Control Mode?"
	}
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (errorMsg == "")
	def TPLinkDevicesMsg = "TP-Link Token is ${state.TpLinkToken}\n\r" +
		"Devices that have not been previously installed and are not in 'Local " +
		"WiFi control only' will appear below. Tap below to see the list of " +
		"TP-Link Kasa Devices available select the ones you want to connect to " +
		"SmartThings.\n\r" + "Press Done when you have selected the devices you " +
		"wish to add, thenpress Done again to install the devices. Press < " +
		"to return to the previous page."
	return dynamicPage(
		name: "selectDevices",
		title: "Device Settings Page",
		install: true,
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
				if (errorMsg =~ "" || devModeLoaded){
					paragraph title: "Information:", TPLinkDevicesMsg
				}
				if (userSelectedOptionZero != "Update Token" && userSelectedOptionTwo != "Update Account" && errorMsg != "" || devModeLoaded) {
					paragraph title: "Device Error:", errorMsg
				}
		}
		if (userSelectedOptionZero =~ "Update Token" || userSelectedOptionTwo =~ "Update Account" || devModeLoaded) {
			section("Account Configuration Page:") {
				input(
					"userSelectedOptionThree", "enum",
					title: "What do you want to do?",
					required: true,
					multiple: false,
					submitOnChange: true,
					options: ["Update Token"],
					image: getAppImg("token.png")
					)
				}
			}
		if (userSelectedOptionZero =~ "Add/Remove Devices" || userSelectedOptionTwo =~ "Activate Account" || userSelectedOptionOne =~ "Add/Remove Devices" || devModeLoaded) {
			section("Device Configuration Page:") {
				if (userSelectedRemoveMode) {
					input(
						"selectedDevices", "enum",
						required: true,
						multiple: true,
						submitOnChange: true,
						title: "Select Devices (${oldDevices.size() ?: 0} found)",
						options: oldDevices,
						image: getAppImg("devices.png")
					)
				} else {
					input(
						"selectedDevices", "enum",
						required: true,
						multiple: true,
						submitOnChange: true,
						title: "Select Devices (${newDevices.size() ?: 0} found)",
						options: newDevices,
						image: getAppImg("devices.png")
					)
				}
				input(
					"userSelectedRemoveMode", "bool",
					title: "Do you want to enable device removal mode?",
					submitOnChange: true,
					image: getAppImg("deviceremover.png")
				)
			}
		}
	}
}

//	----- DEVELOPER MODE PAGE -----
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
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (state.currentError == null)
	return dynamicPage(
		name: "devMode",
		title: "Developer Page",
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Application Information:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
			paragraph title: "Error Count:", "${state.errorCount}"
			paragraph title: "Current Error:", "${state.currentError}"
			paragraph title: "TP-Link Token:", "${state.TpLinkToken}"
			paragraph title: "Hub:", "${hub}"
			paragraph title: "Hub ID:", "${hubId}"
			paragraph title: "Error Messages:", "${errMsg}"
			paragraph title: "Username:", "${userName}"
			paragraph title: "Password:", "${userPassword}"
			paragraph title: "Managed Devices:", "${oldDevices}"
			paragraph title: "New Devices:", "${newDevices}"
		}
		section("Page Selector:") {
			href "authPage", title: "Login Page", description: "Tap to view", image: getAppImg("authpage.png")
			href "mainPage", title: "Settings Page", description: "Tap to view", image: getAppImg("mainpage.png")
			href "selectDevices", title: "Device Settings Page", description: "Tap to view", image: getAppImg("selectdevices.png")
			href "aboutPage", title: "About Page", description: "Tap to view", image: getAppImg("aboutpage.png")
			href "changeLogPage", title: "Changelog Page", description: "Tap to view", image: getAppImg("changelogpage.png")
			href "uninstallPage", title: "Uninstall Page", description: "Tap to view", image: getAppImg("uninstallpage.png")
		}
		section("Configuration:") {
			input(
				"devModeLoaded", "bool",
				title: "Do you want to enable developer mode?",
				submitOnChange: true,
				image: getAppImg("developer.png")
			)
		}
		section("Security:") {
			paragraph title:"What does resetting do?", "If you share a url with someone and want to remove their access you can reset your token and this will invalidate any URL you shared and create a new one for you."
			input (name: "resetSTAccessToken", type: "bool", title: "Reset SmartThings Access Token?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("reset.png"))
			resetSTAccessToken(settings?.resetSTAccessToken == true)
		}
	}
}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: "About Page", install: false, uninstall: false) {
		section("About this App:") {
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

def changeLogPage () {
	dynamicPage(name: "changeLogPage", title: "Changelog Page", install: false) {
		section() {
			paragraph title: "What's New in this Release...", "", state: "complete", image: getAppImg("new.png")
			paragraph appVerInfo()
		}
	}
}

def uninstallPage() {
	dynamicPage(name: "uninstallPage", title: "Uninstall", install: false, uninstall: true) {
		section() {
			paragraph "This will uninstall the App, All Automation Apps and Child Devices.\n\nPlease make sure that any devices created by this app are removed from any routines/rules/smartapps before tapping Remove."
		}
		section("Did You Get an Error?") {
			href "forceUninstallPage", title: "Perform Some Cleanup Steps", description: "Tap to force uninstall", image: getAppImg("forceuninstall.png")
		}
		remove("Remove ${appName()} and Devices!", "WARNING!!!", "Last Chance to Stop!\nThis action is not reversible\n\nThis App, All Devices, and Automations will be removed")
	}
}

def forceUninstallPage() {
	dynamicPage(name: "forceUninstallPage", title: "Uninstall Pre Cleanup", install: false, uninstall: true) {
		section() {
			getChildApps()?.each {
				deleteChildApp(it)
				paragraph "Removed Child App: ${it?.label}"
			}
		}
		remove("Try Removing Again!!!", "WARNING!!!", "Last Chance to Stop!\nThis action is not reversible\n\nThis App, All Devices, and Automations will be removed")
	}
}

//This code really does nothing at the moment but return the dynamic url of the app's endpoints
def getEndpointUrl() {
	def params = [
		uri: "https://graph.api.smartthings.com/api/smartapps/endpoints",
		query: ["access_token": atomicState?.accessToken],
		contentType: 'application/json'
	]
	try {
		httpGet(params) { resp ->
			log.trace "EndPoint URL: ${resp?.data?.uri}"
			return resp?.data?.uri
		}
	} catch (ex) {
		log.error "getEndpointUrl Exception:", ex
	}
}

def getApiURL() {
	return apiServerUrl("/api/token/${atomicState?.accessToken}/smartapps/installations/${app.id}") ?: null
}


void settingUpdate(name, value, type=null) {
	log.trace "settingUpdate($name, $value, $type)..."
	if(name) {
		if(value =~ "" || value =~ null || value == []) {
			settingRemove(name)
			return
		}
	}
	if(name && type) {
		app?.updateSetting("$name", [type: "$type", value: value])
	}
	else if (name && type =~ null){ app?.updateSetting(name.toString(), value) }
}

void settingRemove(name) {
	log.trace "settingRemove($name)..."
	if(name) { app?.deleteSetting("$name") }
}

void resetSTAccessToken(reset) {
	if(reset != true) { return }
	log.info "Resetting SmartApp Access Token...."
	revokeAccessToken()
	atomicState?.accessToken = null
	if(getAccessToken()) {
		log.info "Reset SmartApp Access Token... Successful"
		settingUpdate("resetSTAccessToken", "false", "bool")
	}
}

def getAccessToken() {
	try {
		if(!atomicState?.accessToken) { atomicState?.accessToken = createAccessToken() }
		else { return true }
	}
	catch (ex) {
		def msg = "Error: OAuth is not Enabled for TP-Link Device Manager! Please click remove and Enable OAuth under the SmartApp App Settings in the IDE"
		sendPush(msg)
		log.error "getAccessToken Exception", ex
		return false
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
	childDevices.each {
		try{
			def delete = app.getChildDevices(true).findAll { selectedDevices?.toString()?.contains(it?.deviceNetworkId) }
			if(delete?.size() > 0) {
				updTimestampMap("lastAnalyticUpdDt", null)
				log.debug "Removing ${delete.size()} devices: ${delete}"
				delete.each { deleteChildDevice(it.deviceNetworkId, true) }
			}
		}
		catch (e) {
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
	selectedDevices.each { dni ->
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
	if (state.currentError == null) {
		log.info "TP-Link Connect did not have any set errors."
		return
	}
	def errMsg = state.currentError.msg
	log.info "Attempting to solve error: ${errMsg}"
	state.errorCount = state.errorCount +1
	if (errMsg =~ "Token expired" && state.errorCount < 6) {
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
def getAppImg(imgName, on = null)	{ return (!disAppIcons || on) ? "https://raw.githubusercontent.com/${gitPath()}/images/$imgName" : "" }
def getWikiPageUrl() { return "https://github.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/wiki" }
def getIssuePageUrl() { return "https://github.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/issues" }
def getAppEndpointUrl(subPath) { return "${apiServerUrl("/api/smartapps/installations/${app.id}${subPath ? "/${subPath}" : ""}?access_token=${atomicState.accessToken}")}" }
def appName() { return "${"${appLabel()}"}${appDevName()}" }
def appLabel() { return "TP-Link Device Manager" }
def appDevName() { return appDevType() ? " (Dev)" : "" }
def appDevType() { return false }
def appNamespace() { return "ramiran2" }
def gitRepo()		{ return "ramiran2/TP-Link-Kasa-Device-Manager-SmartThings"}
def gitPath()		{ return "${gitRepo()}/${gitBranch()}"}
def developerVer()	{ return false }
def betaMarker() { return false }
def appInfoDesc()	{
	def str = ""
	str += "TP-Link Device Manager"
	str += "\n" + "• Version: ${appVersion()}"
	str += "\n" + "• Updated: ${appVerDate()}"
	return str
}
def appAuthor() { return "Anthony Ramirez" }
def getServerUrl()			{ return "https://graph.api.smartthings.com" }
def getShardUrl()			{ return getApiServerUrl() }
def getCallbackUrl()		{ return "https://graph.api.smartthings.com/oauth/callback" }
def getBuildRedirectUrl() { return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState?.accessToken}&apiServerUrl=${shardUrl}" }
def textVersion()	{ return "Version: ${appVersion()}" }
def textModified()	{ return "Updated: ${appVerDate()}" }
def textVerInfo()	{ return "${appVerInfo()}" }
def appVerInfo()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/changelog.txt", contentType: "text/plain; charset=UTF-8"], "changelog") }
def textLicense()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/license.txt", contentType: "text/plain; charset=UTF-8"], "license") }
def textDonateLink(){ return "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=S2CJBWCJEGVJA" }
def stIdeLink()		{ return "https://graph.api.smartthings.com" }
def textCopyright()	{ return "Copyright© 2018 - Dave Gutheinz, Anthony Ramirez" }
def textDesc() { return "A Service Manager for the TP-Link Kasa Devices connecting through the TP-Link Servers to SmartThings." }