# MentionBot

MentionBot is a Twitter Bot written in Java.

On Startup, MentionBot takes a Username of a Twitter user and starts a control loop, if the target user
posts anything to Twitter while MentionBot is running, it will post a mention ( @targetUserScreenName ) with
a message that the Target User will then get a notifcation for.

MentionBot terminates when the program is manually terminated or MentionBot has thrown 5 exceptions, 5 being an arbitrary 
number. 
