#!/usr/bin/env bash

printf "%s\t: %s\n" "Starting RestServer" "$(date +%F_%T)"
printf "%s\t: %s\n" "Program Directory  " "$(pwd)"

# Create the named pipe
tmppipe=$(mktemp -u)
mkfifo -m 600 "$tmppipe"
printf "%s\t: %s\n" "Temp Pipe Location " "$tmppipe"

trap ctrl_c INT

function ctrl_c() {
    rm -f "$tmppipe" && printf "\n%s\t%s\n" $(date +%F_%T) ": Ctrl-c detected, restServer gracefully shut down.\n" && exit
}

function HTTP_Response() {
    printf "%s" "$2"
    HTTP_200="HTTP/1.1 200 OK"
    HTTP_CT="Content-Type: text/html"
    HTTP_LOC="Location: $1"
    HTTP_CL="Content-Length: $((${#2} + 0))"
    HTTP_SC="Connection: close"
    HTTP_OUTPUT=$(printf "%s\n%s\n%s\n%s\n%s" "$HTTP_200" "$HTTP_LOC" "$HTTP_CL" "$HTTP_CT" "$HTTP_SC")
    HTTP_OUTPUT=$(printf "%s\n\n%s" "$HTTP_OUTPUT" "$2")
#    printf "%s" "$HTTP_OUTPUT"
    printf "%s" "$HTTP_OUTPUT" >"$tmppipe"
}

while true
do
    cat "$tmppipe" | nc -l 1500 > >( # parse the netcat output, to build the answer redirected to the pipe "out".
    export REQUEST=
    while read line
    do
        line=$(echo "$line" | tr -d '[\r\n]')
        timestamp=$(date +%F_%T)
        if echo "$line" | grep -qE '^GET /' # if line starts with "GET /"
        then
            REQUEST=$(echo "$line" | cut -d ' ' -f2) # extract the request
            printf "\n%s" "$timestamp :  $REQUEST - > "
        elif [ "x$line" = x ] # empty line / end of request
        then
            HTTP_200="HTTP/1.1 200 OK"
            HTTP_LOCATION="Location:"
            HTTP_404="HTTP/1.1 404 Not Found"
            # call a script here
            # Note: REQUEST is exported, so the script can parse it (to answer 200/403/404 status code + content)
            if echo $REQUEST | grep -qE '^/ping/'
            then
                cmd_output=$(ping -c1 ${REQUEST#"/ping/"} >/dev/null && echo '{"state":"online"}' || echo '{"state":"offline"}')
                HTTP_Response "$REQUEST" "$cmd_output"
#                printf "%s\n%s %s\n\n%s\n" "$HTTP_200" "$HTTP_LOCATION" $REQUEST ${REQUEST#"/echo/"} >"$tmppipe"
            elif echo $REQUEST | grep -qE '^/hottub'
            then
                cmd_output=$(ping -c1 10.0.0.35 >/dev/null && echo '{"state":"online"}' || echo '{"state":"offline"}')
                printf "%s\n%s %s\n\n%s\n" "$HTTP_200" "$HTTP_LOCATION" $REQUEST $cmd_output >"$tmppipe"
                printf "%s" "$cmd_output"
            elif echo $REQUEST | grep -qE '^/alive'
            then
                HTTP_Response "$REQUEST" "Yes"
            else
                printf "%s\n%s %s\n\n%s\n" "$HTTP_404" "$HTTP_LOCATION" $REQUEST "Resource $REQUEST NOT FOUND!" >"$tmppipe"
            fi
        fi
    done
    )
done
