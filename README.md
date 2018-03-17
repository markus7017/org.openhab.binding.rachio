# <bindingName> Rachio Sprinkler Binding

This binding allows to retrieve status information from Rachio Sprinklers and control some function like run zones, stop watering etc. It uses the Rachio Cloud API, so you need an account and an apikey. You are able to enabled callbacks to receive events like start/stop zones, skip watering etc. However, this requires some network configuration (e.g. port forwarding on the router). The binding could run in polling mode, but then you don't receive those events. 

**_If possible, provide some resources like pictures, a YouTube video, etc. to give an impression of what can be done with this binding. You can place such resources into a `doc` folder next to this README.md._**

## Supported Things

<table style="width:100%"; border="1">
<tr align="left">
<th>Type</th>
<th>Description</th> 
</tr>
<tr><td><b>cloud</b></td><td>Each Rachio account is reppresented by a cloud thing. The binding supports multiple accounts at the same time.</td>
<tr><td><b>device</b></td><td>Each sprinkler controller is represented by a device thing, which links to the cloud thing
<tr><td><b>zone</b></td><td>Each zone for each controller creates a zone thing, which links to the device thing (and idirectly to the bridge thing)</td>
</tr>
</table>

## Discovery

_Describe the available auto-discovery features here. Mention for what it works and what needs to be kept in mind when using it._
The device setup is read from the Cloude setup, so it shares the same items as the Smartphone and Web Apps, so there is no special setup required. In fact all Apps (including this binding) control the same device. The binding implements monitoring and control functions, but no configuration etc. To change configuration you could use the smartphone App. 

As a result the following things are created
1xcloud per account
nxdevice for each controller
nxzone for each zone on any controller

Example: 2 controllers with 8 zones each under the same account creates 19 things (1xbridge, 2xdevice, 16xzone). 
## Binding Configuration

If the apikey is configured in the rachio.cfg file a bridge thing is created dynamically and device discovery starts. 

```
# Configuration for the Rachio Binding
# apikey=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx
# callbackUrl="https://mydomain.com:myport/rachio/webhook
# clearAllCallbacks=false
# pollingInterval=120
# defaultRuntime=120
```

See configuration of bridge things below for a description of the config parameters.

## Thing Configuration

_Describe what is needed to manually configure a thing, either through the (Paper) UI or via a thing-file. This should be mainly about its mandatory and optional configuration parameters. A short example entry for a thing file can help!_

### bridge thing - represents a Rachio Cloud account

<table style="width:100%"; border="1px">
<tr align="left">
<th>Parameter</th>
<th>Description</th> 
</tr>

<tr>
<td><b>apikey</b></td>
<td>This is a token required to access the Rachio Cloud account.<br/>
Go to Rachio Web App (https://rachio.com->login), click on Account Settings in the left navigation. At the bottom you'll find a link "Get API key". Copy the copy and post it to the bridge configuration: apikey=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx.<p/></td> 
</tr>

<tr>
<td><b>pollingInterval</b></td>
<td>Specifies the delay between two status polls. Usually something like 10 minutes should be enough to have a regular status update when the interfaces is configured. If you don't want/can use events a smaller delay might be interesting to get quicker responses on running zones etc.<p/>
Important: Please make sure to use an interval > 90sec. Rachio has a reshhold for the number of API calls per day: 1700. This means if you are accessing the API for more than once in a minute your account gets blocked for the rest of the day.<p/></td> 
</tr>

<tr>
<td><b>defaultRuntime</b></td>
<td>You could run zones in 2 different ways<br>
1. Just by pushing the button in your UI. The zone will start watering for  <defaultRuntime> seconds.
2. Setting the zone's channel runTime to <n> seconds and then starting the zone. This will start the zone for <n> seconds.<br/>Usually this variant required a OH rule setting the runTime and then sending a ON to the run channel.<p/></td> 
</tr>

<tr>
<td><b>callbackUrl</b></td>
<td>Enable event interface:<p/>
The Rachio Cloud allows receiving events. For this a REST interface will be provided by the binding (<server>:<port>:/rachio/webhook). However, this requires to open a port to the Internet. This can be done by a simple port forwarding from an external port (e.g. 50043) to you OH device. Please make sure to us "https://", so the transport layer is encrypted.<p/>
Please make sure that notifications are enabled if you want to use the event interface. Go to the Rachio Web App->Accounts Settings->Notifications.<p/></td>
</tr>

<tr>
<td><b>clearAllCallbacks</b></td>
<td>The binding dynamically registers the callback. It also supports multiple applications registered to receive events, e.g. a 2nd OH device with the binding providing the same functionality. If for any reason your device setup changes (e.g. new ip address) you need to clear the registered URL once to avoid the "old URL" still receiving events. This also allows to move for a test setup to the regular setup.</td>
</tr>
</table>

The bridge thing doesn't have any channels.
<hr/>

### device thing - represents a single Rachio controller

The are no additional configuration options on the device level.

<b>Channels</b><p/>
<table style="width:100%"; border="1px">
<tr align="left">
<th>Parameter</th>
<th>Description</th> 
</tr>
<td><b>number</b></td><td>Zone number as assigned by the controller (zone 1..8)</td></tr>
<tr><tr><td><b>name</b></td><td>Name of the zone as configured in the App.</td></tr>
<tr><td><b>enabled</b></td><td>ON: zone is enabled (ready to run), OFF: zone is disabled.</td></tr>
<tr><td><b>run</b></td><td>If this channel received ON the zone starts watering. If runTime is = 0 the defaultRuntime will be used.</td></tr>
<tr><td><b>runTime</b></td><td>Number of seconds to run the zone when run receives ON command</td></tr>
<tr><td><b>runTotal</b></td><td>Total number of seconds the zone was watering (as returned by the cloud service).</td></tr>
<tr><td><b>imageUrl</b></td><td>URL to the zone picture as configured in the App. Rachio supplies default pictures if no image was created. This can be used e.g. in a habPanel to show the zione picture and display the zone name.</td></tr>
<tr><td><b>event</b></td><td>This channel receives a JSON-formatted message on each event received from the Rachio Cloud.<br/>Format:<p/></td></tr>
</table>

### zone thing - represents one zone of a controller

The are no additional configuration options on the zone level.

_Note that it is planned to generate some part of this based on the XML files within ```ESH-INF/thing``` of your binding._

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

## Full Example

conf/things/rachio.things

```
Bridge rachio:cloud:1 [ apikey="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxx", pollingInterval=180, defaultRuntime=120, callbackUrl="https://mydomain.com:50043/rachio/webhook", clearAllCallbacks=true  ]
{
}
```


## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
