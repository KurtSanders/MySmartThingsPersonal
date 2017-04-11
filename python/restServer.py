#!/usr/bin/env python2.7

import os
import time
import xmlrpclib
from datetime import datetime

import supervisor.xmlrpc
from flask import Flask
from flask import jsonify
from flask import render_template

app = Flask(__name__)


def banner():
    print("RaspberryPi3 RestAPI Server (c) SanderSoft, Inc")
    return


app.before_first_request(banner)


def viewOutput(title, lines):
    output = "<p>" + title + "</p><pre>"
    for line in lines.splitlines():
        output += line + "<br>"
    output += "</pre>"
    return output


@app.route("/")
def root():
    return "RaspberryPi3 RestAPI Server (c) SanderSoft, Inc"


@app.route("/httpok/")
def httpok():
    return "Ok"


@app.route("/online/")
def online():
    return jsonify(state="online",
                   server="Pi3",
                   timestamp=datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                   )


@app.route("/hottub/")
def hottub():
    return ping("10.0.0.35")


@app.route("/ping/", defaults={"ipAddress": "10.0.0.35"})
@app.route("/ping/<ipAddress>/")
def ping(ipAddress):
    stateValue = "%s" % ("Offline" if os.system("ping -q -c1 %s >/dev/null 2>&1" % ipAddress) else "Online")
    return jsonify(server=ipAddress,
                   state=stateValue
                   )


@app.route("/super/", defaults={"command": "status"})
@app.route("/super/<command>/")
def super(command):
    p = xmlrpclib.ServerProxy(
        'http://127.0.0.1',
        transport=supervisor.xmlrpc.SupervisorTransport(
            None,
            None,
            'unix:///tmp/supervisor.sock')
    )
    processes = p.supervisor.getAllProcessInfo()
    for x, d in enumerate(processes):
        processes[x]['start'] = time.strftime(
            '%a %m-%d %I:%M:%S%p',
            time.localtime(d["start"]
                           )
        )
        processes[x]['description'] = d['description'].split(',')[1]

    return render_template(
        "status.html",
        serverStatus=p.supervisor.getState()['statename'],
        processes=processes
    )


if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5001)
