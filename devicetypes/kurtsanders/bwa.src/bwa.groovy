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
def version() {
    return ["V2.0", "Requires BWA Hot Tub Service Manager App"]
}
// End Version Information
import groovy.time.*
import java.text.DecimalFormat
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
        capability "Contact Sensor"
        capability "Light"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Thermostat"
        
        attribute "tubStatus", "string"
        attribute "statusText", "string"
        attribute "schedulerFreq", "enum", ['0','1','5','10','15','30','60','180']
        attribute "spaPump1", "enum", ['Low','High','Off']
        attribute "spaPump2", "enum", ['Low','High','Off']
        attribute "heatMode", "enum", ['Rest','Ready/Rest','Ready']

        command "heatModeRest"
        command "heatModeReady"
        command "spaPump1Low"
        command "spaPump1High"
        command "spaPump1Off"
        command "spaPump2Low"
        command "spaPump2High"
        command "spaPump2Off"
    }
    tiles(scale: 2) {
        // Current Temperature Reading
        multiAttributeTile(name:"temperature", type:"generic", width:6, height:4, canChangeIcon: false) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("default",label:'${currentValue}º',
                               backgroundColors:[
                                   [value: 0,   color: whiteColor],
                                   [value: 50,  color: navyColor],
                                   [value: 90,  color: blueColor],
                                   [value: 104, color: redColor]
                               ])
            }
            tileAttribute("tubStatus", key: "SECONDARY_CONTROL") {
                attributeState("tubStatus", label:'${currentValue}', defaultState: true)
            }
        }
        valueTile("heatingSetpoint", "device.heatingSetpoint",  decoration: "flat", width: 3, height: 1) {
            state("heatingSetpoint", label:'Set Temp:\n${currentValue}°F', 
                backgroundColors:[
                    [value: 0,   color: whiteColor],
                    [value: 50,  color: navyColor],
                    [value: 90,  color: blueColor],
                    [value: 104, color: redColor]
                ]
            )
        }
        standardTile("thermostatOperatingState", "device.thermostatOperatingState", decoration: "flat", width: 2, height: 2) {
            state "idle", label:'${name}',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/idle.png"
            state "heating", label:'${name}',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/heating.png"
        }
        standardTile("thermostatMode", "device.thermostatMode", decoration: "flat", width: 2, height: 1,) {
            state "off",  label: 'Heat Off', icon: "st.Outdoor.outdoor19"
            state "heat", label: 'Heat On',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/heatMode.png"
        }
        // Hot Tub Turn Power On/Off
        standardTile("switch", "device.switch",  width: 2, height: 2, canChangeIcon: true) {
            state "off", label:'Off', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            state "on", label:'On', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
            state "turningOn", label:'Turning on', icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState: "turningOff"
            state "turningOff", label:'Turning off', icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState: "turningOn"
        }
        // LED Lights
        standardTile("light", "device.light",  width: 2, height: 1, canChangeIcon: true) {
            state "on", label:'Lights ${name}', icon:"st.Lighting.light11", backgroundColor:greenColor	, nextState:"turningOff"
            state "off", label:'Lights ${name}', icon:"st.Lighting.light13", backgroundColor:whiteColor	, nextState:"turningOn"
            state "turningOn", label:'${name}', icon:"st.Lighting.light11", backgroundColor:yellowColor	, nextState:"turningOff"
            state "turningOff", label:'${name}', icon:"st.Lighting.light13", backgroundColor:yellowColor , nextState:"turningOn"
        }
        // Network Connected Status
        standardTile("contact", "device.contact",  width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "open",   label:'Offline', action:"open",
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/offline.png",
                backgroundColor:yellowColor
            state "closed", label:'Online', action:"closed",
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/broadcast.png",
                backgroundColor:greenColor
        }
        standardTile("spaPump1", "spaPump1", inactiveLabel: false, decoration: "flat", width: 2, height: 1,) {
            state "Low", label: 'Jet1 Low', action:"spaPump1High", 
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet1 High', action:"spaPump1Off", 
                icon: "st.valves.water.open", backgroundColor: blueColor
            state "Off", label: 'Jet1 Off', action:"spaPump1Low", 
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        standardTile("spaPump2", "spaPump2", inactiveLabel: false, decoration: "flat", width: 2, height: 1,) {
            state "Low", label: 'Jet2 Low', action:"spaPump2High", 
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet2 High', action:"spaPump2Off", 
                icon: "st.valves.water.open", backgroundColor: blueColor
            state "Off", label: 'Jet2 Off', action:"spaPump2Low", 
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        // Hot Tub Heat Mode On/Off
        standardTile("heatMode", "heatMode", inactiveLabel: false, width: 2, height: 2,) {
            state "Ready", 		label:'Ready', 	action:"heatModeRest", 	icon:"st.Kids.kids20", 	backgroundColor:"#ffffff", nextState:"Rest"
            state "Ready/Rest", label:'Ready', 	action:"heatModeRest", 	icon:"st.Kids.kids20", 	backgroundColor:"#ffffff", nextState:"Rest"
            state "Rest", 		label:'Rest', 	action:"heatModeReady", icon:"st.Kids.kids20", 	backgroundColor:"#00a0dc", nextState:"Ready"
        }
        // Descriptive Text
        valueTile("statusText", "statusText", decoration: "flat", width: 3, height: 1, wordWrap: true) {
            state "statusText", label: '${currentValue}', backgroundColor:whiteColor
        }
        valueTile("schedulerFreq", "schedulerFreq", decoration: "flat", inactiveLabel: false, width: 3, height: 1, wordWrap: true) {
            state "schedulerFreq", label: 'Refresh Every\n${currentValue} min(s)', 
                backgroundColors: [
                    [value: '0',    color: "#FF0000"],
                    [value: '1',    color: "#9400D3"],
                    [value: '2',    color: "#00FF00"],
                    [value: '3',    color: "#458b74"],
                    [value: '4',    color: "#FF7F00"],
                    [value: '5',    color: "#4B0082"],
                    [value: '10',   color: "#0000FF"],
                    [value: '15',   color: "#00FF00"],
                    [value: '30',   color: "#FFFF00"],
                    [value: '60',   color: "#FF7F00"],
                    [value: '180',  color: "#ff69b4"]
                ]
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main(["temperature"])
        details(
            [
                "temperature",
                "switch",
                "heatMode",
                "contact",
                "light",
                "thermostatOperatingState",
                "thermostatMode",
                "spaPump1",
                "spaPump2",
                "heatingSetpoint",
                "refresh",
                "statusText",
                "schedulerFreq"
            ]
        )
    }
}

def refresh() {
    log.debug "BWA handler.refresh ---- Started"
    Date now = new Date()
    def timeString = now.format("EEE MMM dd h:mm:ss a", location.timeZone)
    sendEvent(name: "statusText", value: "Cloud Refresh Requested at\n${timeString}...", "displayed":false)
    parent.refresh()
    log.debug "BWA handler.refresh ---- Ended"
}

def installed() {
	log.debug "BWA Installed: Begin..."
	log.debug "BWA Installed: End..."
}
def on() {
    log.trace "HotTub: Turning On"
    parent.tubAction('switch', 'on')
}
def off() {
    log.trace "HotTub Turning Off"
    parent.tubAction('switch', 'off')
}
def heatModeReady() {
    log.trace "HotTub: Turning HeatMode Ready"
    parent.tubAction('heatMode', 'Ready')
}
def heatModeRest() {
    log.trace "HotTub Turning HeatMode Rest"
    parent.tubAction('heatMode', 'Rest')
}
def spaPump1Low() {
    log.trace "HotTub Turning spaPump1 Low"
    parent.tubAction('spaPump1', 'Low')
}
def spaPump1High() {
    log.trace "HotTub Turning spaPump1 High"
    parent.tubAction('spaPump1', 'High')
}
def spaPump1Off() {
    log.trace "HotTub Turning spaPump1 Off"
    parent.tubAction('spaPump1', 'Off')
}
def spaPump2Low() {
    log.trace "HotTub Turning spaPump2 Low"
    parent.tubAction('spaPump2', 'Low')
}
def spaPump2High() {
    log.trace "HotTub Turning spaPump2 High"
    parent.tubAction('spaPump2', 'High')
}
def spaPump2Off() {
    log.trace "HotTub Turning spaPump2 Off"
    parent.tubAction('spaPump2', 'Off')
}