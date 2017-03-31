/**
*  restAPI
*
*  Copyright 2017 kurt@kurtsanders.com
*
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
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat;

preferences {
}
metadata {
    definition (name: "restapi", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Contact Sensor"
        capability "Polling"
        attribute "state", "string"
    }
    simulator {
    }

    tiles(scale: 2) {
        // Network Connected Status
        standardTile("contact", "device.contact",  width: 2, height: 2, decoration: "flat") {
            state "open",   label:'Offline', icon: "st.contact.contact.open",   action:"open",   backgroundColor:yellowColor
            state "closed", label:'Online',  icon: "st.contact.contact.closed", action:"closed", backgroundColor:greenColor
        }
        standardTile("refresh", "device.poll", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        // Descriptive Text
        valueTile("statusText", "statusText", width: 4, height: 2) {
            state "statusText", label: '${currentValue}', backgroundColor:whiteColor, defaultState: true
        }
        main "contact"
        details(["contact", "statusText", "refresh"])
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
    def jsonSlurper = new JsonSlurper()
    def map = stringToMap(description)
    def bodyString = new String(map.body.decodeBase64())
    log.debug "bodyString: ${bodyString}"
    def body = jsonSlurper.parseText(bodyString)
    log.debug "body: ${body}"
    def state = "open"
    switch (body.state) {
        case "online" :
        state = "closed"
        break
        case "offline" :
        state = "open"
        break
        default :
        state = "open"
        break
    }
    log.debug "state: ${state}"
    sendEvent(name: 'contact', value: state)
}

// handle commands
def poll() {
    log.debug "Executing 'poll'"
    main()
}

def main() {
    def method = "GET"
    def host = "10.0.0.41"
    def path = "/hottub"
    def port = "1500"
    def headers = [:]
    Date now = new Date()
    def timeString = now.format("EEE MM/dd h:mm:ss a", location.timeZone)
    sendEvent(name: 'statusText', value: timeString, , displayed: false)
    def hosthex = convertIPtoHex(host)
    def porthex = convertPortToHex(port)
    device.deviceNetworkId = "$hosthex:$porthex"
    headers.put("HOST", "$host:$port")
    def hubAction = new physicalgraph.device.HubAction(
        method: method,
        path: path,
        headers: headers
    )
    // log.debug hubAction
    hubAction
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect { String.format( '%02x', it.toInteger() ) }.join()
//    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}
private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
//    log.debug hexport
    return hexport
}
private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
//    log.debug("Convert hex to ip: $hex")
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private getHostAddress() {
    def parts = device.deviceNetworkId.split(":")
//    log.debug device.deviceNetworkId
    def ip = convertHexToIP(parts[0])
    def port = convertHexToInt(parts[1])
    return ip + ":" + port
}