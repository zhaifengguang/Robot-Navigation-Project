//This class makes an object for the robot
package RobotDir;
import java.lang.Math.*;
import Structure.*;
import java.util.*;


public class Robot{

	//Robot characteristics
	public double height;
	public double width;
	public double speed;
	public double turnspeed;
	public Sensor mainSensor;
	public double range;

	//Robot position variables
	public Coordinate center;
	public double theta;
	public Coordinate corners[] = new Coordinate[4];
	public LineSeg edges[] = new LineSeg[4];
	public Coordinate newSense[];
	public ArrayList<Coordinate> steps;
	public ArrayList<Double> angles;
	
	//Environment variables
	ArrayList<Landmark> landmarks;
	Coordinate goalPos;
	
	//Navigate Variables
	public double turnRange;
	public long its;

	public Robot(double x, double y, double theta, double height, double width, double speed, double turnspeed, ArrayList<Landmark> landmarks, Coordinate GoalPos) {
		center = new Coordinate(x,y);
		this.height = height;
		this.width = width;
		this.speed = speed;
		this.theta = theta;
		this.turnspeed = turnspeed;
		this.goalPos = goalPos;		
		updateCorners();
	}

	//res needs to be odd,
	public Robot(double x, double y, double theta, double height, double width, double speed, double turnspeed, double range, double angleRange, double facing, Coordinate pos, double res, ArrayList<Landmark> landmarks, Coordinate goalPos) {
		center = new Coordinate(x,y);
		this.height = height;
		this.width = width;
		this.speed = speed;
		this.theta = theta;
		this.landmarks = landmarks;
		this.turnspeed = turnspeed;
		this.goalPos = goalPos;
		this.range = range;
		updateCorners();
		newSense = new Coordinate[(int)res];
		mainSensor = new Sensor(range, angleRange, facing, pos, res, landmarks);
		steps = new ArrayList();
		angles = new ArrayList();
	}	
	
	//updates the corners of the robot based on the center position and the angle
	public void updateCorners() {
		//direct is the straight line from center to corner
		double direct = Math.sqrt((height/2)*(height/2)+(width/2)*(width/2));
		//dtheta is the angle of this line
		double dtheta = theta+Math.PI/4;
		
		//calculates the coordinate of that corner and then increments the angle by 90
		for(int i = 0; i < 4;i++){		
			corners[i] = new Coordinate(center.x+Math.cos(dtheta)*direct,center.y+Math.sin(dtheta)*direct);
			dtheta +=Math.PI/2;			
		}
		
		updateEdges();
	}
	
	//updates the edges based on the corners
	public void updateEdges() {

		for(int i = 0; i < 3;i++){		
			edges[i] = new LineSeg(corners[i],corners[i+1]);
		}
		edges[3] = new LineSeg(corners[3],corners[0]);
		
	}	
	
	//updates sensor array
	public void readSensor(){
		mainSensor.sense(center,theta,newSense);
	}
	
	//rounds numbers
	public double round(double num){
		if(num < 0.000001 && num > -0.000001) num = 0;
		return num;
	}
	
	//checks if it is hitting any landmarks
	public boolean checkContact(){
		LineSeg temp;
		for(int i =0;i<4;i++){
			temp = edges[i];
			for(int j = 0; j < landmarks.size(); j++){
				Landmark mark = landmarks.get(j); //the pointer for the specific landmark
				
				//checks every lineseg
				for(int k = 0; k < mark.lineSegList.size();k++){
					LineSeg line = mark.lineSegList.get(k); //the pointer for specific lineseg	
					
					Coordinate intersect = temp.checkCross(line);
					if(intersect.exists) return true;
				}
			}
		}
		return false;
	}

 	//rotates robot incrementally towards given heading; returns true when there
	public boolean rotate(double heading){
		double tSpeed;
		boolean there;
		if(Math.abs(heading-theta) < 0.00001){
			return true;
		}
		
		if(heading<theta){
			tSpeed=turnspeed*-1;
		}
		else tSpeed=turnspeed;
		
		if(Math.abs(theta-heading)<=turnspeed){
			theta=heading;
			there =true;
		}
		else {
			theta+=tSpeed;
			if(theta>Math.PI*2) theta = theta - (Math.PI*2);
			there = false;
		}
		updateCorners();
		return there;
	}
	
	//increments robot motion towards the destination; returns true when there
	public boolean move(Coordinate dest){
		boolean there;
		double xSpeed = round(speed*Math.cos(theta));
		double ySpeed = round(speed*Math.sin(theta));		
		
		if(((Math.abs(dest.x-center.x)-Math.abs(xSpeed))<0.00001) && ((Math.abs(dest.y-center.y)-Math.abs(ySpeed))<0.00001)){
			center.x = dest.x;
			center.y = dest.y;
			there = true;
		}
		else{
			center.x += xSpeed;
			center.y += ySpeed;
			there = false;
		}
		updateCorners();
		return there;
	}
	
 	//takes the robot to a position
	public boolean goPos(Coordinate point){ //coordinate is relative to robot
		if(distance(center,point)<0.00001) return true;
		double heading = Math.atan2((point.y-center.y),(point.x-center.x));
		boolean there = false;
		while(!there){
			there = rotate(heading);
			save();
			if(checkContact()) return false;
		}
		there = false;
		if(distance(center,point)<0.00001) return true;
		while(!there){
			there = move(point);
			save();			
			if(checkContact()) return false;
		}
		return true;
	} 
	
	public boolean reverse(Coordinate point){
		if(distance(center,point)<0.00001) return true;
		theta = Math.atan2((point.y-center.y),(point.x-center.x));

		boolean there = false;
		while(!there){
			there = move(point);
			save();
			if(checkContact()) return false;
		}
		updateCorners();
		return true;		
	}

	public boolean turnDes(){
		double heading = Math.atan2((goalPos.y-center.y),(goalPos.x-center.x));
		boolean there = false;
		if(distance(center,goalPos)<0.00001) return true;
		while(!there){
			there = rotate(heading);
			if(checkContact()) return false;
		}
		return true;
	} 
	
	//recursive this
 	public Coordinate nextPos(){
		readSensor();
		Coordinate next = newSense[(int)(newSense.length/2)];
		Coordinate current = newSense[0];
		int index = (int)(newSense.length/2);
		for(int i = 0; i < (int)(newSense.length/2); i++){
			for(int k = 0; k<=1;k++){
				if(k == 0)index = newSense.length - i-1;
				if(k == 1)index = i;
				current = newSense[index];
				double cDis = distance(center,current);
				double nDis = distance(center,next);
				if(cDis > nDis || Math.abs(cDis-nDis)<0.00001){
					next = current;
				} 
			
			}
		}
		return next;
	} 
	

	public boolean iterate(Coordinate locate){
		its++;

		if(distance(center,goalPos)<0.00001) return true;
		
		if(!goPos(locate)){
			System.out.println("here5");
			return false;
		} 
		if(!turnDes()){
			System.out.println("here6");			
			return false;
		}	
 
		readSensor();

				
		if(distance(newSense[(int)(newSense.length/2)],center) > distance(goalPos,center)){
			System.out.println("here2");
			return goPos(new Coordinate(goalPos.x,goalPos.y));
		} 
		
		Coordinate current = newSense[(int)(newSense.length/2)];
		int index = (int)(newSense.length/2);
		for(int i = 0; i < (int)(newSense.length/2)+1; i++){
			for(int k = 0; k<=1;k++){
				if(k == 0)index = (int)(newSense.length/2) - i;
				if(k == 1)index = (int)(newSense.length/2) + i;
				current = newSense[index];
				double cDis = distance(locate,current);
//				System.out.println("current: "+current.x+"\t"+current.y+"\t"+cDis);
				if(Math.abs(cDis-range)<0.00001){
					System.out.println("here3");
					if(iterate(current)){
						System.out.println("here4");
						return true;
					}
					else{
						reverse(locate);
						turnDes();
					}
				} 				
			}
		}		
		System.out.println("here1");
		return false;
	}
	
 	public double distance(Coordinate c1, Coordinate c2){
		LineSeg length = new LineSeg(c1,c2);
		return length.getMagnitude();		
	}
	
	public void navigate(){
		iterate(center);

	}
	
	public void save(){
		Coordinate temp = new Coordinate(center.x, center.y);
		steps.add(temp);
		angles.add(theta);
	}
	
}