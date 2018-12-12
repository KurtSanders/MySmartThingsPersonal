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
        attribute "schedulerFreq", "string"
        attribute "spaPump1", "string"
        attribute "spaPump2", "string"
        attribute "modeState", "string"        

        command "setHotTubStatus"
        command "restMode"
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
        // Hot Tub Turn On/Off
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
            state "Low", label: 'Jet1 Low',
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet1 High',
                icon: "st.valves.water.open", backgroundColor: blueColor
            state "Off", label: 'Jet1 Off',
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        standardTile("spaPump2", "spaPump2", inactiveLabel: false, decoration: "flat", width: 2, height: 1,) {
            state "Low", label: 'Jet2 Low',
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet2 High',
                icon: "st.valves.water.open", backgroundColor: blueColor
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
                icon: "st.Kids.kids20", backgroundColor: blueColor
            state "Off", label: '${currentValue}',
                icon: "st.Kids.kids20", backgroundColor: whiteColor
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
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main(["temperature"])
        details(
            [
                "temperature",
                "switch",
                "modeState",
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
