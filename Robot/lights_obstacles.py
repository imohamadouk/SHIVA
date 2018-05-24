#!/usr/bin/env python
# -*- coding: utf-8 -*-

# Authors: SHIVA Team #


import rospy
from sensor_msgs.msg import LaserScan
from geometry_msgs.msg import Twist
import threading, socket, time, sys
hote = '192.168.0.110'
port = 12800

class Obstacle(threading.Thread):
	
    def __init__(self):

	threading.Thread.__init__(self)
	
        self.LIDAR_ERR = 0.05
        self._cmd_pub = rospy.Publisher('/cmd_vel', Twist, queue_size=1)
        #self.obstacle()
	self.etat = False 
	self.paused = False
	self.pause_cond = threading.Condition(threading.Lock())
    
    def pause(self):
	self.paused = True
	self.pause_cond.acquire()
  
    def resume(self):
	self.paused = False
	self.pause_cond.notify()
	self.pause_cond.release()

    def get_scan(self, data):
        scan = LaserScan()
        scan = data
        for i in range(360):
            if i <= 15 or i > 335:
                if scan.ranges[i] >= self.LIDAR_ERR:
                    self.scan_filter.append(scan.ranges[i])

    def run(self):
	self.etat = True
        self.twist = Twist()
        while not rospy.is_shutdown():
		with self.pause_cond :
			while self.paused:
				self.pause_cond.wait()

		    	msg = rospy.wait_for_message("/scan", LaserScan)
		    	self.scan_filter = []
		    	for i in range(360):
		        	if i <= 15 or i > 335:
				    if msg.ranges[i] >= self.LIDAR_ERR:
				        self.scan_filter.append(msg.ranges[i])

		    	if min(self.scan_filter) < 0.2:
		        	self.twist.linear.x = 0.0
		        	self.twist.angular.z = 0.0
		        	self._cmd_pub.publish(self.twist)
		        	rospy.loginfo('Stop!')

		    	else:
				self.twist.linear.x = 0.05
				self.twist.angular.z = 0.0
				rospy.loginfo('distance of the obstacle : %f', min(self.scan_filter))

		    	self._cmd_pub.publish(self.twist)
			time.sleep(0.1)


class Feux(threading.Thread):
	def __init__(self):
		threading.Thread.__init__(self)
		
		self.LIDAR_ERR = 0.05
		self._cmd_pub = rospy.Publisher('/cmd_vel', Twist, queue_size=1)
		#self.obstacle()
		self.etat = False
		self.precedent = False
	

    	def run(self):
		#self.etat = True
		connexion_principale = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		connexion_principale.bind((hote, port))
		connexion_principale.listen(5)
	

		connexion_avec_client, infos_connexion = connexion_principale.accept()

		msg_recu = ""
	
		self.twist = Twist()
		var = 0 # For traffic lights first state (green) purposes 
		while not msg_recu == "fin":
		    msg_recu = connexion_avec_client.recv(1024)
		    
		    rospy.loginfo(msg_recu)
		    connexion_avec_client.send("5 / 5")	
		    
		    self.scan_filter = []
		    
		    if msg_recu == "rouge":
			self.etat = False
			self.precedent = True
			time.sleep(0.5)
                	self.twist.linear.x = 0.0
                	self.twist.angular.z = 0.0
                	self._cmd_pub.publish(self.twist)
			self.precedent = False
		    elif msg_recu == "vert":        	
			self.etat = True
			if var != 0 : 
				self.precedent = False
				rospy.loginfo('Feu vert 2!')
                	
			else : # 1ere fois vert
				self.precedent = True
				var = 1
				rospy.loginfo('Feu vert 1!')
		    

def main():
    	rospy.init_node('turtlebot3_obstacle')
	try :			   
		feux = Feux()
		obstacle = Obstacle()
		obstacle.start()
		feux.start()

		while (1) :
			if feux.precedent == True :
				if feux.etat == False : #Lights are actually red and before they are green
					rospy.loginfo('11') 
					obstacle.pause()
					feux.precedent = False
	
			elif feux.etat == True and feux.precedent == False : #Lights are actually green or orange and before they are red
				rospy.loginfo('22')
				obstacle.resume()
				feux.precedent = True
	except rospy.ROSInterruptException:
        	pass
	

if __name__ == '__main__':
	main()
