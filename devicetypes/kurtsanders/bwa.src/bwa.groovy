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
        capability "Actuator"
        capability "Sensor"
        capability "Light"
        capability "Outlet"
        capability "Contact Sensor"
        capability "Temperature Measurement"
        capability "Refresh"
/*
        attribute "xspaSetTemp"  ,   "string"
        attribute "heatMode"  	,  	"enum"	, ["Off", "On"]
        attribute "spaPump1"  	,  	"enum"	, ["Off", "Low", "High"]
        attribute "spaPump2"	,  	"enum"	, ["Off", "Low", "High"]
        attribute "modeState"	, 	"enum"	, ["Rest", "Ready", "Rest/Ready", "Off"]
*/
        command "setHotTubStatus"
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
                      [value: 50,  color: navyColor],
                      [value: 95,  color: blueColor],
                      [value: 96, color: redColor4],
                      [value: 104, color: redColor]
                  ]
                 )
        }
        // LED Lights
        standardTile("light", "device.light",  width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', icon:"st.Lighting.light11", backgroundColor:greenColor	, nextState:"turningOff"
            state "off", label:'${name}', icon:"st.Lighting.light13", backgroundColor:whiteColor	, nextState:"turningOn"
            state "turningOn", label:'${name}', icon:"st.Lighting.light11", backgroundColor:yellowColor	, nextState:"turningOff"
            state "turningOff", label:'${name}', icon:"st.Lighting.light13", backgroundColor:yellowColor , nextState:"turningOn"
        }
        // Network Connected Status
        standardTile("contact", "device.contact",  width: 2, height: 2, decoration: "flat") {
            state "open",   label:'Offline', action:"open",   icon:"st.alarm.water.wet", backgroundColor:yellowColor
            state "closed", label:'Online', action:"closed", icon:"st.alarm.water.dry", backgroundColor:greenColor
        }
        // Descriptive Text
        valueTile("statusText", "statusText", width: 6, height: 1) {
            state "statusText", label: '${currentValue}', backgroundColor:whiteColor, defaultState: true
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
        valueTile("spaSetTemp", "spaSetTemp", width: 2, height: 2) {
            state("spaSetTemp", label:'${currentValue}°F Set Temp')
        }
        standardTile("heatMode", "heatMode", inactiveLabel: false,
                     decoration: "flat", width: 2, height: 2,) {
            state "On", label: '${name}',
                icon: "st.thermostat.heat", backgroundColor: pinkColor
            state "Off", label: 'Heater',
                icon: "st.thermostat.heating-cooling-off", backgroundColor: whiteColor
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main(["switch"])
        details(
            [
                "switch",
                "temperature",
                "networkConnected",
                "outlet",
                "light",
                "modeState",
                "heatMode",
                "spaPump1",
                "spaPump2",
                "spaSetTemp",
                "refresh",
                "statusText"
            ]
        )
    }
}

def refresh() {
    log.trace("--- handler.refresh")
    sendEvent(name: "statusText", value: "Cloud Refresh Requested...", "displayed":false)

}

def installed() {
	log.debug "Installed: Begin..."
	log.debug "Installed: End..."
}

def parse(String description) {
	// This is a simulated device. No incoming data to parse.
	log.debug "parse: Begin..."
    log.debug "description: $description"
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

def setHotTubStatus(params) {
    log.debug "params: ${params}"
    def quietBool 		= true
    for ( e in params ) {
        log.info "key = ${e.key}, value = ${e.value}"
        quietBool = true
        if (e.key=="statusText") {
            quietBool = false
        }
        sendEvent(name: "${e.key}", value: "${e.value}", displayed: quietBool)
    }
}

def test() {
    Random random = new Random()
    def max = 104
    def min = 50
    int randomNumber = random.nextInt(max + 1 - min) + min;
    def tvalue = [name: "temperature", value: randomNumber, "unit" : "°F"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)

    def	things = ["switch","outlet","light"]
    things.each {
    	log.debug "Things: ${it}"
        def currentState = device.currentValue("${it}")
        log.info "currentState: ${currentState}"
        if (currentState=='on') {
            currentState='off'
        }
        else
        {
            currentState='on'
        }
        tvalue = [name: "${it}", value: currentState]
        log.info "Sending sendEvent(${tvalue})"
        sendEvent(tvalue)
    }

    tvalue = [name: "modeState", value: "Ready/Rest"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)

    tvalue = [name: "networkConnected", value: "closed"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)

    tvalue = [name: "heatMode", value: "On"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)

    tvalue = [name: "spaPump1", value: "Low"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)

    tvalue = [name: "spaPump2", value: "High"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)

    tvalue = [name: "spaSetTemp", value: "101 °F\nSet Mode"]
    log.info "Sending sendEvent(${tvalue})"
    sendEvent(tvalue)


}
