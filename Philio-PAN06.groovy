/* Philio PAN06 Dual Relay by He-Man321
Version 1.1 - 2020/03/05
Version 1.2 - 2020/03/05 - Added the Current States initialisation in installation
NOTE: 	This nust create the chid devices as 1 and 2 (e.g. 22-sw1 and 22-sw2). I have seen it create them with other numbers, which won't work - AI - 19/09
*/ 
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
fingerprint mfr:"013C", prod:"0001", model:"0013", deviceJoinName: "Philio PAN06 Dual Relay" //Untested so far, will see if the next one I add gets assigned this handler automatically - AI - 20/03/09
tiles(scale: 2){
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'Master', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
				attributeState "off", label: 'Master', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState:"turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState:"turningOn"
			}
	}
	standardTile("switch1", "device.switch1",canChangeIcon: false, width: 2, height: 2) {
		state "on", label: 'S1', action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "off", label: 'S1', action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
        state "turningOn", label: 'S1', action: "off1", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "turningOff", label: 'S1', action: "on1", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState:"turningOn"        
    }
	standardTile("switch2", "device.switch2",canChangeIcon: false, width: 2, height: 2) {
		state "on", label: 'S2', action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "off", label: 'S2', action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn"
        state "turningOn", label: 'S2', action: "off2", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState:"turningOff"
		state "turningOff", label: 'S2', action: "on2", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState:"turningOn"        
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
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) {
    log.debug "MultiChannelCapabilityReport: $cmd"
}
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd) {
	log.debug "MultiChannelEndPointReport: $cmd"
}
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    def result=[]
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:2).format()
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:2,commandClass:37,command:2).format()
    delayBetween(result,1000)
}
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {   
    def result=[]
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:1,commandClass:37,command:2).format()
    result << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1,destinationEndPoint:2,commandClass:37,command:2).format()
    delayBetween(result,1000)
}
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {    
    def curswitch=cmd.sourceEndPoint
    def state=cmd.parameter==[255]?"on":"off"
	try {
    	def childDevice=getChildDevices()?.find {it.deviceNetworkId=="$device.deviceNetworkId-sw${curswitch}"}
		if (childDevice) childDevice.sendEvent(name: "switch", value: state)
    } catch (e) {
        log.error "Couldn't find child device, probably not created during setup: ${e}"
    }  
	if (state=="on") sendEvent(name: "switch", value: "on")
    else {
        def allOff=true
        if (curswitch==2 && device.currentState("switch1").value=="on") allOff=false
        if (curswitch==1 && device.currentState("switch2").value=="on") allOff=false        
        if (allOff) sendEvent(name: "switch", value: "off")
    }  
}
def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	log.debug "VersionReport: $cmd"	
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
    zwave.configurationV1.configurationSet(parameterNumber:2,configurationValue:[settings.param2==null?1:settings.param2]).format()
}
def updated() {
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
def installed() {
    zwave.manufacturerSpecificV1.manufacturerSpecificGet()
	def cmds=configure()
    if (!childDevices) {
		try {
			for (i in 1..2)
				addChildDevice("erocm123","Switch Child Device","${device.deviceNetworkId}-sw${i}",device.hub.id,[completedSetup:true,name:"${device.displayName} (S${i})",isComponent:false])
        } catch (e) {
            log.error "Child device creation failed. Please make sure that the \"Switch Child Device\" is installed and published."
        }
    }    
    sendEvent(name: "switch1", value: "off") //Need this to create the Current States - AI - 20/03/09
    sendEvent(name: "switch2", value: "off") //Need this to create the Current States - AI - 20/03/09
    return(cmds)
}
