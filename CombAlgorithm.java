//java -jar ../algtest.jar circuit.props
import java.lang.*;
import java.util.*;

import edu.gwu.geometry.*;
import edu.gwu.algtest.*;
import edu.gwu.util.*;
import edu.gwu.debug.*;


public class CombAlgorithm implements CircuitRoutingAlgorithm {

    public static void main(String[] args) {
        CombAlgorithm alg = new CombAlgorithm();
        IntRectangle region = new IntRectangle(0, 5, 5, 0);
        IntPoint[] points = new IntPoint[6];
        points[0] = new IntPoint(2, 4);
        points[1] = new IntPoint(2, 2);
        points[2] = new IntPoint(4, 2);
        points[3] = new IntPoint(7, 2);
        points[4] = new IntPoint(7, 4);
        points[5] = new IntPoint(9, 7);
        IntRectangle[] obstacles = new IntRectangle[6];
        obstacles[0] = new IntRectangle(0, 4, 1, 0);
        obstacles[1] = new IntRectangle(1, 8, 2, 7);
        obstacles[2] = new IntRectangle(3, 8, 4, 4);
        obstacles[3] = new IntRectangle(5, 5, 6, 2);
        obstacles[4] = new IntRectangle(5, 9, 6, 6);
        obstacles[5] = new IntRectangle(7, 9, 8, 5);

        alg.buildCircuit(region, points, obstacles);

    }

    LinkedList<IntLineSegment> lineList;
    IntLineSegment[] leftmostLine;
    public IntLineSegment[] buildCircuit(IntRectangle region, IntPoint[] points, IntRectangle[] obstacles){
        lineList = new LinkedList<IntLineSegment>();

        //Find the leftmost point, the highest point, and the lowest point
        IntPoint leftmostPoint = new IntPoint(Integer.MAX_VALUE, 0);
        IntPoint highestPoint = new IntPoint(0, Integer.MIN_VALUE);
        IntPoint lowestPoint = new IntPoint(0, Integer.MAX_VALUE);
        for (int i=0; i<points.length; i++){
            if(points[i].x < leftmostPoint.x) leftmostPoint = points[i];
            if(points[i].y > highestPoint.y) highestPoint = points[i];
            if(points[i].y < lowestPoint.y) lowestPoint = points[i];
        }

        //Extend the circuit wire across vertically on the leftmost point,
        // from the height of the highest point to the lowest point
        IntLineSegment line;
        if( highestPoint.y != lowestPoint.y ){
            line = new IntLineSegment(leftmostPoint.x, highestPoint.y, leftmostPoint.x, lowestPoint.y);
            //Creates the leftmost line(s) in the circuit
            insertVertConnection(line, obstacles);
        }
        
        //Connect the rest of the points to the leftmost line
        IntLineSegment heightLine = null;
        for (int i=0; i<points.length; i++){
            //check which line on leftmostline has the same y value as the end point
            if(leftmostLine != null){
                for(int j=0; j<leftmostLine.length; j++){
                    if( (leftmostLine[j].start.y >= points[i].y) && (leftmostLine[j].end.y <= points[i].y) ){
                        heightLine = leftmostLine[j];
                        
                    }
                }
                //If the point is not on the heightline, insert connection. Otherwise move to the next iteration
                if(heightLine.start.x != points[i].x){
                    line = new IntLineSegment(new IntPoint(heightLine.start.x, points[i].y), points[i]);
                    insertHorzConnection(line, obstacles);
                    //lineList.add(line);
                    //System.out.println("Added new " + line);
                }
            } else {
                //Special case: if the points are along the same y axis
                if(points[i] != leftmostPoint){
                    line = new IntLineSegment(leftmostPoint, points[i]);
                    insertHorzConnection(line, obstacles);
                }
            }
        }

        //Create the line segment array 
        IntLineSegment[] lineArray = new IntLineSegment[lineList.size()];
        for(int i=0; i<lineList.size(); i++){
            lineArray[i] = lineList.get(i);
            //System.out.println("Final Line Array: " + lineArray[i]);
        }
        return lineArray;
    }

    public void insertHorzConnection(IntLineSegment line, IntRectangle[] obstacles){
        ArrayList<IntRectangle> obstructingObs = new ArrayList<IntRectangle>(); 
        if(obstacles != null){
            //Checks if line would intersect with obsticles
            for(int i=0; i<obstacles.length; i++){
                if( checksHorzIntersect(line, obstacles[i]) ){
                    obstructingObs.add(obstacles[i]);
                } 
            }
        }
        //If there are no obstacles add the line as it is
        if(obstructingObs.size() == 0){
            lineList.add(line);
        } else {
            int minDistance;
            int distance;
            int index;
            IntRectangle closestObstacle;
            IntPoint currentPoint = line.start;
            while(obstructingObs.size() != 0){
                minDistance = Integer.MAX_VALUE;
                closestObstacle = null;
                index = 0;
                distance = 0;
                //Finds the nearest obsticle to move around
                for(int i=0; i<obstructingObs.size(); i++){
                    distance = obstructingObs.get(i).topLeft.x - line.start.x;
                    if(distance < minDistance) {
                        minDistance = distance;
                        closestObstacle = obstructingObs.get(i);
                        index = i;
                    }
                }
                //Function that moves around the obstacle
                currentPoint = moveAroundObstacleHorz(currentPoint, closestObstacle);
                //Deletes obstacle from array list
                obstructingObs.remove(index);
                //Repeat: New point, next closest obstacle
            }
            //Add the final line to the end of the horizontal line
            lineList.add(new IntLineSegment(currentPoint, line.end));
        }
    }

    public void insertVertConnection(IntLineSegment line, IntRectangle[] obstacles){
        ArrayList<IntRectangle> obstructingObs = new ArrayList<IntRectangle>(); 
        if(obstacles != null){
            //Checks if leftmost line would intersect with obsticles
            for(int i=0; i<obstacles.length; i++){
                if( checksVertIntersect(line, obstacles[i]) ) obstructingObs.add(obstacles[i]);
            }
        }
        //If there are no obstacles just add the leftmost line as it is.
        if(obstructingObs.size() == 0){
            lineList.add(line);
            leftmostLine = new IntLineSegment[1];
            leftmostLine[0] = line;
        } else {
            int minDistance;
            int distance;
            int index;
            IntRectangle closestObstacle;
            IntPoint currentPoint = line.start;
            while (obstructingObs.size() != 0){
                minDistance = Integer.MAX_VALUE;
                closestObstacle = null;
                index = 0;
                distance = 0;
                //Finds nearest obstacle to move around
                for(int i=0; i<obstructingObs.size(); i++){
                    distance = line.start.y - obstructingObs.get(i).topLeft.y;
                    if(distance < minDistance) {
                        minDistance = distance;
                        closestObstacle = obstructingObs.get(i);
                        index = i;
                    }
                }
                //Function that moves around the obstacle
                currentPoint = moveAroundObstacleVert(currentPoint, closestObstacle);
                //Deletes obstacle from array list
                obstructingObs.remove(index);
                //Repeat: New point, next closest obstacle
            }
            //Add the final line to the end of the lefmostLine
            lineList.add(new IntLineSegment(currentPoint, line.end));
            //Give the global variable all the values that make up the left side of the comb
            leftmostLine = new IntLineSegment[lineList.size()];
            for(int i=0; i<lineList.size(); i++){
                leftmostLine[i] = lineList.get(i);
            }
        }
    }

    public IntPoint moveAroundObstacleHorz(IntPoint currentPoint, IntRectangle obstacle){
        IntLineSegment moveAcross;
        IntLineSegment moveVertically;
        IntLineSegment moveAcrossAgain;
        IntLineSegment moveBackVert;
        
        //Move across toward the obstacle
        
        if( currentPoint.x != obstacle.topLeft.x ){
            
            moveAcross = new IntLineSegment(currentPoint, new IntPoint(obstacle.topLeft.x, currentPoint.y));
            
            lineList.add(moveAcross);
        }
        
        //Determine whether to go up or down
        if( (obstacle.topLeft.y-currentPoint.y) < (currentPoint.y-obstacle.bottomRight.y) ){
            //If true move up
            moveVertically = new IntLineSegment(obstacle.topLeft.x, currentPoint.y, obstacle.topLeft.x, obstacle.topLeft.y);
            //Move across the side of the obstacle
            moveAcrossAgain = new IntLineSegment(obstacle.topLeft.x, obstacle.topLeft.y, obstacle.bottomRight.x, obstacle.topLeft.y);
            //Return to same y value
            moveBackVert = new IntLineSegment(obstacle.bottomRight.x, obstacle.topLeft.y, obstacle.bottomRight.x, currentPoint.y);
        } else {
            //Else move down
            moveVertically = new IntLineSegment(obstacle.topLeft.x, currentPoint.y, obstacle.topLeft.x, obstacle.bottomRight.y);
            //Move across the side of the obstacle
            moveAcrossAgain = new IntLineSegment(obstacle.topLeft.x, obstacle.bottomRight.y, obstacle.bottomRight.x, obstacle.bottomRight.y);
            //Return to same y value
            moveBackVert = new IntLineSegment(obstacle.bottomRight.x, obstacle.bottomRight.y, obstacle.bottomRight.x, currentPoint.y);
        }
        //Add lines to global linked List
        lineList.add(moveVertically);
        lineList.add(moveAcrossAgain);
        lineList.add(moveBackVert);

        //Return the next starting point
        return moveBackVert.end;
    }

    public IntPoint moveAroundObstacleVert(IntPoint currentPoint, IntRectangle obstacle){
        IntLineSegment moveDown;
        IntLineSegment moveAcross;
        IntLineSegment moveDownAgain;
        IntLineSegment moveBackAcross;
        //Move down toward the obstacle
        if( currentPoint.y != obstacle.topLeft.y ){
            moveDown = new IntLineSegment(currentPoint, new IntPoint(currentPoint.x, obstacle.topLeft.y));
            lineList.add(moveDown);
        }
        
        //Determine whether to go left or right
        if( (obstacle.bottomRight.x-currentPoint.x) < (currentPoint.x-obstacle.topLeft.x) ){
            //If true move right
            moveAcross = new IntLineSegment(currentPoint.x, obstacle.topLeft.y, obstacle.bottomRight.x, obstacle.topLeft.y);
            //Move down the side of the obstacle
            moveDownAgain = new IntLineSegment(obstacle.bottomRight.x, obstacle.topLeft.y, obstacle.bottomRight.x, obstacle.bottomRight.y);
            //Return to same x value
            moveBackAcross = new IntLineSegment(obstacle.bottomRight.x, obstacle.bottomRight.y, currentPoint.x, obstacle.bottomRight.y);
        } else {
            //Else move left
            moveAcross = new IntLineSegment(currentPoint.x, obstacle.topLeft.y, obstacle.topLeft.x, obstacle.topLeft.y);
            //Move down the side of the obstacle
            moveDownAgain = new IntLineSegment(obstacle.topLeft.x, obstacle.topLeft.y, obstacle.topLeft.x, obstacle.bottomRight.y);
            //Return to same x value
            moveBackAcross = new IntLineSegment(obstacle.topLeft.x, obstacle.bottomRight.y, currentPoint.x, obstacle.bottomRight.y);
        }
        //Add lines to global linked List
        lineList.add(moveAcross);
        lineList.add(moveDownAgain);
        lineList.add(moveBackAcross);

        //Return the next starting point
        return moveBackAcross.end;
    }

    public static boolean checksVertIntersect(IntLineSegment line, IntRectangle obstacle){
        boolean betweenXvals = false;
        if( (line.start.x > obstacle.topLeft.x) && (line.start.x < obstacle.bottomRight.x)) betweenXvals = true;
        boolean obstacleBetweenLine = false;
        if( (line.start.y >= obstacle.topLeft.y) && (line.end.y <= obstacle.topLeft.y) ) obstacleBetweenLine = true;
        if(betweenXvals && obstacleBetweenLine) return true;
        else return false;
    }

    public static boolean checksHorzIntersect(IntLineSegment line, IntRectangle obstacle){
        boolean betweenYvals = false;
        if( (line.start.y < obstacle.topLeft.y) && (line.start.y > obstacle.bottomRight.y) ) betweenYvals = true;
        boolean obstacleBetweenLine = false;
        if( (line.start.x <= obstacle.topLeft.x) && (line.end.x >= obstacle.bottomRight.x) ) obstacleBetweenLine = true;
        if(betweenYvals && obstacleBetweenLine) return true;
        else return false;
    }

    public String getName(){
        return "Capp-CombAlgorithm";
    }

    public void setPropertyExtractor(int algID, PropertyExtractor prop){

    }
}