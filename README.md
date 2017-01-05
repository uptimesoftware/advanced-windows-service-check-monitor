advanced-windows-service-check-monitor
======================================

Requires the [wmic client](http://rpmfind.net/linux/rpm2html/search.php?query=wmic&submit=Search+...&system=&arch=x86_64) to be installed to work on a linux monitoring station

Example regex: To match all the Uptime services running on a monitoring station (useful for those using the overseer model - monitoring your monitoring station) we can use the following service monitor configuration:

Advanced Windows Service Check Settings
Domain	 uptimedemo.com	
Username	 administrator	
Password	 \*\*\*\*\*\*\*	
Service display name	 Uptime.\*	
Startup Type (Include)	 Automatic	
Startup Type (Exclude)	 	
Service Status (Include)	 Running	
Service Status (Exclude)	 	
Number of matches	
    Warning	is less than: 3	 
    Critical	  	 
Matched Services	
    Warning	  	 
    Critical	  	 
Response time	
    Warning	   ms	 
    Critical	   ms
    
A test of this against your monitoring station should yield:

Status: OK
Message: Monitor ran successfully. - time: 531 ms
Matched Services:
Uptime Data Collector / Startup Type : Auto / Status : Running
Uptime Web Server / Startup Type : Auto / Status : Running
Uptime Data Store / Startup Type : Auto / Status : Running
Uptime Controller / Startup Type : Auto / Status : Running
Number of matches: 4
Response time: 531 ms
