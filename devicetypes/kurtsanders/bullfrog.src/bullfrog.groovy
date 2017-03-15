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
 
def devID_old = "00000000-00000000-001527FF-FF09818B"

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
				attributeState "statusText", label: 'Spa Error: Device Not Connected', backgroundColor:"#79b821", defaultState: true
			}         
		}

        valueTile("spaTemp", "device.spaTemp", width: 2, height: 2) {
            state("spaTemp", label:'${currentValue}°F',
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
 		standardTile("spaPump1", "device.spaPump1", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${name}', 
				icon: "st.valves.water.open", backgroundColor: "#FF0000"
			state "Off", label: '${name}', 
				icon: "st.valves.water.closed", backgroundColor: "#ffffff"
		}
 		standardTile("spaPump2", "device.spaPump2", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${name}', 
				icon: "st.valves.water.open", backgroundColor: "#FF0000"
			state "Off", label: '${name}', 
				icon: "st.valves.water.closed", backgroundColor: "#ffffff"
		}
 		standardTile("ledLights", "device.ledLights", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${name}', 
				icon: "st.Lighting.light11", backgroundColor: "#FF0000"
			state "Off", label: '${name}', 
				icon: "st.Lighting.light11", backgroundColor: "#ffffff"
		}
 		standardTile("modeState", "device.modeState", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "Rest", label: '${name}', 
				icon: "st.thermostat.fan-circulate", backgroundColor: "#C1C2C3"
			state "Ready", label: '${name}', 
				icon: "st.thermostat.fan-circulate", backgroundColor: "#008000"
			state "Off", label: '${name}', 
				icon: "st.thermostat.fan-circulate", backgroundColor: "#0000FF"
		}
 		standardTile("heatMode", "device.heatMode", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${name}', 
				icon: "st.thermostat.heat", backgroundColor: "#FF0000"
			state "Off", label: '${name}', 
				icon: "st.thermostat.heat", backgroundColor: "#ffffff"
		}
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
		main(["main"])
		details(["main", "spaTemp", "spaPump1", "spaPump2", "ledLights", "modeState", "heatMode", "refresh"])
	}
}

def installed() {
	log.debug "Installed: Begin..."
    /*
	sendEvent(name: "device.spaTemp"   , value: 100)
	sendEvent(name: "device.statusText", value: "Connected.")
	sendEvent(name: "device.spaPump1"	, value: "Off")
	sendEvent(name: "device.spaPump2"	, value: "Off")
	sendEvent(name: "device.ledLights"	, value: "Off")
	sendEvent(name: "device.modeState"	, value: "Ready")
    sendEvent(name: "device.heatMode"	, value: "On")
    */
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
    getStatus()
}

def off() {
	log.debug "turningOff"
	sendEvent(name: "switch", value: "off")
    getStatus()
}

def refresh() {
    log.trace("--- handler.refresh")
    getStatus()
	return
}

def getStatus() {
	log.debug "getStatus(): Begin"
    def devID = " "
    def url   = "https://my.idigi.com/ws/DeviceCore/.json?condition=dpGlobalIp='174.101.169.159'"
    def header = [
    'UserAgent': 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0', 
    'Cookie': 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000', 
    'Authorization': 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
    def params = [
        'uri'			: url,
        'headers'		: header,
        'contentType'	: 'application/json'
    ]
        log.debug "Start httpGet ============="
        try {
            httpGet(params) 
            { resp -> 
//            log.info "response data: ${resp.data}"
        	devID = resp.data.items.devConnectwareId?.get(0)
            log.info "devID = ${devID}"
			if(resp.status == 200) {
                log.debug "HttpGet Request was OK"
                }
            else {
                log.error "HttpGet Request got http status ${resp.status}"
                return 
        		}
            }
        }    
        catch (Exception e) 
        {
            log.debug e
        }

		log.debug "Web_postdata: Start"
        def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
        def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
        <targets><device id="' + "${devID}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
        <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
        
        params = [
            'uri'			: Web_idigi_post,
            'headers'		: header,
			'body'			: Web_postdata	
			]
            log.debug "Start httpGet ============="
            try {
                httpPost(params) 
                { resp -> 
				if(resp.status == 200) {
                    log.debug "HttpGet Request was OK"
                    sendEvent(name: "device.statusText", value: "Connected.")
                    log.info "response data: ${resp.data}"
                    def B64encoded = resp.data
                    def byte[] B64decoded = B64encoded.decodeBase64()
                    log.info "decoded: ${B64decoded}"
                    def hexstring = B64decoded.encodeHex()
                    log.info "hexstring: ${hexstring}"
					def setTemp 	= B64decoded.getAt(24)
					def ledState 	= B64decoded.getAt(18)
                    if (B64decoded[18]>0) { 
                    	log.info "LED On"
                    }
                    else {
                    	log.info "LED Off"
                    }
					log.debug "setTemp: ${setTemp}"
					log.debug "ledState: ${ledState}"
//                    log.info "Set Temp: ${hexstring.get(24)}"
                    }
                else {
                    log.error "HttpGet Request got http status ${resp.status}"
                    sendEvent(name: "device.statusText", value: "Spa Error: Device Not Connected.")
                    return 
                    }
                }
            }    
            catch (Exception e) 
            {
                log.debug e
            }
        log.debug "getStatus(): End"
        
	}
    
