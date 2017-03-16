/* 
 *  Name:	Virtual Contact Sensor for My Bullfrog Hot Tub 
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
import java.text.SimpleDateFormat; 

metadata {
	definition (name: "bullfrog", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
		capability "Actuator"
		capability "Switch"
		capability "Switch Level"
        capability "Refresh"

		command "setMode"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"main", type:"generic", width:6, height:4) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#FF0000", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#0400ff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffff00", nextState:"turningOn"
			}
 			tileAttribute("device.statusText", key: "SECONDARY_CONTROL") {
				attributeState "statusText", label: '${currentValue}', backgroundColor:"#79b821", defaultState: true
			}         
		}

        valueTile("spaCurTemp", "device.spaCurTemp", width: 2, height: 2) {
            state("spaCurTemp", label:'${currentValue}°F',
                backgroundColors:[
                    [value: 50,  color: "#153591"],
                    [value: 98,  color: "#1e9cbb"],
                    [value: 99,  color: "#90d2a7"],
                    [value: 100, color: "#44b621"],
                    [value: 101, color: "#f1d801"],
                    [value: 102, color: "#d04e00"],
                    [value: 103, color: "#bc2323"]
                ]
            )
        }
        valueTile("spaSetTemp", "device.spaSetTemp", width: 2, height: 2) {
            state("spaSetTemp", label:'${currentValue}')
        }
 		standardTile("spaPump1", "device.spaPump1", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${currentValue}', 
				icon: "st.valves.water.open", backgroundColor: "#FF0000"
			state "Off", label: '${currentValue}', 
				icon: "st.valves.water.closed", backgroundColor: "#ffffff"
		}
 		standardTile("spaPump2", "device.spaPump2", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${currentValue}', 
				icon: "st.valves.water.open", backgroundColor: "#FF0000"
			state "Off", label: '${currentValue}', 
				icon: "st.valves.water.closed", backgroundColor: "#ffffff"
		}
 		standardTile("ledLights", "device.ledLights", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${currentValue}', 
				icon: "st.Lighting.light11", backgroundColor: "#FF0000"
			state "Off", label: '${currentValue}', 
				icon: "st.Lighting.light11", backgroundColor: "#ffffff"
		}
 		standardTile("modeState", "device.modeState", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "Rest", label: '${currentValue}', 
				icon: "st.thermostat.fan-circulate", backgroundColor: "#C1C2C3"
			state "Ready", label: '${currentValue}', 
				icon: "st.thermostat.fan-circulate", backgroundColor: "#008000"
			state "Off", label: '${currentValue}', 
				icon: "st.thermostat.fan-circulate", backgroundColor: "#0000FF"
		}
 		standardTile("heatMode", "device.heatMode", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${currentValue}', 
				icon: "st.thermostat.heat", backgroundColor: "#FF0000"
			state "Off", label: '${currentValue}', 
				icon: "st.thermostat.heat", backgroundColor: "#ffffff"
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
		main(["main"])
		details(["main", "spaSetTemp", "spaCurTemp", "modeState", "heatMode", "spaPump1", "spaPump2", "ledLights", "refresh"])
	}
}

def installed() {
	log.debug "Installed: Begin..."
	log.debug "Installed: End..."
}

def parse(String description) {
	// This is a simulated device. No incoming data to parse.
}

def setMode(state) {
	log.debug "setMode(${state}): Entering"
}

def on() {
	log.debug "turningOn"
	sendEvent(name: "switch", value: "on")
    updateBullfrogStatus()
}

def off() {
	log.debug "turningOff"
	sendEvent(name: "switch", value: "off")
    updateBullfrogStatus()
    return
}

def refresh() {
    log.trace("--- handler.refresh")
    updateBullfrogStatus()
	return
}

def updateBullfrogStatus() {
    def devID 	= "00000000-00000000-001527FF-FF09818B"
    def ip		= "174.101.169.159"
    def header = [
        'UserAgent': 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0', 
        'Cookie': 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000', 
        'Authorization': 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]

    def skip_getDevId = true

    if (skip_getDevId==false) {
        devID = getDevId(ip, header)
        if (devID == false) {
            return
        }
    }

	def byte[] B64decoded = getOnlineData(devID, header)
    log.debug "B64decoded from getOnlineData(): ${B64decoded}"
    
	if (B64decoded==false) {
		return
    }
	decodeBullfrogB64Data(B64decoded)
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
                return false 
            }
        }
    }    
    catch (Exception e) 
    {
        log.debug e
        return false
    }
    return devID
}

def byte[] getOnlineData(d, h) {
    log.debug "getOnlineData: Start"
    def devID 		= d
    def header 		= h
    def byte[] B64decoded
    Date now = new Date()
    def timeString = now.format("EEE MM/dd hh:mm:ss a", location.timeZone)
    log.debug "timeString: ${timeString}"

    def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
    def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${devID}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
    //    log.debug "Web_postdata: ${Web_postdata}"

    def params = [
        'uri'			: Web_idigi_post,
        'headers'		: header,
        'body'			: Web_postdata	
    ]
    log.debug "Start httpPost ============="
    try {
        httpPost(params) 
        { resp -> 
            if(resp.status == 200) {
                log.debug "HttpPost Request was OK"
                sendEvent(name: "statusText", value: "Bullfrog is Online at ${timeString}!")
                //                log.info "response data: ${resp.data}"
                def B64encoded = resp.data
//                def byte[] B64decoded = B64encoded.decodeBase64()
                B64decoded = B64encoded.decodeBase64()
                log.info "B64decoded: ${B64decoded}"
                //                    def hexstring = B64decoded.encodeHex()
                //                    log.info "hexstring: ${hexstring}"
            }
            else {
                log.error "HttpGet Request got http status ${resp.status}"
                sendEvent(name: "statusText", value: "Spa Error: Device Not Connected at ${timeString}.")
                return False
            }
        }
    }    
    catch (Exception e) 
    {
        log.debug e
        return False
    }
    log.debug "getOnlineData: End-> ${B64decoded}"
    return B64decoded
}

def decodeBullfrogB64Data(byte[] d) {
	log.debug "Entering decodeBullfrogB64Data(${d})"
	def byte[] B64decoded = d

    def offset = 9
    switch (B64decoded[offset]) {
        case 0:
        log.info "Mode: Ready"
        sendEvent(name: "modeState", value: 'Ready')
        break
        case 1:
        log.info "Mode: Rest"
        sendEvent(name: "modeState", value: 'Rest')
        break
        case 2:
        log.info "Mode: Ready in Rest"
        sendEvent(name: "modeState", value: 'Ready in Rest')
        break
        default :
        log.info "Mode: Unknown"
        sendEvent(name: "modeState", value: 'Unknown')					                        
    }

    offset = 12
    log.debug "setCurTemp: ${B64decoded[offset]}"
    sendEvent(name: "spaCurTemp", value: B64decoded[offset])

    offset = 17
    log.debug "heatMode: ${B64decoded[offset]}"
    if (B64decoded[offset]>0) { 
        log.info "Heat On"
        sendEvent(name: "heatMode", value: 'On')
    }
    else {
        log.info "Heat Off"
        sendEvent(name: "heatMode", value: 'Off')
    }

    offset = 18
    log.debug "ledState: ${B64decoded[offset]}"
    if (B64decoded[offset]>0) { 
        log.info "LED On"
        sendEvent(name: "ledLights", value: 'On')
    }
    else {
        log.info "LED Off"
        sendEvent(name: "ledLights", value: 'Off')
    }

    offset = 24
    log.debug "setSetTemp: ${B64decoded[offset]}"
    sendEvent(name: "spaSetTemp", value: B64decoded[offset] + '°F\nSet Mode')

}
