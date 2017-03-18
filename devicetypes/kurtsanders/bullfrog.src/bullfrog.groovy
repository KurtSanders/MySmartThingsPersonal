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

def redColor 		= "#FF0000"
def whiteColor 		= "#FFFFFF"
def grayColor		= "#808080"
def silverColor 	= "#C0C0C0"
def yellowColor 	= "#FFFF00"
def greenColor 		= "#008000"
def blueColor 		= "#0000FF"
def purpleColor 	= "#800080"
def fuchsiaColor 	= "#FF00FF"
def navyColor 		= "#000080"

metadata {
	definition (name: "bullfrog", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
		capability "Actuator"
		capability "Switch"
        capability "Refresh"
        capability "Temperature Measurement"
        capability "Sensor"
		capability "Notification"
        
		command "setHotTubStatus"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"main", type:"generic", width:6, height:4) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:greenColor		, nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:whiteColor	, nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:yellowColor	, nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:yellowColor, nextState:"turningOn"
			}
 			tileAttribute("statusText", key: "SECONDARY_CONTROL") {
				attributeState "statusText", label: '${currentValue}', backgroundColor:whiteColor, defaultState: true
			}         
		}

        valueTile("spaCurTemp", "spaCurTemp", width: 2, height: 2) {
            state("spaCurTemp", label:'${currentValue}°F',
                backgroundColors:[
                    [value: 50,  color: blueColor],
                    [value: 98,  color: navyColor],
                    [value: 99,  color: greenColor],
                    [value: 100, color: fuchsiaColor],
                    [value: 101, color: purpleColor],
                    [value: 102, color: yellowColor],
                    [value: 103, color: redColor]
                ]
            )
        }
        valueTile("spaSetTemp", "spaSetTemp", width: 2, height: 2) {
            state("spaSetTemp", label:'${currentValue}')
        }
 		standardTile("spaPump1", "spaPump1", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "Low", label: '${currentValue}', 
				icon: "st.valves.water.open", backgroundColor: yellowColor
			state "High", label: '${currentValue}', 
				icon: "st.valves.water.open", backgroundColor: greenColor
			state "Off", label: '${currentValue}', 
				icon: "st.valves.water.closed", backgroundColor: whiteColor
		}
 		standardTile("spaPump2", "spaPump2", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "Low", label: '${currentValue}', 
				icon: "st.valves.water.open", backgroundColor: yellowColor
			state "High", label: '${currentValue}', 
				icon: "st.valves.water.open", backgroundColor: greenColor
			state "Off", label: '${currentValue}', 
				icon: "st.valves.water.closed", backgroundColor: whiteColor
		}
 		standardTile("ledLights", "ledLights", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '${currentValue}', 
				icon: "st.Lighting.light11", backgroundColor: yellowColor
			state "Off", label: '${currentValue}', 
				icon: "st.Lighting.light11", backgroundColor: whiteColor
		}
 		standardTile("modeState", "modeState", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "Rest", label: '${currentValue}', 
				icon: "st.Kids.kids20", backgroundColor: fuchsiaColor
			state "Ready", label: '${currentValue}', 
				icon: "st.Kids.kids20", backgroundColor: greenColor
			state "Ready in Rest", label: '${currentValue}', 
				icon: "st.Kids.kids20", backgroundColor: yellowColor
			state "Off", label: '${currentValue}', 
				icon: "st.Kids.kids20", backgroundColor: whiteColor
		}
 		standardTile("heatMode", "heatMode", inactiveLabel: false,
			decoration: "flat", width: 2, height: 2,) {
			state "On", label: '', 
				icon: "st.thermostat.heat", backgroundColor: redColor
			state "Off", label: '', 
				icon: "st.thermostat.heating-cooling-off", backgroundColor: whiteColor
		}
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
		main(["main"])
		details(["main", "spaCurTemp", "modeState", "heatMode", "ledLights","spaPump1", "spaPump2", "spaSetTemp",  "refresh"])
	}
}

def installed() {
	log.debug "Installed: Begin..."
	log.debug "Installed: End..."
}

def parse(String description) {
	// This is a simulated device. No incoming data to parse.
	log.debug "parse: Begin..."
	log.debug "parse: End..."
}

def on() {
	log.trace "turningOn"
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.trace "turningOff"
	sendEvent(name: "switch", value: "off")
    return
}

def refresh() {
    log.trace("--- handler.refresh")
	return
}

def setHotTubStatus(params) {
    log.debug "params: ${params}"
    for ( e in params ) {
        log.debug "key = ${e.key}, value = ${e.value}"
        sendEvent(name: "${e.key}", value: "${e.value}")
    }
}

