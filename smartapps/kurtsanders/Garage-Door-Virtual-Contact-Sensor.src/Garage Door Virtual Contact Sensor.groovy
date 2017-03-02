/*
 *  SanderSoft Garage Door Virtual Contact Sensor via REST API callbacks
 */

definition(
    name: "Garage Door Virtual Contact Sensor",
    namespace: "SanderSoft",
    author: "Kurt@KurtSanders.com",
    description: "SanderSoft: Garage Door Virtual Contact Sensor",
    category: "My Apps",
    iconUrl:   "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq.png",
    iconX2Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@3x.png",
    oauth: true
)
import java.text.SimpleDateFormat;

preferences {
  page(name:"mainPage")
  page(name:"devicesPage")
  page(name:"disableAPIPage")
  page(name:"enableAPIPage")
  section ("Garage Door Virtual Contact Sensors...") {
    input "contacts", "capability.contactSensor", multiple: true, required: false
  }
}
def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}
def uninstalled() {
	if (state.endpoint) {
		try {
			logDebug "Revoking API access token"
			revokeAccessToken()
		}
		catch (e) {
			log.warn "Unable to revoke API access token: $e"
		}
	}
}
def updated() {
	// Added additional logging from @kurtsanders
	log.debug "Garage Door Virtual Contact Sensor App updated with the following settings:\n${settings}"
    log.debug "##########################################################################################"
    log.debug "secret = \"${state.endpointSecret}\""
    log.debug "smartAppURL = \"${state.endpointURL}\""
    log.debug "The API has been setup. Please enter the following two lines into the Garage Door Virtual Contact Sensor App Python script."
    log.debug "##########################################################################################"
    log.debug "Recipients configured: $recipients"

	unsubscribe()
	initialize()
}

def initialize() {
}

mappings {
	path("/sensors/:doorname/:state") {
      action: [
        GET: "updateSensors"
    ]
  }
}

def updateSensors() {
	def doorname 	= params.doorname
	def state 		= params.state
	log.debug 	"updateSensors(): doorname=${doorname}"
	log.debug 	"updateSensors(): state=${state}"

	Date now = new Date()
//  SimpleDateFormat timestamp = new SimpleDateFormat("EEE dd hh:mm:ss a");
   	def timeString = now.format("EEE MM/dd hh:mm:ss a", location.timeZone) 

	contacts.each {
    	if(it.name == doorname)
        	{
			log.debug "Found contact ${it.displayName} with id ${it.id} with deviceNetworkId ${it.deviceNetworkId} with current value ${it.currentContact}"
            def message = "The ${doorname} is ${state} at ${timeString}!"
            if (location.contactBookEnabled && recipients) {
                log.debug "Contact book enabled!"
                sendNotificationToContacts(message, recipients)
            } else {
                log.debug "Contact book not enabled"
                if (phone) {
                    sendSms(phone, message)
                }
            }
            def thecontact = it.deviceNetworkId
            def contact = contacts.find { it.deviceNetworkId == thecontact }
                if (contact) {
		          	contact.sensorstate("${state}")
        		}		 
			}
		}
	}

private mainPage() {
	dynamicPage(name: "mainPage", uninstall:true, install:true) {
		section("API Setup") {
			if (state.endpoint) {
					paragraph "API has been setup. Please enter the following information into the Python script."
                    paragraph "URL:\n${state.endpointURL}"
                    paragraph "Secret:\n${state.endpointSecret}"
                    href "disableAPIPage", title: "Disable API", description: ""
			}
            else {
			paragraph "API has not been setup. Tap below to enable it."
            href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"
            }

		}
        section("Device Setup") {
        href name: "devicesPageLink", title: "Select Devices", description: "", page: "devicesPage"
        }
        section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
      }
   }
}


def disableAPIPage() {
	dynamicPage(name: "disableAPIPage", title: "") {
		section() {
			if (state.endpoint) {
				try {
					revokeAccessToken()
				}
				catch (e) {
					log.debug "Unable to revoke access token: $e"
				}
				state.endpoint = null
			}
			paragraph "It has been done. Your token has been REVOKED. You're no longer allowed in API Town (I mean, you can always have a new token). Tap Done to continue."
		}
	}
}

def enableAPIPage() {
	dynamicPage(name: "enableAPIPage") {
		section() {
			if (initializeAppEndpoint()) {
				paragraph "Woo hoo! The API is now enabled. Brace yourself, though. I hope you don't mind typing long strings of gobbledygook. Sorry I don't know of an easier way to transfer this to the PC. Anyways, tap Done to continue"
			}
			else {
				paragraph "It looks like OAuth is not enabled. Please login to your SmartThings IDE, click the My SmartApps menu item, click the 'Edit Properties' button for the BitBar Output App. Then click the OAuth section followed by the 'Enable OAuth in Smart App' button. Click the Update button and BAM you can finally tap Done here.", title: "Looks like we have to enable OAuth still", required: true, state: null
			}
		}
	}
}

def devicesPage() {
	dynamicPage(name:"devicesPage") {

 		section ("Choose Devices") {
			paragraph "Select devices that you want to be displayed in the menubar."
			input "contacts", "capability.contactSensor",
				title: "Which Contact Sensors Control your Garage Doors?",
				multiple: true,
				hideWhenEmpty: true,
				required: false
		}
	}
}

private initializeAppEndpoint() {
	if (!state.endpoint) {
		try {
			def accessToken = createAccessToken()
			if (accessToken) {
				state.endpoint = apiServerUrl("/api/token/${accessToken}/smartapps/installations/${app.id}/")	
                state.endpointURL = apiServerUrl("/api/smartapps/installations/${app.id}/")	
                state.endpointSecret = accessToken
			}
		}
		catch(e) {
			state.endpoint = null
		}
	}
	return state.endpoint
}
