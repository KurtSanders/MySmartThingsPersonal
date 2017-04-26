/*
*  Name:	BWA Hot Tub Virtual Device Handler for Balboa 20P WiFi Module
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
def redColor4		= "#ffCCCC"

def greenColor 		= "#008000"
def whiteColor 		= "#FFFFFF"
def yellowColor 	= "#FFFF00"
def blueColor 		= "#0000FF"
def navyColor 		= "#000080"
def blackColor		= "#000000"

metadata {
    definition (name: "bwa", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Switch"
        capability "Sensor"
        capability "Light"
        capability "Outlet"
        capability "Contact Sensor"
        capability "Temperature Measurement"
        capability "Thermostat"
        capability "Refresh"

/*
        capability "Actuator"
        attribute "spaSetTemp"  ,   "string"
        attribute "heatMode"  	,  	"enum"	, ["Off", "On"]
        attribute "spaPump1"  	,  	"enum"	, ["Off", "Low", "High"]
        attribute "spaPump2"	,  	"enum"	, ["Off", "Low", "High"]
        attribute "modeState"	, 	"enum"	, ["Rest", "Ready", "Rest/Ready", "Off"]
*/
        command "setHotTubStatus"
        command "slider"
    }
    tiles(scale: 2) {
        // Hot Tub Turn On/Off
        standardTile("switch", "device.switch",  width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:greenColor	, nextState:"turningOff"
            state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:whiteColor	, nextState:"turningOn"
            state "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:yellowColor	, nextState:"turningOff"
            state "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:yellowColor , nextState:"turningOn"
        }

        // Current Temperature Reading
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label:'${currentValue}°F', unit:"°F",
                  backgroundColors:[
                      [value: 0,  color: whiteColor],
                      [value: 50,  color: navyColor],
                      [value: 95,  color: blueColor],
                      [value: 96, color: redColor4],
                      [value: 104, color: redColor]
                  ]
                 )
        }
        // LED Lights
        standardTile("light", "device.light",  width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'Lights ${name}', icon:"st.Lighting.light11", backgroundColor:greenColor	, nextState:"turningOff"
            state "off", label:'Lights ${name}', icon:"st.Lighting.light13", backgroundColor:whiteColor	, nextState:"turningOn"
            state "turningOn", label:'${name}', icon:"st.Lighting.light11", backgroundColor:yellowColor	, nextState:"turningOff"
            state "turningOff", label:'${name}', icon:"st.Lighting.light13", backgroundColor:yellowColor , nextState:"turningOn"
        }
        // Network Connected Status
        standardTile("contact", "device.contact",  width: 2, height: 2, decoration: "flat") {
            state "open",   label:'Offline', action:"open",
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/offline.png",
                backgroundColor:yellowColor
            state "closed", label:'Online', action:"closed",
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/online.png",
                backgroundColor:"#18BA02"
        }
        // Descriptive Text
        valueTile("statusText", "statusText", width: 2, height: 2) {
            state "statusText", label: '${currentValue}', backgroundColor:whiteColor, defaultState: true
        }
        valueTile("schedule", "schedule", width: 2, height: 2) {
            state "schedule", label: 'Refresh\nSchedule\n${currentValue} min(s)', backgroundColor:whiteColor, defaultState: true
        }
        standardTile("spaPump1", "spaPump1", inactiveLabel: false, decoration: "flat", width: 2, height: 2,) {
            state "Low", label: 'Jet1 Low',
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet1 High',
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "Off", label: 'Jet1 Off',
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        standardTile("spaPump2", "spaPump2", inactiveLabel: false, decoration: "flat", width: 2, height: 2,) {
            state "Low", label: 'Jet2 Low',
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet2 High',
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "Off", label: 'Jet2 Off',
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        standardTile("modeState", "modeState", inactiveLabel: false,
                     decoration: "flat", width: 2, height: 2,) {
            state "Rest", label: '${currentValue}',
                icon: "st.Kids.kids20", backgroundColor: whiteColor
            state "Ready", label: '${currentValue}',
                icon: "st.Kids.kids20", backgroundColor: greenColor
            state "Ready/Rest", label: '${currentValue}',
                icon: "st.Kids.kids20", backgroundColor: greenColor
            state "Off", label: '${currentValue}',
                icon: "st.Kids.kids20", backgroundColor: whiteColor
        }
        valueTile("heatingSetpoint", "device.heatingSetpoint", width: 2, height: 2) {
            state("heatingSetpoint", label:'Set Temp\n${currentValue}°F')
        }
        standardTile("thermostatOperatingState", "device.thermostatOperatingState", decoration: "flat", width: 2, height: 2) {
            state "idle", label:'${name}',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/idle.png"
            state "heating", label:'${name}',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/heating.png"
        }
        standardTile("thermostatMode", "device.thermostatMode", decoration: "flat", width: 2, height: 2,) {
            state "off",  label: 'Heat Off', icon: "st.Outdoor.outdoor19"
            state "heat", label: 'Heat On',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/heatMode.png"
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        controlTile("levelSliderControl", "device.level", "slider", height: 2,
                    width: 4, inactiveLabel: false, range:"(0..10)") {
            state "level", action:"slider"
    }

        main(["switch"])
        details(
            [
                "switch",
                "temperature",
                "contact",
                "outlet",
                "light",
                "modeState",
                "thermostatOperatingState",
                "thermostatMode",
                "spaPump1",
                "spaPump2",
                "heatingSetpoint",
                "refresh",
                "statusText",
                "schedule",
                "levelSliderControl"
            ]
        )
    }
}

def refresh() {
    log.debug "Started: --- handler.refresh"
    Date now = new Date()
    def timeString = now.format("EEE MM/dd h:mm:ss a", location.timeZone)
    sendEvent(name: "statusText", value: "Cloud Refresh Requested at\n${timeString}...", "displayed":false)
    log.debug "Ended:   --- handler.refresh"
}

def installed() {
	log.debug "Installed: Begin..."
    def params = [
        "statusText":"Installed...",
        "switch":"off",
        "temperature":0,
        "contact":"open",
        "thermostatOperatingState":"idle",
        "outlet":"off",
        "light":"off",
        "modeState":"Off",
        "heatMode":"Off",
        "spaPump1":"Off",
        "spaPump2":"Off",
        "heatingSetpoint":0
    ]
    setHotTubStatus(params)
	log.debug "Installed: End..."
}

def parse(String description) {
	// This is a simulated device. No incoming data to parse.
	log.debug "parse: Begin..."
    log.debug "description: $description"
	log.debug "parse: End..."
}

def slider(setLevel) {
	log.debug "setLevel: ${setLevel}"
}


def on() {
	log.trace "HotTub: Turning On"
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.trace "HotTub Turning Off"
	sendEvent(name: "switch", value: "off")
}

def setHotTubStatus(params) {
    log.debug "params: ${params}"
    def quietBool = true
    for ( e in params ) {
        log.info "key = ${e.key}, value = ${e.value}"
        quietBool = true
        if (e.key=="statusText") {
            quietBool = false
        }
        sendEvent(name: "${e.key}", value: "${e.value}", displayed: quietBool)
    }
}