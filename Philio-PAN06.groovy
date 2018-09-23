/* Philio PAN06 Dual Relay by He-Man321*/ 
metadata {
definition (name:"Philio PAN06 Dual Relay",namespace:"",author:"He-Man321") {
capability "Switch"
capability "Relay Switch"
capability "Polling"
capability "Configuration"
capability "Refresh"
capability "Zw Multichannel"
attribute "switch1","string"
attribute "switch2","string"
command "childOn"
command "childOff"
command "on1"
command "off1"
command "on2"
command "off2"
}

tiles(scale: 2){
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
			}
	}
	standardTile("switch1", "device.switch1",canChangeIcon: false, width: 2, height: 2) {
		state "on", label: 'S1', action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "off", label: 'S1', action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
        state "turningOn", label: 'S1', action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "turningOff", label: 'S1', action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
        
    }
	standardTile("switch2", "device.switch2",canChangeIcon: false, width: 2, height: 2) {
		state "on", label: 'S2', action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "off", label: 'S2', action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
        state "turningOn", label: 'S2', action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "turningOff", label: 'S2', action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
        
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }    

    main(["switch","switch1", "switch2"])
    details(["switch","switch1","switch2","refresh"])
}   
   
   preferences {
        def paragraph="Device Settings"
        input name:"param2",type:"number",range:"1..3",defaultValue:"1",required:false,
            title: "Change the physical button mode (e.g. Toggle/Momentary switch).\n" +
                   "1 - Edge mode,\n" +
                   "2 - Pulse mode,\n" +
                   "3 - Edge-Toggle mode\n" +
                   "Default value: 1."
    }
}

def parse(String description) {
    def result=[]
    def cmd=zwave.parse(description)
    if (cmd)
        result+=zwaveEvent(cmd)
    else
        log.debug "Non-parsed event: ${description}"
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
    def result=[]
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:2).format()
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:2,commandClass:37,command:2).format()
    response(delayBetween(result,1000))
}
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    def curswitch=cmd.sourceEndPoint
    def map=[name:"switch$curswitch"]
	if (cmd.parameter==[0]) map.value="off"
    if (cmd.parameter==[255]) map.value="on"
    def curstate=map.value
    try {
    	def childDevice=getChildDevices()?.find {it.deviceNetworkId=="$device.deviceNetworkId-sw${curswitch}"}
		if (childDevice) childDevice.sendEvent(name: "switch", value: curstate)
    } catch (e) {
        log.error "Couldn't find child device, probably not created during setup: ${e}"
    }        
    def events=[createEvent(map)]
    if (map.value=="on") events+=[createEvent([name:"switch",value:"on"])]
    else {
        def allOff=true
        if (curswitch==2 && device.currentState("switch1").value=="on") allOff=false
        if (curswitch==1 && device.currentState("switch2").value=="on") allOff=false        
        if (allOff) events+=[createEvent([name:"switch",value:"off"])]
    }
    events
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.error "Unhandled event=$cmd"
}

def refresh() {
	def cmds=[]
    cmds<<zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
	cmds<<zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:2).format()
    cmds<<zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:2,commandClass:37,command:2).format()
	delayBetween(cmds,1000)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
    updateDataValue("MSR", msr)
}

def poll() {
	def cmds=[]
	cmds<<zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:2).format()
    cmds<<zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:2,commandClass:37,command:2).format()
	delayBetween(cmds,1000)
}

def configure() {
    zwave.configurationV1.configurationSet(parameterNumber:2,configurationValue:[param2.value]).format()
}

def updated()
{
    def cmds=configure()
    if (!childDevices) {
		state.oldLabel=device.label
		try {
			for (i in 1..2)
				addChildDevice("erocm123","Switch Child Device","${device.deviceNetworkId}-sw${i}",device.hub.id,[completedSetup:true,name:"${device.displayName} (S${i})",isComponent:false])
        } catch (e) {
            log.error "Child device creation failed. Please make sure that the \"Switch Child Device\" is installed and published."
        }
    }    
    response(cmds)
}

def childRefresh(String dni) {
    refresh()
}
def childOn(String dni) {
    "on${channelNumber(dni)}"()
}
def childOff(String dni) {
    "off${channelNumber(dni)}"()
}
private channelNumber(String dni) {
	dni.split("-sw")[-1] as Integer
}

def on() {
	zwave.switchAllV1.switchAllOn().format()
}
def off() {
	zwave.switchAllV1.switchAllOff().format()
}

def on1() {
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:1,parameter:[255]).format()
}

def off1() {
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:1,parameter:[0]).format()
}

def on2() {
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:3,destinationEndPoint:2,commandClass:37,command:1,parameter:[255]).format()
}

def off2() {
	zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:3,destinationEndPoint:2,commandClass:37,command:1,parameter:[0]).format()
}