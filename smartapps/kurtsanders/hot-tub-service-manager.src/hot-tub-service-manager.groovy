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
//    appSetting "IP"
//    appSetting "devID"
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
        createChildDevice()
        section ("Fully Qualified Domain Name (FQDN)") {
            input name: "hostName", type: "string",
                title: "Enter FQDN or IP4 address of where your Hot Tub resides on the Internet?",
                multiple: false,
                required: true
        }
    }
}

def mainDevice() {
    dynamicPage(name: "mainDevice",
                title: "My Hot Tub Virtual Device",
                nextPage: "mainSchedule")
    {
        section("My BWA Hot Tub Virtual Device") {
            if (hostName != null) {
                paragraph "IP Address from FQDN: ${convertHostnameToIPAddress(hostName)}"
            }
            else {
                paragraph "Please Return to enter a Valid FQDN for Verification!"
            }
            paragraph "Virtual Hot Tub Device Name."
            input "HotTub", "device.bwa",
                title: "Select Hot Tub Virtual Switch",
                multiple: false,
                required: true
        }
    }
}

def mainSchedule() {
    dynamicPage(name: "mainSchedule",
                title: "Hot Tub Status Update Frequency",
                nextPage: "mainNotifications")
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
    initialize()
}
def uninstalled() {
    unschedule()
    unsubscribe()
    removeChildDevices(getChildDevices())
}
def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initialize:------- Started"
    state.hostName = hostName
	setScheduler(schedulerFreq)
    subscribe(HotTub, "switch", appHandler)
    subscribe(HotTub, "refresh", appHandler)
    subscribe(app, STrefresh)
    subscribeToCommand(HotTub, "refresh", appHandler)
    //	updateHotTubStatus()
    log.debug "initialize-------- Ended"
}

def STrefresh(evt) {
    log.debug("SmartApp handler.STrefresh----- Started")
    updateHotTubStatus()
    log.debug("SmartApp handler.STrefresh----- Ended")
}
def appHandler(evt) {
    log.debug("SmartApp Apphandler----- Started")
    log.debug "app event ${evt.name}:${evt.value} received"
    updateHotTubStatus()
//    log.debug "Test: ${HotTub.currentSwitch}"
    log.debug("SmartApp Apphandler----- Ended")
}

def updateHotTubStatus() {
    log.debug("handler.updateHotTubStatus----Started")

// Define HTTP Header for Access
    def header = [
        'UserAgent': 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0',
        'Cookie': 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000',
        'Authorization': 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
    // Get IP Address of the HotTub WiFi Unit
    if (!state.ipAddress) {
        state.ipAddress 	= convertHostnameToIPAddress(hostName)
        if (!state.ipAddress) {
            log.error "convertHostnameToIPAddress(hostName) returned Null"
            return
        }
        else {
            log.info "Hot Tub ipAddres: ${state.ipAddress}"
        }
    }
    else {
        log.debug "Skipping IPAddress(): Previously defined as constant: ${state.ipAddress}"
    }


    // Get WiFi Module Device ID using ipAddress (Skip if already obtained/defined above)
    if (!state.devID) {
        state.devID = "00000000-00000000-001527FF-FF09818B"
        if (!state.devID) {
            state.devID = getDevId(state.ipAddress, header)
            if (!state.devID) {
                log.error "getDevId(ipAddress, header) returned Null"
                return
            }
        }
    }
    else {
        log.debug "Skipping getDevID: Previously defined as constant: ${state.devID}"
    }

// Get array values from cloud for Hot Tub Status
	def byte[] B64decoded = getOnlineData(state.devID, header)
//    log.debug "B64decoded returned from getOnlineData(): ${B64decoded}"
    if (!B64decoded) {
        for (int i = 0; i < 3; i++) {
            log.debug "${i} Trying B64decoded..."
            B64decoded = getOnlineData(state.devID, header)
            if (!B64decoded) {
                break
            }
        }
    }
    if (!B64decoded) {
    	log.error "getOnlineData(devID, header) returned Null:  Exiting..."
    	return
    }
    // Decode the array status values into operational statuses
    decodeHotTubB64Data(B64decoded)

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

def getDevId(p,h) {
    log.debug "getOnlineStatus(): Begin"
    def devID = ""
    def ip 		= p
    def header	= h
    def url   	= "https://my.idigi.com/ws/DeviceCore/.json?condition=dpGlobalIp='" + ip + "'"
    def params = [
        'uri'			: url,
        'headers'		: header,
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
    return devID
}

def byte[] getOnlineData(d, h) {
    log.debug "getOnlineData: Start"
    def devID 		= d
    def header 		= h
    def httpPostStatus = resp
    def byte[] B64decoded
    Date now = new Date()
    def timeString = now.format("EEE MM/dd h:mm:ss a", location.timeZone)

    def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
    def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${devID}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
	def respParams = [:]
    def params = [
        'uri'			: Web_idigi_post,
        'headers'		: header,
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
            respParams <<  ["statusText": "Hot Tub Fatal Error\n${resp.data}\n${timeString}"]
            respParams <<  ["contact":"open"]
            updateDeviceStates(respParams)
            unschedule()
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
            updateDeviceStates([
                "statusText": "${timeString}",
                "contact":"closed"
            ])

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
        HotTub.setHotTubStatus("statusText":"Hot Tub Fatal Error\nHttp Status ${resp.status} at ${timeString}.")
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
    log.info "temperature: ${spaCurTemp}"
    params << ["temperature": spaCurTemp]

    //  Hot Tub Mode State
    offset = 9
    def modeStateDecodeArray = ["Ready","Rest","Ready/Rest"]
    params << ["modeState": modeStateDecodeArray[B64decoded[offset]]]

    //	Hot Tub Pump1 and Pump2 Status
    offset = 15
    def pumpDecodeArray = []
    switch (B64decoded[offset]) {
        case 0:
        log.info "Pump1: Off, Pump2: Off"
        pumpDecodeArray=["Off","Off"]
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
    }
    params << ["spaPump1": pumpDecodeArray[0]]
    params << ["spaPump2": pumpDecodeArray[1]]

//  Hot Tub Switch
    if (pumpDecodeArray==["Off","Off"]) {
        if (HotTub.currentSwitch == "on") {
            log.debug "HotTub Switch: Jets Off: Switch: Off"
            HotTub.off()
        }
    }
    else {
        log.debug "HotTub Switch: Jets On: Switch: On"
        if (pumpDecodeArray==["Off","Off"]) {
            HotTub.on()
        }
    }

    //	Hot Tub Heat Mode
    offset = 17
    log.debug "heatMode: ${B64decoded[offset]}"
    if (B64decoded[offset]>0) {
        log.info "Heat On"
        params << ["heatMode": "On"]
    }
    else {
        log.info "Heat Off"
        params << ["heatMode": "Off"]
    }

//	Hot Tub LED Lights
    offset = 18
    log.debug "LED light: ${B64decoded[offset]}"
    if (B64decoded[offset]>0) {
        log.info "LED On"
        params << ["light": "on"]
    }
    else {
        log.info "LED Off"
        params << ["light": "off"]
    }

	// Hot Tub Set Temperature
    offset = 24
    log.debug "setSetTemp: ${B64decoded[offset]}"
    params << ["spaSetTemp": B64decoded[offset] + '°F\nSet Mode']
    params << ["spaSetTemp": B64decoded[offset].toInteger()]

    // Send Update to Hot Tub Virtual Device
    log.debug "Sending Update to Virtual Hot Tub Device: ${params}"
    updateDeviceStates(params)
}

def updateDeviceStates(params) {
    HotTub.setHotTubStatus(params)
}

def checkValidIpAddress(hostName) {
    if (convertHostnameToIPAddress(hostName) != null) {
        return true
    }
    else {
        return false
    }
}

def createChildDevice() {
    log.debug "createChildDevice--------Started"
    def deviceId = app.id + "bwaVdevice"
    log.debug "deviceId: ${deviceId}"
    def existing = getChildDevice(deviceId)
    if (!existing) {
        def childDevice = addChildDevice("kurtsanders", "bwa", deviceId, null, ["label" : "My Hot Tub"])
    }
    def children = app.getChildDevices()
    children.each { child ->
        log.debug "child device id $child.id with label $child.label"
    }
    log.debug "createChildDevice--------Ended"
}

def removeChildDevices(delete) {
    delete.each {
        log.debug "Before deleteChildDevice: getChildDevices: ${getChildDevices()}"
        deleteChildDevice(it.deviceNetworkId)
        log.debug "After deleteChildDevice: getChildDevices: ${getChildDevices()}"
    }
}

def setScheduler(schedulerFreq) {
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