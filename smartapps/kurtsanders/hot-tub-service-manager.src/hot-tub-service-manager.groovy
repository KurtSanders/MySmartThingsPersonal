/*
* SanderSoft Hot Tub Service Manager for Balboa 20P WiFi Cloud Access Module
* Tested on BullFrog Model A7L
* 2017 (c) SanderSoft
*  Author: Kurt Sanders
*  Email:	Kurt@KurtSanders.com
*  Date:	3/2017
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/

definition(
    name: 		"Hot Tub (Service Manager)",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com",
    description:"Hot Tub (Service Manager)",
    category: 	"My Apps",
    iconUrl: 	"https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/smartapps/kurtsanders/hot-tub-service-manager.src/MyHotTubSmall.png",
    iconX2Url: 	"https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/smartapps/kurtsanders/hot-tub-service-manager.src/MyHotTubLarge.png",
    iconX3Url: 	"https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/smartapps/kurtsanders/hot-tub-service-manager.src/MyHotTubLarge.png",
    singleInstance: true
)
// {
//		Not Used in this version
//    	appSetting "IP"
//    	appSetting "devID"
// }
import java.text.SimpleDateFormat;

preferences {
    page(name:"mainNetwork")
    page(name:"mainDevice")
    page(name:"mainSchedule")
    page(name:"mainNotifications")
}

def mainNetwork() {
    dynamicPage(name: "mainNetwork",
                title: "Hot Tub Network Location Information",
                nextPage: "mainDevice",
                uninstall: true)
    {
        section ("Hot Tub WiFi Information") {
            input name: "hostName", type: "enum",
                title: "Select the FQDN or public IP Address of your network?",
                options: ["kurtsanders.mynetgear.com"],
                capitalization: "none",
                multiple: false,
                required: true
        }
    }
}

def mainDevice() {
    dynamicPage(name: "mainDevice",
                title: "My Hot Tub Virtual Device",
                nextPage: "mainSchedule",
                uninstall: true)
    {
        if (state.devID==null) {
            getHotTubDeviceID(hostName)
        }
        section("Hot Tub Virtual Device") {
            paragraph "Hot Tub WiFi Module DevID: ${state.devID}"
            input "HotTubDevice", "capability.switch",
                title: "Select Hot Tub Device",
                multiple: false,
                required: true
        }
    }
}

def mainSchedule() {
    dynamicPage(name: "mainSchedule",
                title: "Hot Tub Status Update Frequency",
                nextPage: "mainNotifications",
                uninstall: true)
    {
        section("Hot Tub Polling Interval") {
            input name: "schedulerFreq", type: "enum",
                title: "Run Refresh on a X Min Schedule?",
                options: ["Off",1,5,10,15,30],
                required: true
            mode(title: "Limit Polling Hot Tub to specific ST mode(s)",
                 image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png")
        }
    }
}
def mainNotifications() {
    dynamicPage(name: "mainNotifications",
                title: "Notifications and Alerts",
                install: true,
                uninstall: true)
    {
        section("Send Notifications?") {
            paragraph "Alerts"
            input("recipients", "contact", title: "Send notifications to") {
                input "phone", "phone", title: "Warn with text message (optional)",
                    description: "Phone Number", required: false
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(app, appHandler)
    subscribe(HotTubDevice, "switch", appHandler)
    subscribeToCommand(HotTubDevice, "refresh", appHandler)
    state.hotTubMap= [
    "contact"					:null,
    "switch"					:null,
    "schedule"					:null,
    "light"						:null,
    "spaPump1"					:null,
    "spaPump2"					:null,
    "heatingSetpoint"			:null,
    "modeState"					:null,
    "temperature"				:null,
    "thermostatOperatingState"	:null,
    "thermostatMode"			:null,
    "statusText"				:null
]
    log.debug "state.hotTubMap: ${state.hotTubMap}"

}
def uninstalled() {
    log.debug "uninstalled:------- Started"
    log.debug "uninstalled:------- Ended"
}
def updated() {
    log.debug "updated:------- Started"
    unsubscribe()
    installed()
    setScheduler(schedulerFreq)
    log.debug "updated:------- Ended"
}

def appHandler(evt) {
    log.debug("SmartApp Apphandler----- Started")
    log.debug "ST app event ${evt.name}:${evt.value} received"
    updateHotTubStatus()
    setScheduler(schedulerFreq)
    log.debug("SmartApp Apphandler----- Ended")
}

def updateHotTubStatus() {
    log.debug("handler.updateHotTubStatus----Started")

// Get array values from cloud for Hot Tub Status
	def byte[] B64decoded = null
    if (!B64decoded) {
        for (int i = 1; i < 4; i++) {
            log.debug "getOnlineData: ${i} attempt..."
            B64decoded = getOnlineData()
            if (B64decoded!=null) {break}
        }
    }
    if (B64decoded==null) {
    	log.error "getOnlineData: returned Null:  Exiting..."
        updateDeviceStates()
    	return
    }
    // Decode the array status values into operational statuses
    decodeHotTubB64Data(B64decoded)

// Send Update to Hot Tub Virtual Device
    updateDeviceStates()
    log.debug("handler.updateHotTubStatus----Ended")
}

private String convertHostnameToIPAddress(hostname) {
    def params = [
        uri: "http://dns.google.com/resolve?name=" + hostname,
        contentType: 'application/json'
    ]
    def retVal = null
    try {
        retVal = httpGet(params) { response ->
            // log.debug "Request was successful, data=$response.data, status=$response.status"
            // log.debug "Result Status : ${response.data?.Status}"
            if (response.data?.Status == 0) { // Success
                for (answer in response.data?.Answer) { // Loop through results looking for the first IP address returned otherwise it's redirects
                    // log.debug "Processing response: ${answer}"
                    log.info "Hostname ${answer?.name} has IP Address of '${answer?.data}'"
                    return answer?.data
                }
            } else {
                log.warn "DNS unable to resolve hostname ${response.data?.Question[0]?.name}, Error: ${response.data?.Comment}"
            }
        }
    } catch (Exception e) {
        log.warn("Unable to convert hostname to IP Address, Error: $e")
    }

    //log.trace "Returning IP $retVal for Hostname $hostname"
    return retVal
}

def getDevId() {
    log.debug "getOnlineStatus(): Begin-----------"
    def devID = ""
    state.header = [
        'UserAgent': 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0',
        'Cookie': 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000',
        'Authorization': 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
    def url   	= "https://my.idigi.com/ws/DeviceCore/.json?condition=dpGlobalIp='" + state.ipAddress + "'"
    def params = [
        'uri'			: url,
        'headers'		: state.header,
        'contentType'	: 'application/json'
    ]
    log.debug "Start httpGet ============="
    try {
        httpGet(params)
        { resp ->
            // log.debug "response data: ${resp.data}"
            devID = resp.data.items.devConnectwareId?.get(0)
            log.info "devID = ${devID}"
            if(resp.status == 200) {
                log.debug "HttpGet Request was OK"
            }
            else {
                log.error "HttpGet Request got http status ${resp.status}"
                return null
            }
        }
    }
    catch (Exception e)
    {
        log.debug e
        return null
    }
    state.devID = devID
    log.debug "getOnlineStatus(): End----------"

    return
}

def byte[] getOnlineData() {
    log.debug "getOnlineData: Start"
    def httpPostStatus = resp
    def byte[] B64decoded
    Date now = new Date()
    def timeString = now.format("EEE MM/dd h:mm:ss a", location.timeZone)
    def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
    def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${state.devID}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
	def respParams = [:]
    def params = [
        'uri'			: Web_idigi_post,
        'headers'		: state.header,
        'body'			: Web_postdata
    ]
    log.debug "Start httpPost ============="
    try {
        httpPost(params) {
            resp ->
            log.debug "httpPost resp.status: ${resp.status}"
            httpPostStatus = resp
        }
    }
    catch (Exception e)
    {
        log.debug "Catch HttpPost Error: ${e}"
        return null
    }
    if (httpPostStatus==null) {
        return null
    }
    def resp = httpPostStatus
    if(resp.status == 200) {
        log.debug "HttpPost Request was OK ${resp.status}"
        if(resp.data == "Device Not Connected") {
            log.error "HttpPost Request: ${resp.data}"
            unschedule()
            state.hotTubMap.statusText 		= "Hot Tub Fatal Error\n${resp.data}\n${timeString}"
            state.hotTubMap.contact 		= "open"
            state.hotTubMap.schedule		= "0"
            updateDeviceStates()
            def message = "Hot Tub Error: ${resp.data}! at ${timeString}."
            if (location.contactBookEnabled && recipients) {
                log.debug "${message}"
                sendNotificationToContacts(message, recipients)
            }
            else {
                log.debug "Contact book not enabled"
                if (phone) {
                    sendSms(phone, message)
                }
            }
            return null
        }
        else {
            // log.info "response data: ${resp.data}"
            state.hotTubMap.statusText	= "${timeString}"
            state.hotTubMap.contact		= "closed"
            def B64encoded = resp.data
            B64decoded = B64encoded.decodeBase64()
            log.info "B64decoded: ${B64decoded}"
            // def byte[] B64decoded = B64encoded.decodeBase64()
            // def hexstring = B64decoded.encodeHex()
            // log.info "hexstring: ${hexstring}"
        }
    }
    else {
        log.error "HttpPost Request got http status ${resp.status}"
        state.hotTubMap.statusText	= "Hot Tub Fatal Error\nHttp Status ${resp.status} at ${timeString}."
        return null
    }
    log.debug "getOnlineData: End"
    return B64decoded
}

def decodeHotTubB64Data(byte[] d) {
    log.debug "Entering decodeHotTubB64Data"
    def byte[] B64decoded = d
    def params = [:]
    def offset = 0

    //	Hot Tub Current Temperature ( <0 is Unavailable )
    offset = 6
    def spaCurTemp = B64decoded[offset]
    if (spaCurTemp < 0) {
        spaCurTemp = "--"
    }
    state.hotTubMap.temperature	= "${spaCurTemp}"

    //  Hot Tub Mode State
    offset = 9
    def modeStateDecodeArray = ["Ready","Rest","Ready/Rest"]
	state.hotTubMap.modeState = modeStateDecodeArray[B64decoded[offset]]
    //	Hot Tub Pump1 and Pump2 Status
    offset = 15
    def pumpDecodeArray = []
    state.hotTubMap.switch = "on"
    switch (B64decoded[offset]) {
        case 0:
        log.info "Pump1: Off, Pump2: Off"
        pumpDecodeArray=["Off","Off"]
        state.hotTubMap.switch = "off"
        break
        case 1:
        log.info "Pump1: Low, Pump2: Off"
        pumpDecodeArray=["Low","Off"]
        break
        case 2:
        log.info "Pump1: High, Pump2: Off"
        pumpDecodeArray=["High","Off"]
        break
        case 4:
        log.info "Pump1: Off, Pump2: Low"
        pumpDecodeArray=["Off","Low"]
        break
        case 5:
        log.info "Pump1: Low, Pump2: Low"
        pumpDecodeArray=["Low","Low"]
        break
        case 6:
        log.info "Pump1: High, Pump2: Low"
        pumpDecodeArray=["High","Low"]
        break
        case 8:
        log.info "Pump1: Off, Pump2: High"
        pumpDecodeArray=["Off","High"]
        break
        case 9:
        log.info "Pump1: Low, Pump2: High"
        pumpDecodeArray=["Low","High"]
        break
        case 10:
        log.info "Pump1: High, Pump2: High"
        pumpDecodeArray=["High","High"]
        break
        default :
        log.info "Pump Mode: Unknown"
        pumpDecodeArray=["Off","Off"]
        state.hotTubMap.switch = "off"
    }
    state.hotTubMap.spaPump1 = pumpDecodeArray[0]
    state.hotTubMap.spaPump2 = pumpDecodeArray[1]

    //	Hot Tub Heat Mode
    offset = 17
    if (B64decoded[offset]>0) {
        state.hotTubMap.thermostatOperatingState = "heating"
        state.hotTubMap.thermostatMode = "heat"
    }
    else {
        state.hotTubMap.thermostatOperatingState = "idle"
        state.hotTubMap.thermostatMode = "off"
}

//	Hot Tub LED Lights
    offset = 18
    if (B64decoded[offset]>0) {
        log.info "LED On"
        state.hotTubMap.light = "on"
    }
    else {
        state.hotTubMap.light = "off"
    }

	// Hot Tub Set Temperature
    offset = 24
    // params << ["heatingSetpoint": B64decoded[offset] + '°F\nSet Mode']
    state.hotTubMap.heatingSetpoint = B64decoded[offset].toInteger()
}

def setScheduler(schedulerFreq) {
    state.hotTubMap.schedule = "${schedulerFreq}"
    switch(schedulerFreq) {
        case 'Off':
        log.debug "UNScheduled all RunEvery"
        unschedule()
        break
        case '1':
        log.debug "Scheduled RunEvery${schedulerFreq}Minute"
        runEvery1Minute(updateHotTubStatus)
        break
        case '5':
        log.debug "Scheduled RunEvery${schedulerFreq}Minute"
        runEvery5Minutes(updateHotTubStatus)
        break
        case '10':
        log.debug "Scheduled RunEvery${schedulerFreq}Minute"
        runEvery10Minutes(updateHotTubStatus)
        break
        case '15':
        log.debug "Scheduled RunEvery${schedulerFreq}Minute"
        runEvery15Minutes(updateHotTubStatus)
        break
        case '30':
        log.debug "Scheduled RunEvery${schedulerFreq}Minute"
        runEvery30Minutes(updateHotTubStatus)
        break
        default :
        log.debug "Unknown Schedule Frequency"
        unschedule()
    }
}

def boolean isIP(String str)
{
    try {
        String[] parts = str.split("\\.");
        if (parts.length != 4) return false;
        for (int i = 0; i < 4; ++i)
        {
            int p = Integer.parseInt(parts[i]);
            if (p > 255 || p < 0) return false;
        }
        return true;
    } catch (Exception e){return false}
}

def getHotTubDeviceID(hostName) {
    def boolean isIPbool = isIP(hostName)
    log.info "isIPbool: ${isIPbool}"
    if(isIPbool){
        log.debug "Valid IP4: ${hostName}"
        state.ipAddress = hostName
    }
    else {
        def dns2ipAddress = convertHostnameToIPAddress(hostName)
        if (dns2ipAddress != null) {
            log.debug "Valid IP4: ${dns2ipAddress}"
            state.ipAddress = dns2ipAddress
        }
    }
    if (state.ipAddress!=null) {
        getDevId()
        log.debug "state.devID: ${state.devID}"
    }
}

def updateDeviceStates() {
    log.debug "Start: updateDeviceStates-------------"
    log.debug "Sending Update to Virtual Hot Tub Device:\n${state.hotTubMap}"
    HotTubDevice.setHotTubStatus(state.hotTubMap)

    log.debug "End: updateDeviceStates-------------"
}