/*
 * SanderSoft Hot Tub Service Manager for Balboa 20P WiFi Cloud Access Module
 * Tested on BullFrog Model A7L
 * 2017 (c) SanderSoft
 */

definition(
    name: 		"Hot Tub (Service Manager)",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com",
    description:"Hot Tub (Service Manager)",
    category: 	"My Apps",
    iconUrl: 	"https://s3.amazonaws.com/kurtsanders/MyHotTubSmall.png",
    iconX2Url: 	"https://s3.amazonaws.com/kurtsanders/MyHotTubLarge.png",
    iconX3Url: 	"https://s3.amazonaws.com/kurtsanders/MyHotTubLarge.png",
    singleInstance: true
)
// {
//    appSetting "IP"
//    appSetting "devID"
// }
import java.text.SimpleDateFormat;

preferences {
    page(name: "mainPage0")
    page(name: "mainPage1")
    page(name: "mainPage2")
    page(name: "mainPage3")
    page(name: "mainPage4")
}

def mainPage0() {
    dynamicPage(name: "mainPage0",
                title: "Hot Tub Setup Information",
                install: true,
                uninstall: true) {
        section ("Enter information in all the sections below") {
            href(name: "href",
                 title: "Network Information",
                 required: true,
                 page: "mainPage1")
            href(name: "href",
                 title: "Virtual Information",
                 required: true,
                 page: "mainPage2")
            href(name: "href",
                 title: "Polling Status Update Frequency",
                 required: true,
                 page: "mainPage3")
            href(name: "href",
                 title: "Notifications and Alerts",
                 required: false,
                 page: "mainPage4")
        }
    }
}

def mainPage1() {
    dynamicPage(name: "mainPage1",
                title: "Hot Tub Network Location Information",
                uninstall: true)
    {
        section ("Hot Tub Virtual device") {
            paragraph "${createChildDevice()}"
        }
        section ("Fully Qualified Domain Name (FQDN)") {
            input name: "hostName", type: "string",
                title: "Enter FQDN or IP4 address of where your Hot Tub resides on the Internet?",
                multiple: false,
                submitOnChange: true,
                capitalization: "none",
                required: true
            if (hostName != null) {
                paragraph "IP Address from FQDN: ${convertHostnameToIPAddress(hostName)}"
            }
            else {
                paragraph "Enter a Valid FQDN for Verification!"
            }
        }
    }
}
def mainPage2() {
    dynamicPage(name: "mainPage2",
                title: "My Hot Tub Virtual Device",
                uninstall: true)
    {
        section ("My BWA Hot Tub Virtual Device") {
            paragraph "Virtual Hot Tub Device Name."
            input "HotTub", "device.bwa",
                title: "Select Hot Tub Virtual Switch",
                multiple: false,
                required: true
        }
    }
}

def mainPage3() {
    dynamicPage(name: "mainPage3",
                title: "Hot Tub Status Update Frequency",
                uninstall: true )
    {
        section ("CloudControl Polling Interval") {
            input name: "schedulerFreq", type: "enum",
                title: "Run Refresh on a X Min Schedule?",
                options: [0,1,5,10,15,30],
                required: true
            mode(title: "Limit Polling Hot Tub to specific ST mode(s)",
                 image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png")
        }
    }
}
def mainPage4() {
    dynamicPage(name: "mainPage4",
                title: "Notifications and Alerts",
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
    unschedule(updateHotTubStatus)
    unsubscribe()
    removeChildDevices(getChildDevices())
}
def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    log.debug "initialize() Started"
    state.hostName = hostName
    subscribeToCommand(HotTub, "refresh", refresh)
    log.debug "Scheduled ${schedulerFreq} Min Refresh Cron"
    switch(schedulerFreq) {
        case '0':
        log.debug "UNScheduled all RunEvery"
        unschedule()
        case '1':
        runEvery1Minute(updateHotTubStatus)
        break
        case '5':
        runEvery5Minutes(updateHotTubStatus)
        break
        case '10':
        runEvery10Minutes(updateHotTubStatus)
        break
        case '15':
        runEvery15Minutes(updateHotTubStatus)
        break
        case '30':
        runEvery30Minutes(updateHotTubStatus)
        break
        default :
        log.debug "Unknown Schedule Frequency"
    }
    updateHotTubStatus()
    log.debug "initialize() Ended"
}


def refresh(evt) {
    log.debug("--- SmartApp handler.refresh")
    // get the Date this event happened at
    log.debug "Refresh event called at ${evt.date}"
    // Update Hot Tub State
    updateHotTubStatus()
    return
}

def updateHotTubStatus() {

    log.debug("--- handler.updateHotTubStatus")

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
    log.debug "Ended: updateHotTubStatus()"
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
            respParams <<  ["cloudConnected":"Offline"]
            HotTub.setHotTubStatus(respParams)
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
            HotTub.setHotTubStatus([
                "statusText": "${timeString}",
                "cloudConnected":"Online"
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
    log.info "spaCurTemp: ${spaCurTemp}"
    params << ["spaCurTemp": spaCurTemp]

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
    log.debug "ledState: ${B64decoded[offset]}"
    if (B64decoded[offset]>0) {
        log.info "LED On"
        params << ["ledLights": "On"]
    }
    else {
        log.info "LED Off"
        params << ["ledLights": "Off"]
    }

	// Hot Tub Set Temperature
    offset = 24
    log.debug "setSetTemp: ${B64decoded[offset]}"
    params << ["spaSetTemp": B64decoded[offset] + '°F\nSet Mode']

    // Send Update to Hot Tub Virtual Device
    log.debug "Sending Update to Virtual Hot Tub Device: ${params}"
    HotTub.setHotTubStatus(params)
}

def checkValidIpAddress(hostName) {
    if (convertHostnameToIPAddress(hostName) != null) {
        return true
    }
    else {return false}
}

def createChildDevice() {
    log.debug "createChildDevice"
    def deviceId = app.id + "bwaVdevice"
    log.debug "deviceId: ${deviceId}"
    log.debug "Before getChildDevices(): ${getChildDevices()}"
    def existing = getChildDevice(deviceId)
    if (!existing) {
        log.debug "Creating Child Device for My Hot Tub"
        def childDevice = addChildDevice("kurtsanders", "bwa", deviceId, null, ["label" : "My Hot Tub"])
        return "Virtual Device 'My Hot Tub' has been created"
    }
    else {
        log.debug "The Virtual Device ${getChildDevices()} already exists"
        return "Virtual Device 'My Hot Tub' already defined"
    }
}

def removeChildDevices(delete) {
    delete.each {
        log.debug "Before deleteChildDevice: getChildDevices: ${getChildDevices()}"
        deleteChildDevice(it.deviceNetworkId)
        log.debug "After deleteChildDevice: getChildDevices: ${getChildDevices()}"
    }
    return "All Child Devices have been removed"
}

def boolean isIP(String str)
{
    try
    {
         String[] parts = str.split("\\.");
         if (parts.length != 4) return false;
         for (int i = 0; i < 4; ++i)
         {
             int p = Integer.parseInt(parts[i]);
             if (p > 255 || p < 0) return false;
         }
         return true;
    } catch (Exception e)
    {
        return false;
    }
}