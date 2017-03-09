/* Virtual Contact Sensor for Garage Doors */

metadata {
    definition (name: "VirtualContactSensorGarage", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Contact Sensor"
        capability "Refresh"
        capability "Polling"
    // Add commands as needed
    command "sensorstate"
    }
  simulator {
    // Nothing here, you could put some testing stuff here if you like
  }

  tiles {
    // Main Row
    standardTile("sensor", "device.contact", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
      state "open",   label: '${name}', icon: "st.contact.contact.open",   backgroundColor: "#ffa81e"
      state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
    }

    // This tile will be the tile that is displayed on the Hub page.
    main "sensor"

    // These tiles will be displayed when clicked on the device, in the order listed here.
    details(["sensor"])
  }
}

// handle commands
def sensorstate(String state) {
  // state will be a valid state for a zone (open, closed)
  // door will be a number for the zone
  log.debug "SensorState: ${state}"
  sendEvent (name: "contact", value: "${state}")
}

def poll() {
  log.debug "Executing 'poll'"
  // TODO: handle 'poll' command
  // On poll what should we do? nothing for now..
}

def refresh() {
  log.debug "Executing 'refresh' which is actually poll()"
  poll()
  // TODO: handle 'refresh' command
}
