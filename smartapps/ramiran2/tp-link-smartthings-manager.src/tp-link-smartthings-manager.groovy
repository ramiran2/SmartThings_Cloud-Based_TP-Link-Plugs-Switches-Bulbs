/*
TP-Link SmartThings Manager and TP-Link Cloud Connect, 2018 Version 4

	Copyright 2018 Dave Gutheinz, Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

Discalimer: This Service Manager and the associated Device
Handlers are in no way sanctioned or supported by TP-Link. All
development is based upon open-source data on the TP-Link Kasa Devices;
primarily various users on GitHub.com.*/

//	======== Developer Namespace =============================================================
//	def appNamespace()	{ return "davegut" }												//	Davegut: Developer Namespace
	def appNamespace()	{ return "ramiran2" }												//	Ramiran2: Developer Namespace
//	======== Repository Name =================================================================
//	def gitName()	{ return "SmartThings_Cloud-Based_TP-Link-Plugs-Switches-Bulbs" }		//	Davegut: Repository Name
	def gitName()	{ return "TP-Link-SmartThings" }										//	Ramiran2: Repository Name
//	======== Application Name ================================================================
//	def appLabel()	{ return "TP-Link Cloud Connect" }										//	Davegut: Application Name
	def appLabel()	{ return "TP-Link SmartThings Manager" }								//	Ramiran2: Application Name
//	======== Application Information =========================================================
	def appVersion()	{ return "4.2.0" }													//	Application Version
	def appVerDate()	{ return "10-27-2018" }												//	Application Date
	def appAuthor()	{ return "Dave Gutheinz, Anthony Ramirez" }								//	Application Author
//	==========================================================================================

definition (name: "${appLabel()}", namespace: "${appNamespace()}", author: "${appAuthor()}", description: "${textDesc()}", category: "Convenience", iconUrl: "${getAppImg("logo.png", on)}", iconX2Url: "${getAppImg("logo.png", on)}", iconX3Url: "${getAppImg("logo.png", on)}", singleInstance: true)

preferences {
	page(name: "startPage")
	page(name: "welcomePage")
	page(name: "kasaUserSelectionAuthenticationPage")
	page(name: "kasaComputerSelectionAuthenticationPage")
	page(name: "kasaInstallationAuthenticationPage")
	page(name: "hubBridgeDiscoveryPage")
	page(name: "hubInstallationBridgeDiscoveryPage")
	page(name: "kasaUserSelectionPage")
	page(name: "kasaComputerSelectionPage")
	page(name: "userAddDevicesPage")
	page(name: "userRemoveDevicesPage")
	page(name: "userApplicationPreferencesPage")
	page(name: "userDevicePreferencesPage")
	page(name: "kasaUserAuthenticationPreferencesPage")
	page(name: "kasaUserSelectionTokenPage")
	page(name: "developerPage")
	page(name: "developerTestingPage")
	page(name: "hiddenPage")
	page(name: "aboutPage")
	page(name: "changeLogPage")
	page(name: "uninstallPage")
}

def startPage()	{
	settingUpdate("userSelectedManagerMode", "false", "bool")	//	Cloud (false) or Hub (true)
	if (!userSelectedManagerMode) {
		setInitialStatesKasa()
		if (userSelectedAssistant) {
			setRecommendedOptions()
		}
		if ("${userName}" =~ null || "${userPassword}" =~ null) {
			kasaInstallationAuthenticationPage()
		} else {
			welcomePage()
		}
	} else {
		setInitialStatesHub()
		if ("${userName}" =~ null || "${userPassword}" =~ null) {
			hubInstallationBridgeDiscoveryPage()
		} else {
			welcomePage()
		}
	}
}

//	----- WELCOME PAGE -----
def welcomePage()	{
	def strLatestDriverVersion = textDriverVersion()
	def welcomePageText = "None"
	def strPageName = "None"
	if (!userSelectedManagerMode) {
		strPageName = "Dashboard - Cloud Controller"
		welcomePageText = "Welcome to the new SmartThings application for TP-Link Kasa Devices. If you want to check for updates you can now do that in the changelog page."
	} else {
		strPageName = "Dashboard - Hub Controller"
		welcomePageText = "This SA installs and manages TP-Link bulbs, plugs, and switches DHs as well as " + "the associated Bridge. You will encounter other pages in the following order. " + "NEXT goes to Bridge Discovery (no bridge detected) or Device Discovery. " + "Bridge Discovery - Does the initial discover and user selection of the Bridge " + "between SmartThings and the TP-Link Devices. Entered only on initial " + " installation. DONE installs the selected bridge and exits the program." + "Device Discovery - Discovers and installs the TP-Link Devices. Also used for " + "updating IP addresses and for discovering added devices. DONE installs the " + "selected devices then exits the program."
	}
	def driverVersionText = "Current Driver Version: ${strLatestDriverVersion}"
	return dynamicPage (name: "welcomePage", title: "${strPageName}", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (!userSelectedManagerMode) {
				if (state.TpLinkToken != null) {
					paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
				} else {
					paragraph tokenInfoOffline(), image: getAppImg("error.png")
				}
			}
			paragraph title: "Information: ", welcomePageText, image: getAppImg("information.png")
			paragraph title: "Driver Version: ", driverVersionText, image: getAppImg("devices.png")
		}
		if (!userSelectedLauncher) {
			section("Page Selector:") {
				if (!userSelectedManagerMode) {
					if (state.currentError != null) {
						paragraph pageSelectorErrorText(), image: getAppImg("error.png")
					} else {
						paragraph pageSelectorText(), image: getAppImg("pageselected.png")
					}
					if ("${userName}" =~ null || "${userPassword}" =~ null) {
						href "kasaUserSelectionAuthenticationPage", title: "Login Page", description: "Tap to continue", image: getAppImg("userselectionauthenticationpage.png")
					} else {
						href "kasaUserSelectionPage", title: "Launcher Page", description: "Tap to continue", image: getAppImg("userselectionpage.png")
					}
				} else {
					paragraph pageSelectorText(), image: getAppImg("pageselected.png")
					href "hubBridgeDiscoveryPage", title: "Bridge Discovery Page", description: "Tap to continue", image: getAppImg("samsunghub.png")
				}
			}
		}
		if (userSelectedQuickControl) {
			section("Device Manager:") {
				if (!userSelectedManagerMode) {
					href "userAddDevicesPage", title: "Device Installer Page", description: "Tap to view", image: getAppImg("adddevicespage.png")
					href "userRemoveDevicesPage", title: "Device Uninstaller Page", description: "Tap to view", image: getAppImg("removedevicespage.png")
				} else {
					href "userAddDevicesPage", title: "Device Installer Page", description: "Tap to view", image: getAppImg("adddevicespage.png")
					href "userRemoveDevicesPage", title: "Device Uninstaller Page", description: "Tap to view", image: getAppImg("removedevicespage.png")
				}
			}
		}
		section("Settings:") {
			if (userSelectedQuickControl) {
				if (!userSelectedManagerMode) {
					href "kasaUserAuthenticationPreferencesPage", title: "Login Settings Page", description: "Tap to view", image: getAppImg("userauthenticationpreferencespage.png")
				} else {
				
				}
				href "userDevicePreferencesPage", title: "Device Preferences Page", description: "Tap to view", image: getAppImg("userdevicepreferencespage.png")
			}
			href "userApplicationPreferencesPage", title: "Application Settings Page", description: "Tap to view", image: getAppImg("userapplicationpreferencespage.png")
		}
		section("Uninstall:") {
			href "uninstallPage", title: "Uninstall Page", description: "Tap to view", image: getAppImg("uninstallpage.png")
		}
		if (userSelectedDeveloper) {
			section("Developer:") {
				href "developerPage", title: "Developer Page", description: "Tap to view", image: getAppImg("developerpage.png")
			}
		}
		section("Help and Feedback:") {
			href url: getWikiPageUrl(), style: "${strBrowserMode()}", title: "View the Projects Wiki", description: "Tap to open in browser", state: "complete", image: getAppImg("help.png")
			href url: getIssuePageUrl(), style: "${strBrowserMode()}", title: "Report | View Issues", description: "Tap to open in browser", state: "complete", image: getAppImg("issue.png")
		}
		section("Changelog and About:") {
			href "changeLogPage", title: "Changelog Page", description: "Tap to view", image: getAppImg("changelogpage.png")
			href "aboutPage", title: "About Page", description: "Tap to view", image: getAppImg("aboutpage.png")
		}
		section("${textCopyright()}")
	}
}

//	----- USER SELECTION AUTHENTICATION PAGE -----
def kasaUserSelectionAuthenticationPage()	{
	def kasaUserSelectionAuthenticationPageText = "If possible, open the IDE and select Live Logging. Then, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete. " + "\nAvailable actions: \n" +
		"Activate Account: You will be required to login into TP-Link Kasa Account and you will be required to adds devices to SmartThings Account. \n" +
		"Update Account: You will be required to update your credentials to login into your TP-Link Kasa Account. \n" +
		"Delete Account: Deletes your credentials to login into your TP-Link Kasa Account."
	return dynamicPage (name: "kasaUserSelectionAuthenticationPage", title: "Login Page", nextPage: "kasaComputerSelectionAuthenticationPage", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (state.TpLinkToken != null) {
				paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
			} else {
				paragraph tokenInfoOffline(), image: getAppImg("error.png")
			}
			paragraph title: "Information: ", kasaUserSelectionAuthenticationPageText, image: getAppImg("information.png")
		}
		section("Account Configuration:") {
			input ("userName", "email", title: "TP-Link Kasa Email Address", required: true, submitOnChange: false, image: getAppImg("email.png"))
			input ("userPassword", "password", title: "TP-Link Kasa Account Password", required: true, submitOnChange: false, image: getAppImg("password.png"))
		}
		section("User Configuration:") {
			input ("userSelectedOptionTwo", "enum", title: "What do you want to do?", required: true, multiple: false, submitOnChange: true, metadata: [values: ["Update Account", "Activate Account", "Delete Account"]], image: getAppImg("userinput.png"))
		}
		section("Page Selector:") {
			if (userSelectedOptionTwo != null) {
				if (state.currentError != null) {
					paragraph pageSelectorErrorText(), image: getAppImg("error.png")
				} else {
					paragraph pageSelectorText(), image: getAppImg("pageselected.png")
				}
			} else {
				paragraph pageSelectorNullText(), image: getAppImg("pickapage.png")
			}
			if (userSelectedOptionTwo =~ "Activate Account") {
				getToken()
				href "userAddDevicesPage", title: "Device Installer Page", description: "Tap to continue", image: getAppImg("adddevicespage.png")
			}
			if (userSelectedOptionTwo =~ "Update Account") {
				href "kasaUserSelectionTokenPage", title: "Token Manager Page", description: "Tap to continue", image: getAppImg("userselectiontokenpage.png")
			}
			if (userSelectedOptionTwo =~ "Delete Account") {
				settingRemove("userName")
				settingRemove("userPassword")
				state.TpLinkToken = null
				href "welcomePage", title: "Dashboard", description: "Tap to view", image: getAppImg("welcomepage.png")
			}
		}
		section("${textCopyright()}")
	}
}

//	----- COMPUTER SELECTION AUTHENTICATION PAGE -----
def kasaComputerSelectionAuthenticationPage()	{
	switch (userSelectedOptionTwo) {
		case "Update Account" :
			return kasaUserSelectionTokenPage()
			break
		case "Activate Account" :
			return userAddDevicesPage()
			break
		case "Delete Account" :
			return welcomePage()
			break
		default:
			if ("${userName}" =~ null || "${userPassword}" =~ null) {
				return welcomePage()
			} else {
				return kasaUserSelectionPage()
			}
	}
}

//	----- USER AUTHENTICATION PREFERENCES PAGE -----
def kasaInstallationAuthenticationPage()	{
	def kasaInstallationAuthenticationPageText = "If possible, open the IDE and select Live Logging. Then, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete."
	return dynamicPage (name: "kasaInstallationAuthenticationPage", title: "Initial Login Page - Cloud Controller", nextPage: "kasaInstallationAddDevicesPage", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			paragraph title: "Information: ", kasaUserAuthenticationPreferencesPageText, image: getAppImg("information.png")
		}
		section("Account Configuration:") {
			input ("userName", "email", title: "TP-Link Kasa Email Address", required: true, submitOnChange: false, image: getAppImg("email.png"))
			input ("userPassword", "password", title: "TP-Link Kasa Account Password", required: true, submitOnChange: false, image: getAppImg("password.png"))
		}
		section("${textCopyright()}")
	}
}

// ----- Page: Hub (Bridge) Discovery ------------------------------
def hubBridgeDiscoveryPage()	{
	checkForBridgeHub()
	ssdpSubscribe()
	ssdpDiscover()
	verifyBridges()
	def hubBridgeDiscoveryPageText = "Please wait while we discover your TP-Link Bridge. Discovery can take "+ "several minutes\n" + "If no bridges are discovered after several minutes, press DONE. This " + "will install the app. Then re-run the application."
	return dynamicPage(name: "hubBridgeDiscoveryPage", title: "Bridge Discovery", nextPage: "", refreshInterval: 5, install: false, uninstall: false){
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			paragraph title: "Information: ", hubBridgeDiscoveryPageText, image: getAppImg("information.png")
		}
		section("Bridge Controller:") {
			input ("userSelectedBridgeAddHub", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Bridges to Add (${state.strfoundbridge.size() ?: 0} found)", metadata: [values: state.strfoundbridge], image: getAppImg("adddevices.png"))
		}
	}
}

//	----- USER SELECTION PAGE -----
def kasaUserSelectionPage()	{
	def kasaUserSelectionPageText = "Available actions: \n" +
		"Add Devices: You will be able to add devices to your SmartThings Hub so you can control them from the SmartThings application. \n" +
		"Remove Devices: You will be able to remove any device from your SmartThings Hub that is controlled by this application. \n" +
		"Update Token: You will be able to request for a new token or delete your current token from the application. \n" +
		"Initial Installation: You will be asked to login into TP-Link Account and you may be asked to adds devices if you have not done so already."
	def errorMsgCom = "None"
	if (state.currentError != null) {
		errorMsgCom = "Error communicating with cloud: \n" + "${state.currentError} " +
			"\nPlease resolve the error and try again."
	}
	return dynamicPage (name: "kasaUserSelectionPage", title: "Launcher Page", nextPage: "kasaComputerSelectionPage", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (state.TpLinkToken != null) {
				paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
			} else {
				paragraph tokenInfoOffline(), image: getAppImg("error.png")
			}
			paragraph title: "Information: ", kasaUserSelectionPageText, image: getAppImg("information.png")
			paragraph title: "Communication Error: ", errorMsgCom, image: getAppImg("error.png")
		}
		section("User Configuration:") {
			input ("userSelectedOptionOne", "enum", title: "What do you want to do?", required: true, multiple: false, submitOnChange: true, metadata: [values: ["Add Devices", "Remove Devices", "Update Token", "Initial Installation"]], image: getAppImg("userinput.png"))
		}
		section("Page Selector:") {
			if (userSelectedOptionOne != null) {
				if (state.currentError != null) {
					paragraph pageSelectorErrorText(), image: getAppImg("error.png")
				} else {
					paragraph pageSelectorText(), image: getAppImg("pageselected.png")
				}
			} else {
				paragraph pageSelectorNullText(), image: getAppImg("pickapage.png")
			}
			if (userSelectedOptionOne =~ "Initial Installation") {
				href "kasaUserSelectionAuthenticationPage", title: "Login Page", description: "Tap to continue", image: getAppImg("userselectionauthenticationpage.png")
			}
			if (userSelectedOptionOne =~ "Add Devices") {
				href "userAddDevicesPage", title: "Device Installer Page", description: "Tap to continue", image: getAppImg("adddevicespage.png")
			}
			if (userSelectedOptionOne =~ "Remove Devices") {
				href "userRemoveDevicesPage", title: "Device Uninstaller Page", description: "Tap to continue", image: getAppImg("removedevicespage.png")
			}
			if (userSelectedOptionOne =~ "Update Token") {
				href "kasaUserSelectionTokenPage", title: "Token Manager Page", description: "Tap to continue", image: getAppImg("userselectiontokenpage.png")
			}
		}
		section("${textCopyright()}")
	}
}

//	----- COMPUTER SELECTION PAGE -----
def kasaComputerSelectionPage()	{
  switch (userSelectedOptionOne) {
		case "Initial Installation" :
			return kasaUserSelectionAuthenticationPage()
			break
		case "Add Devices" :
			return userAddDevicesPage()
			break
		case "Remove Devices" :
			return userRemoveDevicesPage()
			break
		case "Update Token" :
			return kasaUserSelectionTokenPage()
			break
		default:
			return welcomePage()
	}
}

//	----- ADD DEVICES PAGE -----
def userAddDevicesPage()	{
	if (!userSelectedManagerMode) {
		checkForDevicesKasa()
	} else {
		checkForDevicesHub()
		discoverDevices()
	}
	def errorMsgDev = "None"
	if (!userSelectedManagerMode) {
		if (state.kasadevices == [:]) {
			errorMsgDev = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+ "that all devices are in 'Local Control Only'. Correct them then " + "rerun the application."
		}
		if (state.newkasadevices == [:]) {
			errorMsgDev = "No new devices to add. Are you sure they are in Remote " + "Control Mode?"
		}
	} else {
		if (state.kasadevices == [:]) {
			errorMsgDev = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+ "that all devices are in 'Local Control Only'. Correct them then " + "rerun the application."
		}
		if (state.newkasadevices == [:]) {
			errorMsgDev = "No new devices to add. Are you sure they are in Remote " + "Control Mode?"
		}
	}
	if (!userSelectedManagerMode) {
		def userAddDevicesPageText = "Devices that have not been previously installed and are not in 'Local " + "WiFi control only' will appear below. Tap below to see the list of " + "TP-Link Kasa Devices available select the ones you want to connect to " + "SmartThings.\n" + "Press Done when you have selected the devices you " + "wish to add, then press Save to add the devices to your SmartThings account."
	} else {
		def userAddDevicesPageText = "Discovering TP-Link Devices on your LAN. This may take several minutes. " + "You can follow the process by looking at the count of devices below. " + "When you are ready to select devices to install, touch the area below."
	}
	return dynamicPage (name: "userAddDevicesPage", title: "Device Installer Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (!userSelectedManagerMode) {
				if (state.TpLinkToken != null) {
					paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
				} else {
					paragraph tokenInfoOffline(), image: getAppImg("error.png")
				}
			}
			paragraph title: "Information: ", userAddDevicesPageText, image: getAppImg("information.png")
			paragraph title: "Device Error: ", errorMsgDev, image: getAppImg("error.png")
		}
		section("Device Controller:") {
			if (!userSelectedManagerMode) {
				input ("userSelectedDevicesAddKasa", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices to Add (${state.newkasadevices.size() ?: 0} found)", metadata: [values: state.newkasadevices], image: getAppImg("adddevices.png"))
			} else {
				input ("userSelectedDevicesAddHub", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices to Add (${state.newhubdevices.size() ?: 0} found)", metadata: [values: state.newhubdevices], image: getAppImg("adddevices.png"))
			}
		}
		section("${textCopyright()}")
	}
}

//	----- REMOVE DEVICES PAGE -----
def userRemoveDevicesPage()	{
	def errorMsgDev = "None"
	if (!userSelectedManagerMode) {
		checkForDevicesKasa()
	} else {
		checkForDevicesHub()
	}
	if (state.kasadevices == [:]) {
		errorMsgDev = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+
		"that all devices are in 'Local Control Only'. Correct them then " + "rerun the application."
	}
	if (state.oldkasadevices == [:]) {
		errorMsgDev = "There are no devices to remove from the SmartThings app at this time."
	}
	def userRemoveDevicesPageText = "Devices that have been installed " +
		"will appear below. Tap below to see the list of " +
		"TP-Link Kasa Devices available select the ones you want to connect to " +
		"SmartThings.\n" + "Press Done when you have selected the devices you " +
		"wish to remove, then Press Save to remove the devices to your SmartThings account."
	return dynamicPage (name: "userRemoveDevicesPage", title: "Device Uninstaller Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (!userSelectedManagerMode) {
				if (state.TpLinkToken != null) {
					paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
				} else {
					paragraph tokenInfoOffline(), image: getAppImg("error.png")
				}
			}
			paragraph title: "Information: ", userRemoveDevicesPageText, image: getAppImg("information.png")
			paragraph title: "Device Error: ", errorMsgDev, image: getAppImg("error.png")
		}
		section("Device Controller:") {
			if (!userSelectedManagerMode) {
				input ("userSelectedDevicesRemoveKasa", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices to Remove (${state.oldkasadevices.size() ?: 0} found)", metadata: [values: state.oldkasadevices], image: getAppImg("removedevices.png"))
			} else {
				input ("userSelectedDevicesRemoveHub", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices to Remove (${state.oldhubdevices.size() ?: 0} found)", metadata: [values: state.oldhubdevices], image: getAppImg("removedevices.png"))
			}
		}
		section("${textCopyright()}")
	}
}

//	----- USER APPLICATION PREFERENCES PAGE -----
def userApplicationPreferencesPage()	{
	def hiddenRecordInput = 0
	def hiddenDeveloperInput = 0
	if (userSelectedDeveloper) {
		hiddenDeveloperInput = 1
	} else {
		hiddenDeveloperInput = 0
	}
	if ("${restrictedRecordPasswordPrompt}" =~ null) {
		hiddenRecordInput = 0
	} else {
		hiddenRecordInput = 1
	}
	def userApplicationPreferencesPageText = "Welcome to the application settings page. \n" +
		"Recommended options: Will allow your device to pick a option for you that you are likely to pick."
	return dynamicPage (name: "userApplicationPreferencesPage", title: "Application Settings Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (!userSelectedManagerMode) {
				if (state.TpLinkToken != null) {
					paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
				} else {
					paragraph tokenInfoOffline(), image: getAppImg("error.png")
				}
			}
			paragraph title: "Information: ", userApplicationPreferencesPageText, image: getAppImg("information.png")
		}
		section("Application Configuration:") {
			input ("userSelectedNotification", "bool", title: "Do you want to enable notification?", submitOnChange: false, image: getAppImg("notification.png"))
			input ("userSelectedAppIcons", "bool", title: "Do you want to disable application icons?", submitOnChange: false, image: getAppImg("noicon.png"))
			input ("userSelectedManagerMode", "bool", title: "Do you want to switch to hub controller mode?", submitOnChange: false, image: getAppImg("samsunghub.png"))
			input ("userSelectedLauncher", "bool", title: "Do you want to disable the launcher page?", submitOnChange: false, image: getAppImg("launcher.png"))
			if (!userSelectedLauncher) {
				input ("userSelectedAssistant", "bool", title: "Do you want to enable recommended options?", submitOnChange: false, image: getAppImg("ease.png"))
			}
			input ("userSelectedBrowserMode", "bool", title: "Do you want to open all external links within the SmartThings app?", submitOnChange: false, image: getAppImg("browsermode.png"))
			if (!userSelectedManagerMode) {
				input ("userSelectedReload", "bool", title: "Do you want to refresh your current state?", submitOnChange: true, image: getAppImg("sync.png"))
			}
			if (userSelectedAppIcons && userSelectedBrowserMode && userSelectedNotification || hiddenDeveloperInput == 1) {
				hiddenDeveloperInput = 1
				input ("userSelectedDeveloper", "bool", title: "Do you want to enable developer mode?", submitOnChange: true, image: getAppImg("developer.png"))
			}
			if (userSelectedDeveloper) {
				input ("userSelectedQuickControl", "bool", title: "Do you want to enable post install features?", submitOnChange: false, image: getAppImg("quickcontrol.png"))
				input ("userSelectedTestingPage", "bool", title: "Do you want to enable developer testing mode?", submitOnChange: true, image: getAppImg("developertesting.png"))
				input ("userSelectedDriverNamespace", "bool", title: "Do you want to switch the device handlers namespace?", submitOnChange: false, image: getAppImg("drivernamespace.png"))
			}
			if (userSelectedTestingPage && !userSelectedNotification  || hiddenRecordInput == 1) {
				hiddenRecordInput = 1
				input ("restrictedRecordPasswordPrompt", type: "password", title: "This is a restricted record, Please input your password", description: "Hint: xKillerMaverick", required: false, submitOnChange: false, image: getAppImg("passwordverification.png"))
			}
			if (userSelectedReload) {
				checkError()
				setInitialStatesKasa()
			} else {
				settingUpdate("userSelectedReload", "false", "bool")
			}
		}
		section("${textCopyright()}")
	}
}

//	----- USER DEVICE PREFERENCES PAGE -----
def userDevicePreferencesPage()	{
	if (!userSelectedManagerMode) {
		checkForDevicesKasa()
	} else {
		checkForDevicesHub()
	}
	def userDevicePreferencesPageText = "Welcome to the Device Preferences page. \n" + "Enter a value for Transition Time and Refresh Rate then select the devices that you want to update. \n" + "After that you may procide to save by clicking the save button."
	return dynamicPage (name: "userDevicePreferencesPage", title: "Device Preferences Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (!userSelectedManagerMode) {
				if (state.TpLinkToken != null) {
					paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
				} else {
					paragraph tokenInfoOffline(), image: getAppImg("error.png")
				}
			}
			paragraph title: "Information: ", userDevicePreferencesPageText, image: getAppImg("information.png")
		}
		section("Device Configuration:") {
			if (!userSelectedManagerMode) {
				input ("userSelectedDevicesToUpdateKasa", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices to Update (${state.oldkasadevices.size() ?: 0} found)", metadata: [values: state.oldkasadevices], image: getAppImg("devices.png"))
			} else {
				input ("userSelectedDevicesToUpdateHub", "enum", required: true, multiple: true, submitOnChange: true, title: "Select Devices to Update (${state.oldkasadevices.size() ?: 0} found)", metadata: [values: state.oldkasadevices], image: getAppImg("devices.png"))
				input ("deviceIPAddress", "text", title: "Device IP", required: true, image: getDevImg("samsunghub.png"))
				input ("gatewayIPAddress", "text", title: "Gateway IP", required: true, image: getDevImg("router.png"))
			}
			input ("userLightTransTime", "enum", required: true, multiple: false, submitOnChange: false, title: "Lighting Transition Time", metadata: [values: ["500" : "0.5 second", "1000" : "1 second", "1500" : "1.5 second", "2000" : "2 seconds", "2500" : "2.5 seconds", "5000" : "5 seconds", "10000" : "10 seconds", "20000" : "20 seconds", "40000" : "40 seconds", "60000" : "60 seconds"]], image: getAppImg("transition.png"))
			input ("userRefreshRate", "enum", required: true, multiple: false, submitOnChange: false, title: "Device Refresh Rate", metadata: [values: ["1" : "Refresh every minute", "5" : "Refresh every 5 minutes", "10" : "Refresh every 10 minutes", "15" : "Refresh every 15 minutes", "30" : "Refresh every 30 minutes"]], image: getAppImg("refresh.png"))
		}
		section("${textCopyright()}")
	}
}

//	----- USER AUTHENTICATION PREFERENCES PAGE -----
def kasaUserAuthenticationPreferencesPage()	{
	def kasaUserAuthenticationPreferencesPageText = "If possible, open the IDE and select Live Logging. Then, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete."
	return dynamicPage (name: "kasaUserSelectionAuthenticationPage", title: "Login Settings Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			if (state.TpLinkToken != null) {
				paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
			} else {
				paragraph tokenInfoOffline(), image: getAppImg("error.png")
			}
			paragraph title: "Information: ", kasaUserAuthenticationPreferencesPageText, image: getAppImg("information.png")
		}
		section("Account Configuration:") {
			input ("userName", "email", title: "TP-Link Kasa Email Address", required: true, submitOnChange: false, image: getAppImg("email.png"))
			input ("userPassword", "password", title: "TP-Link Kasa Account Password", required: true, submitOnChange: false, image: getAppImg("password.png"))
		}
		section("${textCopyright()}")
	}
}

//	----- TOKEN MANAGER PAGE -----
def kasaUserSelectionTokenPage()	{
	def kasaUserSelectionTokenPageText = "Your current token: ${state.TpLinkToken}" +
		"\nAvailable actions:\n" +
		"Update Token: Updates the token on your SmartThings Account from your TP-Link Kasa Account. \n" +
		"Remove Token: Removes the token on your SmartThings Account from your TP-Link Kasa Account. \n" +
		"Recheck Token: This will attempt to check if the token is valid as well as check for errors."
		def errorMsgTok = "None"
		if (state.TpLinkToken == null) {
			errorMsgTok = "You will be unable to control your devices until you get a new token."
		}
		if (state.currentError != null) {
			errorMsgTok = "You may not be able to control your devices until you update your credentials."
		}
	dynamicPage (name: "kasaUserSelectionTokenPage", title: "Token Manager Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information and Diagnostics: ", hideable: true, hidden: true) {
			paragraph title: "Information: ", kasaUserSelectionTokenPageText, image: getAppImg("information.png")
			paragraph title: "Account Error: ", errorMsgTok, image: getAppImg("error.png")
		}
		section("Account Status:") {
			if (state.TpLinkToken != null) {
				paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
			} else {
				paragraph tokenInfoOffline(), image: getAppImg("error.png")
			}
		}
		section("User Configuration:") {
			input ("userSelectedOptionThree", "enum", title: "What do you want to do?", required: true, multiple: false, submitOnChange: true, metadata: [values: ["Update Token", "Recheck Token", "Delete Token"]], image: getAppImg("token.png"))
		}
		section("Command Status:") {
			if (userSelectedOptionThree != null) {
				if (state.currentError != null) {
					paragraph pageSelectorErrorText(), image: getAppImg("error.png")
				} else {
					paragraph sendingCommandSuccess(), image: getAppImg("sent.png")
				}
			} else {
				paragraph sendingCommandFailed(), image: getAppImg("issue.png")
			}
			if (userSelectedOptionThree =~ "Update Token") {
				getToken()
			}
			if (userSelectedOptionThree =~ "Delete Token") {
				state.TpLinkToken = null
			}
			if (userSelectedOptionThree =~ "Recheck Token") {
				checkError()
			}
		}
		section("${textCopyright()}")
	}
}

//	----- DEVELOPER PAGE -----
def developerPage()	{
	cleanStorage()
	checkForUpdates()
	checkForDevicesKasa()
	def hub = location.hubs[0]
	def hubId = hub.id
	def strLatestSmartAppVersion = textSmartAppVersion()
	def strLatestDriverVersion = textDriverVersion()
	def strLoadedDriverVersion = "Tunable White Bulb: ${state.devTWBVer}, Soft White Bulb: ${state.devSWBVer}, Color Bulb: ${state.devCBVer}, Plug: ${state.devPGVer}, Energy Monitor Plug: ${state.devEMPGVer}, Switch: ${state.devSHVer}, Dimming Switch: ${state.devDSHVer}"
	return dynamicPage (name: "developerPage", title: "Developer Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Application Information: ", hideable: true, hidden: true) {
			paragraph title: "TP-Link Token: ", "${state.TpLinkToken}", image: getAppImg("token.png")
			paragraph title: "Hub: ", "${hub}", image: getAppImg("samsunghub.png")
			paragraph title: "Hub ID: ", "${hubId}", image: getAppImg("samsunghub.png")
			paragraph title: "Latest Smart Application Version: ", "${strLatestSmartAppVersion}", image: getAppImg("kasa.png")
			paragraph title: "Latest Device Handlers Version: ", "${strLatestDriverVersion}", image: getAppImg("devices.png")
			paragraph title: "Current Smart Application Version: ", "${appVersion()}", image: getAppImg("kasa.png")
			paragraph title: "Current Device Handlers Version: ", "${strLoadedDriverVersion}", image: getAppImg("devices.png")
			paragraph title: "Beta Build: ", "${betaMarker()}", image: getAppImg("beta.png")
			paragraph title: "GitHub Namespace: ", "${appNamespace()}", image: getAppImg("github.png")
			paragraph title: "Device Handlers Namespace: ", "${driverNamespace()}", image: getAppImg("devices.png")
			paragraph title: "Username: ", "${userName}", image: getAppImg("email.png")
			paragraph title: "Password: ", "${userPassword}", image: getAppImg("password.png")
			paragraph title: "Managed Devices: ", "${state.oldkasadevices}", image: getAppImg("devices.png")
			paragraph title: "New Devices: ", "${state.newkasadevices}", image: getAppImg("devices.png")
		}
		section("Page Selector:") {
			if (userSelectedTestingPage) {
				href "startPage", title: "Initialization Page", description: "This page is not viewable", image: getAppImg("computerpages.png")
			}
			href "welcomePage", title: "Cloud Controller Dashboard", description: "Tap to view", image: getAppImg("welcomepage.png")
			href "welcomePage", title: "Hub Controller Dashboard", description: "Tap to view", image: getAppImg("welcomepage.png")
			href "kasaUserSelectionAuthenticationPage", title: "Login Page", description: "Tap to view", image: getAppImg("userselectionauthenticationpage.png")
			href "hubUserAuthenticationPreferencesPage", title: "Login Settings Page", description: "Tap to view", image: getAppImg("userauthenticationpreferencespage.png")
			if (userSelectedTestingPage) {
				href "kasaComputerSelectionAuthenticationPage", title: "Computer Login Page", description: "This page is not viewable", image: getAppImg("computerpages.png")
			}
			href "hubBridgeDiscoveryPage", title: "Bridge Discovery Page", description: "Tap to continue", image: getAppImg("samsunghub.png")
			href "kasaUserSelectionPage", title: "Launcher Page", description: "Tap to view", image: getAppImg("userselectionpage.png")
			if (userSelectedTestingPage) {
				href "kasaComputerSelectionPage", title: "Computer Launcher Page", description: "This page is not viewable", image: getAppImg("computerpages.png")
			}
			href "userAddDevicesPage", title: "Cloud Device Installer Page", description: "Tap to view", image: getAppImg("adddevicespage.png")
			href "userAddDevicesPage", title: "Hub Device Installer Page", description: "Tap to view", image: getAppImg("adddevicespage.png")
			href "userRemoveDevicesPage", title: "Cloud Device Uninstaller Page", description: "Tap to view", image: getAppImg("removedevicespage.png")
			href "userRemoveDevicesPage", title: "Hub Device Uninstaller Page", description: "Tap to view", image: getAppImg("removedevicespage.png")
			href "userApplicationPreferencesPage", title: "Cloud Application Settings Page", description: "Tap to view", image: getAppImg("userapplicationpreferencespage.png")
			href "userApplicationPreferencesPage", title: "Hub Application Settings Page", description: "Tap to view", image: getAppImg("userapplicationpreferencespage.png")
			href "userDevicePreferencesPage", title: "Cloud Device Preferences Page", description: "Tap to view", image: getAppImg("userdevicepreferencespage.png")
			href "userDevicePreferencesPage", title: "Hub Device Preferences Page", description: "Tap to view", image: getAppImg("userdevicepreferencespage.png")
			href "kasaUserSelectionTokenPage", title: "Token Manager Page", description: "Tap to view", image: getAppImg("userselectiontokenpage.png")
			if (userSelectedTestingPage) {
				href "developerPage", title: "Developer Page", description: "You are currently on this page", image: getAppImg("developerpage.png")
				href "developerTestingPage", title: "Developer Testing Page", description: "Tap to view", image: getAppImg("testingpage.png")
			}
			if ("${restrictedRecordPasswordPrompt}" =~ "Mac5089") {
				href "hiddenPage", title: "xKiller Clan Page", description: "Tap to view", image: getAppImg("xkillerclanpage.png")
			}
			href "aboutPage", title: "About Page", description: "Tap to view", image: getAppImg("aboutpage.png")
			href "changeLogPage", title: "Changelog Page", description: "Tap to view", image: getAppImg("changelogpage.png")
			href "uninstallPage", title: "Uninstall Page", description: "Tap to view", image: getAppImg("uninstallpage.png")
		}
		section("${textCopyright()}")
	}
}

//	----- DEVELOPER TESTING PAGE -----
def developerTestingPage()	{
	checkForDevicesKasa()
	def errorMsgCom = "None"
	def errorMsgDev = "None"
	def errorMsgNew = "None"
	def errorMsgOld = "None"
	def errorMsgTok = "None"
	if (state.TpLinkToken == null) {
		errorMsgTok = "You will be unable to control your devices until you get a new token."
	}
	if (state.currentError != null) {
		errorMsgCom = "Error communicating with cloud:\n" + "${state.currentError}" +
			"\nPlease resolve the error and try again."
	}
	if (state.kasadevices == [:]) {
		errorMsgDev = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+
		"that all devices are in 'Local Control Only'. Correct them then " +
		"rerun the application."
	}
	if (state.newkasadevices == [:]) {
		errorMsgNew = "No new devices to add. Are you sure they are in Remote " +
		"Control Mode?"
	}
	if (state.oldkasadevices == [:]) {
		errorMsgOld = "No current devices to remove from SmartThings."
	}
	return dynamicPage (name: "developerTestingPage", title: "Developer Testing Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Application Information: ", hideable: true, hidden: true) {
			paragraph title: "Communication Error: ", errorMsgCom, image: getAppImg("error.png")
			paragraph title: "Finding Devices Error: ", errorMsgDev, image: getAppImg("error.png")
			paragraph title: "New Devices Error: ", errorMsgNew, image: getAppImg("error.png")
			paragraph title: "Current Devices Error: ", errorMsgOld, image: getAppImg("error.png")
			paragraph title: "Account Error: ", errorMsgTok, image: getAppImg("error.png")
			paragraph title: "Error Count: ", "${state.errorCount}", image: getAppImg("error.png")
			paragraph title: "Current Error: ", "${state.currentError}", image: getAppImg("error.png")
			paragraph title: "Error Messages: ", "${errMsg}", image: getAppImg("error.png")
		}
		section("Information and Diagnostics:") {
			paragraph tokenInfoOnline(), image: getAppImg("tokenactive.png")
			paragraph tokenInfoOffline(), image: getAppImg("error.png")
		}
		section("Page Selector:") {
			paragraph pageSelectorErrorText(), image: getAppImg("error.png")
			paragraph sendingCommandSuccess(), image: getAppImg("sent.png")
			paragraph sendingCommandFailed(), image: getAppImg("issue.png")
			paragraph pageSelectorText(), image: getAppImg("pageselected.png")
			paragraph pageSelectorNullText(), image: getAppImg("pickapage.png")
		}
		section("Account Configuration:") {
			input ("userName", "email", title: "TP-Link Kasa Email Address", required: true, submitOnChange: false,image: getAppImg("email.png"))
			input ("userPassword", "password", title: "TP-Link Kasa Account Password", required: true, submitOnChange: false, image: getAppImg("password.png"))
		}
		section("User Configuration:") {
			input ("userSelectedOptionTwo", "enum", title: "What do you want to do?", required: true, multiple: false, submitOnChange: true, metadata: [values: ["Update Account", "Activate Account", "Delete Account"]], image: getAppImg("userinput.png"))
			input ("userSelectedOptionOne", "enum", title: "What do you want to do?", required: true, multiple: false, submitOnChange: true, metadata: [values: ["Add Devices", "Remove Devices", "Update Token", "Initial Installation"]], image: getAppImg("userinput.png"))
			input ("userSelectedOptionThree", "enum", title: "What do you want to do?", required: true, multiple: false, submitOnChange: true, metadata: [values: ["Update Token", "Recheck Token", "Delete Token"]], image: getAppImg("token.png"))
		}
		section("Device Controller:") {
			input ("userSelectedDevicesAddKasa", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices (${state.newkasadevices.size() ?: 0} found)", metadata: [values: state.newkasadevices], image: getAppImg("adddevices.png"))
			input ("userSelectedDevicesRemoveKasa", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices (${state.oldkasadevices.size() ?: 0} found)", metadata: [values: state.oldkasadevices], image: getAppImg("removedevices.png"))
		}
		section("Application Configuration:") {
			input ("userSelectedNotification", "bool", title: "Do you want to enable notification?", submitOnChange: false, image: getAppImg("notification.png"))
			input ("userSelectedAppIcons", "bool", title: "Do you want to disable application icons?", submitOnChange: false, image: getAppImg("noicon.png"))
			input ("userSelectedManagerMode", "bool", title: "Do you want to switch to hub controller mode?", submitOnChange: false, image: getAppImg("samsunghub.png"))
			input ("userSelectedAssistant", "bool", title: "Do you want to enable recommended options?", submitOnChange: false, image: getAppImg("ease.png"))
			input ("userSelectedBrowserMode", "bool", title: "Do you want to open all external links within the SmartThings app?", submitOnChange: false, image: getAppImg("browsermode.png"))
			input ("userSelectedReload", "bool", title: "Do you want to refresh your current state?", submitOnChange: true, image: getAppImg("sync.png"))
			input ("userSelectedDeveloper", "bool", title: "Do you want to enable developer mode?", submitOnChange: true, image: getAppImg("developer.png"))
			input ("userSelectedLauncher", "bool", title: "Do you want to disable the launcher page?", submitOnChange: false, image: getAppImg("launcher.png"))
			input ("userSelectedQuickControl", "bool", title: "Do you want to enable post install features?", submitOnChange: false, image: getAppImg("quickcontrol.png"))
			input ("userSelectedTestingPage", "bool", title: "Do you want to enable developer testing mode?", submitOnChange: true, image: getAppImg("developertesting.png"))
			input ("userSelectedDriverNamespace", "bool", title: "Do you want to switch the device handlers namespace?", submitOnChange: false, image: getAppImg("drivernamespace.png"))
		}
		section("Device Configuration:") {
			input ("userSelectedDevicesToUpdateKasa", "enum", required: true, multiple: true, submitOnChange: false, title: "Select Devices to Update (${state.oldkasadevices.size() ?: 0} found)", metadata: [values: state.oldkasadevices], image: getAppImg("devices.png"))
			input ("userLightTransTime", "enum", required: true, multiple: false, submitOnChange: false, title: "Lighting Transition Time", metadata: [values: ["500" : "0.5 second", "1000" : "1 second", "1500" : "1.5 second", "2000" : "2 seconds", "2500" : "2.5 seconds", "5000" : "5 seconds", "10000" : "10 seconds", "20000" : "20 seconds", "40000" : "40 seconds", "60000" : "60 seconds"]], image: getAppImg("transition.png"))
			input ("userRefreshRate", "enum", required: true, multiple: false, submitOnChange: false, title: "Device Refresh Rate", metadata: [values: ["1" : "Refresh every minute", "5" : "Refresh every 5 minutes", "10" : "Refresh every 10 minutes", "15" : "Refresh every 15 minutes", "30" : "Refresh every 30 minutes"]], image: getAppImg("refresh.png"))
		}
		section("${textCopyright()}")
	}
}

//	----- HIDDEN PAGE -----
def hiddenPage()	{
	def xkMembersInfo = "Although most of these members have left here is a complete list of all the members we had"
	def xkMembers = "xKllerBOSSXXX, xKillerDDigital, xKillerIntense, xKillerMaverick, xKillerKittyKat, xKillerPP, xKillerBrute, xKillerBSOD, xKillerFoxy, xKillerTricky, xKillerReaper, xKillerPain, xKillerRobot, xKillerSasha, XKillerAwesomer, xKillerSonic, xKillerChakra, xKillerDoobage, xKillerSeki, xKillerEvo, xKillerSubXero, xKillerCali, xKillerAsh, xKillerTruKillah,xKillerSierra, Weirdowack"
	def xkGameInfo = "Although we may not play most of these games anymore but as a bunch of friends and some family had fun along the way but i guess some things just don't last"
	dynamicPage (name: "hiddenPage", title: "xKiller Clan Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Members:") {
			paragraph xkMembersInfo, image: getAppImg("xkillerclanv2.png")
			paragraph xkMembers, image: getAppImg("family.png")
		}
		section("Games:") {
			paragraph xkGameInfo, image: getAppImg("xkillerclanv1.png")
			paragraph "Halo 2 For Windows Vista - RIP late 2015", image: getAppImg("halo2.png")
			paragraph "Battlefield 3", image: getAppImg("battlefield3.png")
			paragraph "Garrys Mod", image: getAppImg("garrysmod.png")
			paragraph "Portal 2", image: getAppImg("portal2.png")
			paragraph "Dead Speace 3", image: getAppImg("deadspace3.png")
			paragraph "Clash of Clans - Clan Tag: #YYCLJ2YR", image: getAppImg("clashofclans.png")
			paragraph "Halo: The Master Chief Collection", image: getAppImg("halomcc.png")
			paragraph "Clash Royale - Clan Tag: #209G8L9", image: getAppImg("clashroyale.png")
			paragraph "Saints Row 3", image: getAppImg("saintsrow3.png")
			paragraph "Boom Beach - Clan Tag: #92V92QCC", image: getAppImg("boombeach.png")
			paragraph "Call of Duty Black Ops 2", image: getAppImg("callofdutyblackops2.png")
			paragraph "Halo 5 Guardians", image: getAppImg("halo5.png")
			paragraph "Vainglory - Guild: XKILLER, Team: xKiller Clan", image: getAppImg("vainglory.png")
			paragraph "Minecraft Bedrock Edition - Realm: 0EOy4uYzhxQ", image: getAppImg("minecraft.png")
		}
		section("Easter Eggs:") {
			href url: linkYoutubeEE1(), style: "${strBrowserMode()}", required: false, title: "Youtube Link #1", description: "Tap to open in browser", state: "complete", image: getAppImg("youtube.png")
			href url: linkYoutubeEE2(), style: "${strBrowserMode()}", required: false, title: "Youtube Link #2", description: "Tap to open in browser", state: "complete", image: getAppImg("youtube.png")
			href url: linkYoutubeEE3(), style: "${strBrowserMode()}", required: false, title: "Youtube Link #3", description: "Tap to open in browser", state: "complete", image: getAppImg("youtube.png")
		}
		section("Contact:") {
			href url: linkDiscord(), style: "${strBrowserMode()}", required: false, title: "Discord", description: "Tap to open in browser", state: "complete", image: getAppImg("discord.png")
			href url: linkWaypoint(), style: "${strBrowserMode()}", required: false, title: "Halo Waypoint", description: "Tap to open in browser", state: "complete", image: getAppImg("waypoint.png")
			href url: linkXbox(), style: "${strBrowserMode()}", required: false, title: "Xbox", description: "Tap to open in browser", state: "complete", image: getAppImg("xbox.png")
			href url: linkSteam(), style: "${strBrowserMode()}", required: false, title: "Steam", description: "Tap to open in browser", state: "complete", image: getAppImg("steam.png")
			href url: linkFacebook(), style: "${strBrowserMode()}", required: false, title: "Facebook", description: "Tap to open in browser", state: "complete", image: getAppImg("facebook.png")
		}
		section("${textCopyright()}")
	}
}

//	----- ABOUT PAGE -----
def aboutPage()	{
	dynamicPage (name: "aboutPage", title: "About Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png", true)
		}
		section("Donations:") {
			paragraph title: "Donations (@DaveGut)", "Donate to a charity", state: "complete", image: getAppImg("heart.png")
			href url: textDonateLinkAntR(), style: "${strBrowserMode()}", required: false, title: "Donations (@ramiran2)", description: "Tap to open in browser", state: "complete", image: getAppImg("paypal.png")
		}
		section("Credits:") {
			paragraph title: "Creator: ", "Dave G. (@DaveGut)", state: "complete", image: getAppImg("dave.png")
			paragraph title: "Co-Author: ", "Anthony R. (@ramiran2)", state: "complete", image: getAppImg("bigmac.png")
			if ("${restrictedRecordPasswordPrompt}" =~ "Mac5089") {
			paragraph title: "Unknown: ", "Lindsey M. (@Unknown)", state: "complete", image: getAppImg("unknown.png")
			}
			paragraph title: "Collaborator: ", "Anthony S. (@tonesto7)", state: "complete", image: getAppImg("tonesto7.png")
		}
		section("Application Changes Details:") {
			href "changeLogPage", title: "View App Revision History", description: "Tap to view", image: getAppImg("changelogpage.png")
		}
		section("GitHub:") {
			href url: linkGitHubDavG(), style: "${strBrowserMode()}", required: false, title: "Dave G. (@DaveGut)", description: "Tap to open in browser", state: "complete", image: getAppImg("github.png")
			href url: linkGitHubAntR(), style: "${strBrowserMode()}", required: false, title: "Anthony R. (@ramiran2)", description: "Tap to open in browser", state: "complete", image: getAppImg("github.png")
			href url: linkGitHubAntS(), style: "${strBrowserMode()}", required: false, title: "Anthony S. (@tonesto7)", description: "Tap to open in browser", state: "complete", image: getAppImg("github.png")
		}
		section("Licensing Information:") {
			paragraph "${textLicense()}"
		}
		section("${textCopyright()}")
	}
}

//	----- CHANGELOG PAGE -----
def changeLogPage()	{
	cleanStorage()
	checkForUpdates()
	def intUpdateCheckOne = 0
	def intUpdateCheckTwo = 0
	def childDevices = app.getChildDevices(true)
	def strLatestSmartAppVersion = textSmartAppVersion()
	def updateNeeded = "Both the Smart Application and Device Handlers need to be updated"
	def upToDate = "Both the Smart Application and Device Handlers are up to date"
	def driverUpdateNeeded = "Your Device Handlers need to be updated"
	def smartAppUpdateNeeded = "Your Smart Application needs to be updated"
	def updateFailed = "We are unable to check for updates"
	def updateNeedsDevices = "We are unable to check for updates, please check if you have any devices installed"
	dynamicPage (name: "changeLogPage", title: "Changelog Page", install: false, uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Check for Updates:") {
			if (childDevices) {
				if ("${strLatestSmartAppVersion}" =~ "${appVersion()}" && "${state.devManVer}" =~ "${state.devVerLnk}") {
					paragraph upToDate, image: getAppImg("success.png")
				} else {
					if ("${strLatestSmartAppVersion}" =~ "${appVersion()}" && "${state.devManVer}" =~ "${state.devVerLnk}") {
						if ("${strLatestSmartAppVersion}" != "${appVersion()}") {
							paragraph smartAppUpdateNeeded, image: getAppImg("issue.png")
						} else {
							intUpdateCheckOne = 1
						}
						if ("${state.devManVer}" != "${state.devVerLnk}") {
							paragraph driverUpdateNeeded, image: getAppImg("issue.png")
						} else {
							intUpdateCheckTwo = 1
						}
						if (intUpdateCheckOne == 1 && intUpdateCheckTwo == 1) {
							paragraph updateFailed, image: getAppImg("error.png")
						}
					} else {
						paragraph updateNeeded, image: getAppImg("error.png")
					}
				}
			} else {
				if ("${strLatestSmartAppVersion}" =~ "${appVersion()}") {
					paragraph upToDate, image: getAppImg("success.png")
				} else {
					paragraph updateNeedsDevices, image: getAppImg("issue.png")
				}
			}
		}
		section("Changelog:") {
			paragraph title: "What's New in this Release...", "", state: "complete", image: getAppImg("new.png")
			paragraph appVerInfo()
		}
		section("${textCopyright()}")
	}
}

//	----- UNINSTALL PAGE -----
def uninstallPage()	{
	def uninstallPageText = "This will uninstall the All Child Devices including this Application with all it's user data. \nPlease make sure that any devices created by this app are removed from any routines/rules/smartapps before tapping Remove."
	dynamicPage (name: "uninstallPage", title: "Uninstall Page", install: false, uninstall: true) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa.png")
		}
		section("Information:") {
			paragraph title: "", uninstallPageText, image: getAppImg("information.png")
		}
		section("${textCopyright()}")
		remove("Uninstall this application", "Warning!!!", "Last Chance to Stop! \nThis action is not reversible \n\nThis will remove All Devices including this Application with all it's user data")
	}
}

def setInitialStatesKasa()	{
	if (!state.TpLinkToken) {state.TpLinkToken = null}
	if (!state.kasadevices) {state.kasadevices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
	settingUpdate("userSelectedReload", "false", "bool")
	settingRemove("userSelectedDevicesRemoveKasa")
	settingRemove("userSelectedDevicesAddKasa")
	settingRemove("userSelectedDevicesToUpdateKasa")
	settingRemove("userSelectedOptionThree")
	if ("${userName}" =~ null || "${userPassword}" =~ null) {
		settingRemove("userName")
		settingRemove("userPassword")
	}
	if (userSelectedOptionTwo =~ "Delete Account") {
		settingRemove("userSelectedOptionTwo")
	}
	if (userSelectedOptionThree =~ "Delete Token") {
		settingRemove("userSelectedOptionThree")
	}
	if (!userSelectedDeveloper) {
		settingUpdate("userSelectedLauncher", "true", "bool")
		if ("${userName}" =~ null || "${userPassword}" =~ null) {
			state.TpLinkToken = null
			state.currentError = null
			state.errorCount = 0
			settingUpdate("userSelectedQuickControl", "false", "bool")
		} else {
			settingUpdate("userSelectedQuickControl", "true", "bool")
		}
		settingUpdate("userSelectedDriverNamespace", "false", "bool")	//	If true the DaveGut is set as default
	}
}

def setInitialStatesHub()	{
	if (!state.bridgeIP) {state.bridgeIP = "new"}
	if (!state.bridgeDNI) {state.bridgeDNI = "new"}
	if (!state.bridges) {state.bridges = [:]}
	if (!state.hubdevices) {state.hubdevices = [:]}
	if (!state.bridgePort) {state.bridgePort = 8082}
	settingRemove("userSelectedDevicesRemoveHub")
	settingRemove("userSelectedDevicesAddHub")
	settingRemove("userSelectedDevicesToUpdateHub")
	if (!userSelectedDeveloper) {
		settingUpdate("userSelectedLauncher", "true", "bool")
		if (state.bridgeIP == "new") {
			settingUpdate("userSelectedQuickControl", "false", "bool")
		} else {
			settingUpdate("userSelectedQuickControl", "true", "bool")
		}
		settingUpdate("userSelectedDriverNamespace", "false", "bool")	//	If true the DaveGut is set as default
	}
}

def cleanStorage()	{
	state.devManVer = null
	state.devTWBVer = null
	state.devSWBVer = null
	state.devCBVer = null
	state.devPGVer = null
	state.devEMPGVer = null
	state.devSHVer = null
	state.devDSHVer = null
	state.devVerLnk = null
}

def setRecommendedOptions()	{
	def childDevices = app.getChildDevices(true)
	if ("${userName}" =~ null || "${userPassword}" =~ null) {
		settingUpdate("userSelectedOptionTwo", "Activate Account", "enum")
	} else {
		settingUpdate("userSelectedOptionTwo", "Update Account", "enum")
	}
	if (state.TpLinkToken != null) {
		if (childDevices) {
			settingUpdate("userSelectedOptionOne", "Add Devices", "enum")
		}
		if (!childDevices) {
			settingUpdate("userSelectedOptionOne", "Remove Devices", "enum")
		}
	} else {
		if ("${userName}" =~ null || "${userPassword}" =~ null) {
			settingUpdate("userSelectedOptionOne", "Initial Installation", "enum")
		} else {
			settingUpdate("userSelectedOptionOne", "Update Token", "enum")
		}
	}
	if (state.currentError != null) {
		settingUpdate("userSelectedOptionThree", "Recheck Token", "enum")
	} else {
		if (state.TpLinkToken != null) {
			if (userSelectedOptionTwo =~ "Update Account") {
				settingUpdate("userSelectedOptionThree", "Update Token", "enum")
			}
		} else {
			settingUpdate("userSelectedOptionThree", "Update Token", "enum")
		}
	}
}

def checkForDevicesKasa()	{
	getDevices()
	def devices = state.kasadevices
	def newKasaDevices = [:]
	def oldKasaDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (isChild) {
			oldKasaDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
		if (!isChild) {
			newKasaDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	state.oldkasadevices = oldKasaDevices
	state.newkasadevices = newKasaDevices
}

def checkForDevicesHub()	{
	def devices = state.hubdevices
	def newHubDevices = [:]
	def oldHubDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (isChild) {
			oldHubDevices["${it.value.deviceMac}"] = "$it.value.deviceIP : ${it.value.deviceAlias} model ${it.value.deviceModel}"
		}
		if (!isChild) {
			newHubDevices["${it.value.deviceMac}"] = "$it.value.deviceIP : ${it.value.deviceAlias} model ${it.value.deviceModel}"
		}
	}
	state.oldhubdevices = oldHubDevices
	state.newhubdevices = newHubDevices
}

def checkForBridgeHub()	{
	def bridgeIP = state.bridgeIP
	def strFoundBridge = [:]
	def verBridges = state.bridges.findAll{ it.value.verified == true }
	verBridges.each {
		strFoundBridge["${it.value.bridgeMac}"] = "${it.value.bridgeIP} : ${it.value.nodeApp}"
	}
	state.strfoundbridge = strFoundBridge
}

def checkForUpdates()	{
	def strLatestSmartAppVersion = textSmartAppVersion()
	def strLatestDriverVersion = textDriverVersion()
	def intMessage = 0
	def strDevVersion = state.devManVer ?: [:]
	strDevVersion["devVer"] = strLatestDriverVersion ?: ""
	state.devManVer = strDevVersion
	def childDevices = app.getChildDevices(true)
	childDevices?.each {
		def strTypRawData = it?.currentState("devTyp")?.value?.toString()
		def strDeviceType = state.devTyp ?: [:]
		strDeviceType["devTyp"] = strTypRawData ?: ""
		state.devTyp = strDeviceType
		if (state.devTyp =~ "Tunable White Bulb") {
			def strTWBRawData = it?.currentState("devVer")?.value?.toString()
			def strTWB = state.devTWBVer ?: [:]
			strTWB["devVer"] = strTWBRawData ?: ""
			state.devTWBVer = strTWB
		}
		if (state.devTyp =~ "Soft White Bulb") {
			def strSWBRawData = it?.currentState("devVer")?.value?.toString()
			def strSWB = state.devSWBVer ?: [:]
			strSWB["devVer"] = strSWBRawData ?: ""
			state.devSWBVer = strSWB
		}
		if (state.devTyp =~ "Color Bulb") {
			def strCBRawData = it?.currentState("devVer")?.value?.toString()
			def strCB = state.devCBVer ?: [:]
			strCB["devVer"] = strCBRawData ?: ""
			state.devCBVer = strCB
		}
		if (state.devTyp =~ "Plug") {
			def strPGRawData = it?.currentState("devVer")?.value?.toString()
			def strPG = state.devPGVer ?: [:]
			strPG["devVer"] = strPGRawData ?: ""
			state.devPGVer = strPG
		}
		if (state.devTyp =~ "Energy Monitor Plug") {
			def strEMPGRawData = it?.currentState("devVer")?.value?.toString()
			def strEMPG = state.devEMPGVer ?: [:]
			strEMPG["devVer"] = strEMPGRawData ?: ""
			state.devEMPGVer = strEMPG
		}
		if (state.devTyp =~ "Switch") {
			def strSHRawData = it?.currentState("devVer")?.value?.toString()
			def strSH = state.devSHVer ?: [:]
			strSH["devVer"] = strSHRawData ?: ""
			state.devSHVer = strSH
		}
		if (state.devTyp =~ "Dimming Switch") {
			def strDSHRawData = it?.currentState("devVer")?.value?.toString()
			def strDSH = state.devDSHVer ?: [:]
			strDSH["devVer"] = strDSHRawData ?: ""
			state.devDSHVer = strDSH
		}
	}
	if (state.devTWBVer =~ null) {
		state.devTWBVer = strDevVersion
	} else {
		state.devVerLnk = state.devTWBVer
	}
	if (state.devSWBVer =~ null) {
		state.devSWBVer = strDevVersion
	} else {
		state.devVerLnk = state.devSWBVer
	}
	if (state.devCBVer =~ null) {
		state.devCBVer = strDevVersion
	} else {
		state.devVerLnk = state.devCBVer
	}
	if (state.devPGVer =~ null) {
		state.devPGVer = strDevVersion
	} else {
		state.devVerLnk = state.devPGVer
	}
	if (state.devEMPGVer =~ null) {
		state.devEMPGVer = strDevVersion
	} else {
		state.devVerLnk = state.devEMPGVer
	}
	if (state.devSHVer =~ null) {
		state.devSHVer = strDevVersion
	} else {
		state.devVerLnk = state.devSHVer
	}
	if (state.devDSHVer =~ null) {
		state.devDSHVer = strDevVersion
	} else {
		state.devVerLnk = state.devDSHVer
	}
	if ("${state.devManVer}" =~ "${state.devVerLnk}") {
		intMessage = 3
	} else {
		if (userSelectedNotification) {
			sendPush("${appLabel()} Device Handlers need to be updated")
		}
	}
	if ("${strLatestSmartAppVersion}" =~ "${appVersion()}" ) {
		if (intMessage == 3) {
			intMessage = 2
		} else {
			intMessage = 1
		}
	} else {
		if (userSelectedNotification) {
			sendPush("${appLabel()} needs to be updated")
		}
	}
}

def updatePreferences()	{
	userSelectedDevicesToUpdateKasa.each {
		def child = getChildDevice(it)
		child.setLightTransTime(userLightTransTime)
		child.setRefreshRate(userRefreshRate)
		log.info "Kasa device ${child} preferences updated"
		if (userSelectedNotification) {
			sendPush("Successfully updated TP-Link $deviceModel with alias ${device.value.alias}")
		}
	}
}

void settingUpdate(name, value, type=null) {
	log.trace "settingUpdate($name, $value, $type)..."
	if(name) {
		if(value == "" || value == null || value == []) {
			settingRemove(name)
			return
		}
	}
	if(name && type) {
		app?.updateSetting("$name", [type: "$type", value: value])
	}
	else if (name && type == null) { app?.updateSetting(name.toString(), value) }
}

void settingRemove(name) {
	log.trace "settingRemove($name)..."
	if(name) { app?.deleteSetting("$name") }
}

def getDevices()	{
	def currentDevices = getDeviceData()
	state.kasadevices = [:]
	def devices = state.kasadevices
	currentDevices.each {
		def device = [:]
		device["deviceMac"] = it.deviceMac
		device["alias"] = it.alias
		device["deviceModel"] = it.deviceModel
		device["deviceId"] = it.deviceId
		device["appServerUrl"] = it.appServerUrl
		devices << ["${it.deviceMac}" : device]
		def isChild = getChildDevice(it.deviceMac)
		if (isChild) {
			isChild.syncAppServerUrl(it.appServerUrl)
		}
		log.info "Device ${it.alias} added to devices array"
	}
}

def removeDevicesKasa()	{
	userSelectedDevicesRemoveKasa.each { dni ->
		try{
			def isChild = getChildDevice(dni)
			if (isChild) {
				def delete = isChild
				delete.each { deleteChildDevice(it.deviceNetworkId, true) }
			}
			if (userSelectedNotification) {
				sendPush("Successfully uninstalled TP-Link $deviceModel with alias ${device.value.alias}")
			}
		} catch (e) {
			log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
}

def removeDevicesHub()	{
	userSelectedDevicesRemoveHub.each { dni ->
		try{
			def isChild = getChildDevice(dni)
			if (isChild) {
				def delete = isChild
				delete.each { deleteChildDevice(it.deviceNetworkId, true) }
			}
			if (userSelectedNotification) {
				sendPush("Successfully uninstalled TP-Link $deviceModel with alias ${device.value.alias}")
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
			log.error "getWebData(params: $params, desc: $desc, text: $text) Exception: ", ex
		}
		return "${label} info not found"
	}
}

// ----- Add TP-Link Devices to STs --------------------------------
def addDevicesHub()	{
	def tpLinkModel = [:]
	//	Plug-Switch Devices (no energy monitor capability)
	tpLinkModel << ["HS100" : "TP-Link Smart Plug"]						//	HS100
	tpLinkModel << ["HS103" : "TP-Link Smart Plug"]						//	HS103
	tpLinkModel << ["HS105" : "TP-Link Smart Plug"]						//	HS105
	tpLinkModel << ["HS200" : "TP-Link Smart Switch"]					//	HS200
	tpLinkModel << ["HS210" : "TP-Link Smart Switch"]					//	HS210
	tpLinkModel << ["KP100" : "TP-Link Smart Plug"]						//	KP100
	//	Dimming Switch Devices
	tpLinkModel << ["HS220" : "TP-Link Smart Dimming Switch"]			//	HS220
	//	Energy Monitor Plugs
	tpLinkModel << ["HS110" : "TP-Link Smart Energy Monitor Plug"]		//	HS110
	tpLinkModel << ["HS115" : "TP-Link Smart Energy Monitor Plug"]		//	HS110
	//	Soft White Bulbs
	tpLinkModel << ["KB100" : "TP-Link Smart Soft White Bulb"]			//	KB100
	tpLinkModel << ["LB100" : "TP-Link Smart Soft White Bulb"]			//	LB100
	tpLinkModel << ["LB110" : "TP-Link Smart Soft White Bulb"]			//	LB110
	tpLinkModel << ["KL110" : "TP-Link Smart Soft White Bulb"]			//	KL110
	tpLinkModel << ["LB200" : "TP-Link Smart Soft White Bulb"]			//	LB200
	//	Tunable White Bulbs
	tpLinkModel << ["LB120" : "TP-Link Smart Tunable White Bulb"]		//	LB120
	tpLinkModel << ["KL120" : "TP-Link Smart Tunable White Bulb"]		//	KL120
	//	Color Bulbs
	tpLinkModel << ["KB130" : "TP-Link Smart Color Bulb"]				//	KB130
	tpLinkModel << ["LB130" : "TP-Link Smart Color Bulb"]				//	LB130
	tpLinkModel << ["KL130" : "TP-Link Smart Color Bulb"]				//	KL130
	tpLinkModel << ["LB230" : "TP-Link Smart Color Bulb"]				//	LB230
	userSelectedDevicesAddHub.each { dni ->
		try {
		def isChild = getChildDevice(dni)
			if (!isChild) {
				def device = state.hubdevices.find { it.value.deviceMac == dni }
				def deviceModel = device.value.deviceModel.substring(0,5)
				addChildDevice("${driverNamespace()}", tpLinkModel["${deviceModel}"], device.value.deviceMac, device?.value.hub, ["label": device.value.deviceAlias, "name": "TP-Link ${device.value.deviceModel}", "data": ["bridgeIP": state.bridgeIP, "bridgePort": state.bridgePort, "deviceIP": device.value.deviceIP, "deviceModel": device.value.deviceModel]])
			log.info "Installed TP-Link $deviceModel with alias ${device.value.deviceAlias}"
			}
		} catch (e) {
			log.debug "Error Adding ${deviceModel}: ${e}"
		}
	}
}

def addDevicesKasa()	{
	def tpLinkModel = [:]
	//	Plug-Switch Devices (no energy monitor capability)
	tpLinkModel << ["HS100" : "TP-Link Smart Plug"]						//	HS100
	tpLinkModel << ["HS103" : "TP-Link Smart Plug"]						//	HS103
	tpLinkModel << ["HS105" : "TP-Link Smart Plug"]						//	HS105
	tpLinkModel << ["HS200" : "TP-Link Smart Switch"]					//	HS200
	tpLinkModel << ["HS210" : "TP-Link Smart Switch"]					//	HS210
	tpLinkModel << ["KP100" : "TP-Link Smart Plug"]						//	KP100
	//	Dimming Switch Devices
	tpLinkModel << ["HS220" : "TP-Link Smart Dimming Switch"]			//	HS220
	//	Energy Monitor Plugs
	tpLinkModel << ["HS110" : "TP-Link Smart Energy Monitor Plug"]		//	HS110
	tpLinkModel << ["HS115" : "TP-Link Smart Energy Monitor Plug"]		//	HS110
	//	Soft White Bulbs
	tpLinkModel << ["KB100" : "TP-Link Smart Soft White Bulb"]			//	KB100
	tpLinkModel << ["LB100" : "TP-Link Smart Soft White Bulb"]			//	LB100
	tpLinkModel << ["LB110" : "TP-Link Smart Soft White Bulb"]			//	LB110
	tpLinkModel << ["KL110" : "TP-Link Smart Soft White Bulb"]			//	KL110
	tpLinkModel << ["LB200" : "TP-Link Smart Soft White Bulb"]			//	LB200
	//	Tunable White Bulbs
	tpLinkModel << ["LB120" : "TP-Link Smart Tunable White Bulb"]		//	LB120
	tpLinkModel << ["KL120" : "TP-Link Smart Tunable White Bulb"]		//	KL120
	//	Color Bulbs
	tpLinkModel << ["KB130" : "TP-Link Smart Color Bulb"]				//	KB130
	tpLinkModel << ["LB130" : "TP-Link Smart Color Bulb"]				//	LB130
	tpLinkModel << ["KL130" : "TP-Link Smart Color Bulb"]				//	KL130
	tpLinkModel << ["LB230" : "TP-Link Smart Color Bulb"]				//	LB230
	def hub = location.hubs[0]
	def hubId = hub.id
	userSelectedDevicesAddKasa.each { dni ->
		try {
			def isChild = getChildDevice(dni)
			if (!isChild) {
				def device = state.kasadevices.find { it.value.deviceMac == dni }
				def deviceModel = device.value.deviceModel.substring(0,5)
				addChildDevice("${driverNamespace()}", tpLinkModel["${deviceModel}"], device.value.deviceMac,hubId, ["label" : device.value.alias, "name" : device.value.deviceModel,"data" : ["deviceId" : device.value.deviceId, "appServerUrl" : device.value.appServerUrl]])
				log.info "Installed TP-Link $deviceModel with alias ${device.value.alias}"
				if (userSelectedNotification) {
					sendPush("Successfully installed TP-Link $deviceModel with alias ${device.value.alias}")
				}
			}
		} catch (e) {
			log.debug "Error Adding ${deviceModel}: ${e}"
		}
	}
}

//	----- GET A NEW TOKEN FROM CLOUD -----
def getToken()	{
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
def getDeviceData()	{
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
	def cmdBody = [method: "passthrough", params: [deviceId: deviceId, requestData: "${command}"]]
	def sendCmdParams = [uri: "${appServerUrl}/?token=${state.TpLinkToken}", requestContentType: 'application/json', contentType: 'application/json', headers: ['Accept':'application/json; version=1, */*; q=0.01'], body : new groovy.json.JsonBuilder(cmdBody).toString()]
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
				log.debug "state.errorCount = ${state.errorCount}	//	state.currentError = ${state.currentError}"
			}
		//log.debug "state.errorCount = ${state.errorCount}		//	state.currentError = ${state.currentError}"
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

def appInfoDesc()	{
	def str = ""
	str += "${appLabel()}"
	str += "\n" + "• ${textVersion()}"
	str += "\n" + "• ${textModified()}"
	return str
}

def uninstManagerApp()	{
	try {
		if (userSelectedNotification) {
			sendPush("${appLabel()} is uninstalled")
		}
	} catch (ex) {
		log.error "uninstManagerApp Exception: ", ex
	}
}

//	----- INSTALL, UPDATE, INITIALIZE, UNINSTALLED -----
def installed()	{
	initialize()
}

def updated()	{
	unsubscribe()
	initialize()
}

def initialize()	{
	unsubscribe()
	unschedule()
	runEvery3Hours(cleanStorage)
	runEvery3Hours(checkForUpdates)
	if (!userSelectedManagerMode) {
		runEvery5Minutes(checkError)
		schedule("0 30 2 ? * WED", getToken)
		if (userSelectedDevicesAddKasa) {
			addDevicesKasa()
		}
		if (userSelectedDevicesRemoveKasa) {
			removeDevicesKasa()
		}
		if (userSelectedDevicesToUpdateKasa) {
			updatePreferences()
		}
	} else {
		runEvery5Minutes(discoverDevices)
		ssdpSubscribe()
		if (userSelectedBridgeAddHub) {
			addBridges()
		}
		if (userSelectedDevicesAddHub) {
			addDevicesHub()
		}
		if (userSelectedDevicesRemoveHub) {
			removeDevicesHub()
		}
		if (userSelectedDevicesToUpdateHub) {
			updatePreferences()
		}
	}
}

def uninstalled()	{
	uninstManagerApp()
}

// ----- Child-called IP Refresh Function -------------------------
def checkDeviceIPs()	{
	ssdpDiscover()
	runIn(30, discoverDevices)
}

// ----- Hub Discovery ---------------------------------------------
void ssdpSubscribe()	{
	def target = getSearchTarget()
	subscribe(location, "ssdpTerm.$target", ssdpHandler)
}
void ssdpDiscover()	{
	def target = getSearchTarget()
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery $target", physicalgraph.device.Protocol.LAN))
}
def ssdpHandler(evt) {
	def bridgeDNI = state.bridgeDNI
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(description)
	def ip = convertHexToIP(parsedEvent.networkAddress)
	String mac = parsedEvent.mac.toString()
	def bridges = state.bridges
	if (bridges."${mac}") {
		def d = bridges."${mac}"
		if (d.bridgeIP != ip) {
			d.bridgeIP = ip
			if (bridgeDNI == mac) {
				state.bridgeIP = ip
				def allDevices = getChildDevices()
				allDevices.each {
					it.syncBridgeIP(ip)
					log.info "The Hub IP Address was updated to $d.bridgeIP"
					discoverDevices()
				}
			}
		}
	} else {
		log.info "Adding potential Hub with MAC: $mac and IP: $ip"
		def bridgeDevice = [:]
		bridgeDevice["bridgeMac"] = mac
		bridgeDevice["bridgeIP"] = ip
		bridgeDevice["hub"] = hub
		bridges << ["${mac}": bridgeDevice]
	}
}

// ----- Verify Discovered Devices as Hub --------------------------
void verifyBridges()	{
	def bridges = state.bridges.findAll { it?.value?.verified != true }
	def bridgePort = state.bridgePort
	bridges.each {
		def bridgeIP = it.value.bridgeIP
		def host = "${bridgeIP}:${bridgePort}"
		def headers = [:]
		headers.put("HOST", host)
		headers.put("command", "verifyBridge")
		sendHubCommand(new physicalgraph.device.HubAction([headers: headers], it.value.mac, [callback: "bridgeDescriptionHandler"]))
	}
}
void bridgeDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def bridgeMac = hubResponse.headers["bridgeMac"]
	def nodeApp = hubResponse.headers["nodeApp"]
	def bridgePort = state.bridgePort
	def foundBridge = state.bridges.find { it?.key?.contains("${bridgeMac}") }
	if (foundBridge) {
		foundBridge.value << [verified: true, nodeApp: nodeApp, bridgePort: bridgePort]
		log.info "Verified potential Hub with MAC: $bridgeMac"
	}
}

// ----- Add Hub Device to ST --------------------------------------
def addBridges()	{
	userSelectedBridgeAddHub.each { dni ->
		def selectedBridge = state.bridges.find { it.value.bridgeMac == dni }
		def strDevices
		if (selectedBridge) {
			strDevices = getChildDevices()?.find {it.deviceNetworkId == selectedBridge.value.bridgeMac}
		}
		if (!strDevices) {
			state.bridgeIP = selectedBridge.value.bridgeIP
			state.bridgeDNI = selectedBridge.value.bridgeMac
			addChildDevice("${driverNamespace()}", "TP-Link Bridge", selectedBridge.value.bridgeMac, selectedBridge?.value.hub, ["label": "TP-Link SmartThings Bridge", "name": "PC Bridge for TP-Link Devices", "data": ["bridgeIP": state.bridgeIP, "bridgePort": state.bridgePort, "connectStatus": "ok"]])
			selectedBridge.value << [installed: true]
			log.info "Installed TP-Link Hub with MAC: ${state.bridgeDNI} and IP: ${state.bridgeIP}"
		}
	}
}

// ----- TP-Link Device Discovery ----------------------------------
void discoverDevices()	{
	def headers = [:]
	def bridgePort = state.bridgePort
	def bridgeIP = state.bridgeIP
	headers.put("HOST", "$bridgeIP:$bridgePort")
	headers.put("command", "pollForDevices")
	sendHubCommand(new physicalgraph.device.HubAction([headers: headers], state.bridgeDNI, [callback: parseDiscoveredDevices]))
}
void parseDiscoveredDevices(response) {
	def cmdResponse = response.headers["cmd-response"]
	def foundDevices = parseJson(cmdResponse)
	if (foundDevices == ":") {
		log.error "Found Devices Failed. Rerun!"
	}
	def devices = state.hubdevices
	foundDevices.each {
		String foundDeviceMac = it.value.deviceMac
		if (devices."${foundDeviceMac}") {
			def device = devices."${foundDeviceMac}"
			if (it.value.deviceIP != device.deviceIP ) {
				device.deviceIP = it.value.deviceIP
				alias = it.value.alias
				def child = getChildDevice(foundDeviceMac)
				if (child) {
					child.syncDeviceIP(it.value.deviceIP)
					child.syncBridgeIP(state.bridgeIP)
					log.info "Updated IP address $alias to $device.IP"
				}
			}
		} else {
			def hub = response.hubId
			def foundDevice = foundDevices."${foundDeviceMac}"
			foundDevice << ["hub": hub]
			devices << ["${foundDeviceMac}": foundDevice]
			log.info "Added potential device with MAC $foundDeviceMac"
		}
	}
}

//	----- PERIODIC CLOUD MX TASKS -----
def checkError()	{
	if (state.currentError == null || state.currentError == "none") {
		log.info "${appLabel()} did not find any errors."
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

// ----- Utility Functions -----------------------------------------
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

// ----- UPNP Search Target Definition -----------------------------
def getSearchTarget()	{
	def searchTarget = "upnp:rootdevice"
}

//	======== Other Application Values ==========================================================================================================================================================================
	def gitBranch()	{ return betaMarker() ? "beta" : "master" }
	def getAppImg(imgName, on = null)	{ return (!userSelectedAppIcons || on) ? "https://raw.githubusercontent.com/${gitPath()}/images/$imgName" : "" }
	def getWikiPageUrl()	{ return "https://github.com/${gitRepo()}/wiki" }
	def getIssuePageUrl()	{ return "https://github.com/${gitRepo()}/issues" }
	def strBrowserMode()	{ return (userSelectedBrowserMode) ? "embedded" : "external" }
	def driverNamespace()	{ return (userSelectedDriverNamespace) ? "DaveGut" : "ramiran2" }
	def gitRepo()		{ return "${appNamespace()}/${gitName()}" }
	def gitPath()		{ return "${gitRepo()}/${gitBranch()}"}
	def betaMarker()	{ return false }
	def sendingCommandSuccess()	{ return "Command Sent to SmartThings Application" }
	def sendingCommandFailed()	{ return "Ready to Send Command to SmartThings Application" }
	def tokenInfoOnline()	{ return "Online and Ready to Control Devices" }
	def tokenInfoOffline()	{ return "Offline, Please Fix to Restore Control on Devices" }
	def pageSelectorText()	{ return "Please tap below to continue" }
	def pageSelectorNullText()	{ return "Please select a option to continue" }
	def pageSelectorErrorText()	{ return "Please continue with caution, we have detected a error" }
	def textVersion()	{ return "Version: ${appVersion()}" }
	def textModified()	{ return "Updated: ${appVerDate()}" }
	def appVerInfo()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/changelog.txt", contentType: "text/plain; charset=UTF-8"], "changelog") }
	def textLicense()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/license.txt", contentType: "text/plain; charset=UTF-8"], "license") }
	def textSmartAppVersion()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/appversion.txt", contentType: "text/plain; charset=UTF-8"], "appversion") }
	def textDriverVersion()	{ return getWebData([uri: "https://raw.githubusercontent.com/${gitPath()}/data/driverversion.txt", contentType: "text/plain; charset=UTF-8"], "driverversion") }
	def textDonateLinkAntR()	{ return "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=S2CJBWCJEGVJA" }
	def linkGitHubDavG()	{ return "https://github.com/DaveGut/SmartThings_Cloud-Based_TP-Link-Plugs-Switches-Bulbs" }
	def linkGitHubAntR()	{ return "https://github.com/ramiran2/TP-Link-SmartThings" }
	def linkGitHubAntS()	{ return "https://github.com/tonesto7/nest-manager" }
	def linkYoutubeEE1()	{ return "https://www.youtube.com/watch?v=87JPlNk5ves&list=PL0S-Da7zGmE9PRn_YIitvUZEHYQglJw" }
	def linkYoutubeEE2()	{ return "https://www.youtube.com/watch?v=0eYTZrucx_o" }
	def linkYoutubeEE3()	{ return "https://www.youtube.com/watch?v=4_5kpOeiZyg&index=3&list=PL0S-Da7zGmE-i5MQdHORm6a" }
	def linkDiscord()	{ return "https://discord.gg/JDXeV23" }
	def linkXbox()	{ return "https://account.xbox.com/en-us/clubs/profile?clubid=3379843591790358" }
	def linkWaypoint()	{ return "https://www.halowaypoint.com/en-us/spartan-companies/xkiller%20clan" }
	def linkSteam()	{ return "https://steamcommunity.com/groups/xKillerClan" }
	def linkFacebook()	{ return "https://www.facebook.com/groups/xKillerClan/" }
	def textCopyright()	{ return "Copyright© 2018 - Dave Gutheinz, Anthony Ramirez" }
	def textDesc()	{ return "A Service Manager for the TP-Link Kasa Devices connecting through the TP-Link Servers to SmartThings." }
//	============================================================================================================================================================================================================