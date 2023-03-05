import com.neuronrobotics.bowlerkernel.Bezier3d.*;

import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase

import eu.mihosoft.vrl.v3d.*
import eu.mihosoft.vrl.v3d.parametrics.*


return new ICadGenerator(){

	@Override
	public ArrayList<CSG> generateCad(DHParameterKinematics arg0, int arg1) {
		// TODO Auto-generated method stub
		ArrayList<CSG> back =[]
		back.add(new Cube(1).toCSG())
		for(CSG c:back)
			c.setManipulator(arg0.getLinkObjectManipulator(arg1))
		return back;
	}

	@Override
	public ArrayList<CSG> generateBody(MobileBase arg0) {
		// Initialize an empty ArrayList to hold the generated CSG objects
		ArrayList<CSG> back =[]
		
		// Define URL link to the GitHub repo for this robot
		def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"
		
		// define the parameters for the flat stock material
		//LengthParameter boardThickness = new LengthParameter('Board Thickness (mm)', 3, [0, 100])			// laser cut wood thickness
		LengthParameter boardThickness = new LengthParameter('Board Thickness (mm)', 19, [0, 100])			// 4'x8' plywood sheets
		boardThickness.setMM(19)
		
		// define the parameters for the overall size of the bay itself
		LengthParameter bayDepth = new LengthParameter("Bay Depth (mm)", 400, [0, 1000])
		//bayDepth.setMM(379.5)
		//bayDepth.setMM(390)
		bayDepth.setMM(400)
		//bayDepth.setMM(420)
		//bayDepth.setMM(440)
		LengthParameter bayWidth = new LengthParameter("Bay Width (mm)", 400, [0, 1000])
		bayWidth.setMM(400)
		LengthParameter bayHeight = new LengthParameter("Bay Height (mm)", 1300, [0, 5000])
		bayHeight.setMM(1400)
		
		// define the parameters for the shelf that holds the plant
		def armBezierPieces = 12
		LengthParameter bucketDiameter = new LengthParameter("Bucket Diameter (mm)", 304.8, [0, 1000])
		bucketDiameter.setMM(304.8)
		//LengthParameter bucketDistFromWall = new LengthParameter("Bucket Distance From Wall (mm)", 75, [0, 1000])
		//bucketDistFromWall.setMM(75)
		
		// define the parameters for the monorail track shelf
		LengthParameter railElevation = new LengthParameter("Rail Elevation (mm)", 600, [0, 1000])
		railElevation.setMM(600)
		LengthParameter trackDistFromWall = new LengthParameter("Track Distance from Wall (mm)", 25, [0, 1000])
		trackDistFromWall.setMM(25)
		
		// define the parameters for the monorail linear gears
		//
		
		// define the parameters for the construction screw holes
		LengthParameter screwDiameter = new LengthParameter("Screw Hole Diameter (mm)", 3, [0, 20])
		//screwDiameter.setMM(3)														// construction correct
		screwDiameter.setMM(10)															// temporary, for visualization
		LengthParameter screwSpacing = new LengthParameter("Distance Between Construction Screws (mm)", 150, [0, 400])
		screwSpacing.setMM(150)
		
		CSG plantShelf = new Cube(bayDepth.getMM()/2, bayWidth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayDepth.getMM()/4)
			.movey(0)
			.movez(boardThickness.getMM()/2)
			.rotz(90)
		CSG bucketGhost = new Cylinder(bucketDiameter.getMM()/2,boardThickness.getMM(), (int) 40).toCSG()
		plantShelf = plantShelf.difference(bucketGhost)
		
		// Define the port & starboard plantShelf arms, used to guide the bucket into place
		def armDepth = bayDepth.getMM()/3
		def armWidth = (bayWidth.getMM()-bucketDiameter.getMM()) / 2
		
		// Define a possible rectangular arm geometry
		CSG armRect = new Cube(armWidth, armDepth, boardThickness.getMM()).toCSG() // rectangle is temporary
			.movex(bucketDiameter.getMM()/2+armWidth/2)
			.movey(armDepth/2)
			.movez(boardThickness.getMM()/2)
			
		// Define a possible arm geometry along a bezier curve
		BezierEditor armBezierEditor = new BezierEditor(ScriptingEngine.fileFromGit(URL, "armBez.json"),armBezierPieces)
		armBezierEditor.setStart(bucketDiameter.getMM()/2, 0, 0)
		armBezierEditor.setEnd(bayWidth.getMM()/2, armDepth, 0)
		//armBez.setCP1(bucketDiameter.getMM()/2-10, armDepth, 0)			//Used to reset control point before manually tweaking - JMS, Feb 2023
		//armBez.setCP2(bucketDiameter.getMM()/2, armDepth+10, 0)
		ArrayList<Transform> armTrans = armBezierEditor.transforms()
		ArrayList<CSG> armCurve = armBezierEditor.getCSG()
		 List<Vector3d> armPoly = [new Vector3d(armRect.getMaxX(), armRect.getMinY(),0), new Vector3d(armRect.getMinX(),0,0)]
		for(Transform trans : armTrans) {
			armPoly.add(new Vector3d(trans.getX(),trans.getY(),trans.getZ()))
		}
		CSG armBez = Extrude.points(new Vector3d(0,0,boardThickness.getMM()),armPoly)
		
		// Use either the rectangular arms or the bezier guided arms
		CSG armShelfPort = armBez
		CSG armShelfStarboard = armShelfPort.mirrorx()
		
		plantShelf = plantShelf.union(armShelfPort)
		plantShelf = plantShelf.union(armShelfStarboard)
		
		ArrayList<CSG> fasteners = []
		// 
		ArrayList<CSG> returned = addTabs(plantShelf, Vector3d.X_ONE, screwDiameter);
		plantShelf = returned.get(0);
		fasteners.addAll(returned.subList(1, returned.size()));

		
		
//		def boardTrans = new Transform().rotz(90)
//		boardTemp = boardTemp.transformed(boardTrans)
//		plantShelf = plantShelf.transformed(boardTrans).union(addTabs(boardTemp))
//		
//		boardTrans = boardTrans.rotz(180)
//		boardTemp = boardTemp.transformed(boardTrans)
//		plantShelf = plantShelf.transformed(boardTrans).union(addTabs(boardTemp))
//		
//		boardTrans = boardTrans.rotz(0)
//		boardTemp = boardTemp.transformed(boardTrans)
//		plantShelf = plantShelf.transformed(boardTrans).union(addTabs(boardTemp))
		
//		plantShelf = plantShelf.transformed(boardTrans.inverse())
//		back.add(boardTemp.setColor(javafx.scene.paint.Color.GREEN))
//		back.add(tabTemp.setColor(javafx.scene.paint.Color.BLUE))
		
//		CSG railShelf = plantShelf.movez(railElevation.getMM())
		
		CSG portTrack = new Cube(trackDistFromWall.getMM(), bayDepth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()/2)
			.movey(0)
			.movez(railElevation.getMM())
		CSG starboardTrack = portTrack.mirrorx()
		CSG backTrack = new Cube(bayWidth.getMM(), trackDistFromWall.getMM(), boardThickness.getMM()).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2+trackDistFromWall.getMM()/2)
			.movez(railElevation.getMM())
		
		def turning_radius = 50		// minimum (or just stated) turning radius of the monorail
		CSG portTrackCircle = new Cylinder(turning_radius, boardThickness.getMM(), (int) 16 ).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()-turning_radius)
			.movey(-(bayDepth.getMM()/2-trackDistFromWall.getMM()-turning_radius))
			.movez(railElevation.getMM()-boardThickness.getMM()/2)
		CSG portTrackSquare = new Cube(turning_radius, turning_radius, boardThickness.getMM()).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()-turning_radius/2)
			.movey(-(bayDepth.getMM()/2-trackDistFromWall.getMM()-turning_radius/2))
			.movez(railElevation.getMM())
		CSG portTrackArc = portTrackSquare.difference(portTrackCircle)
		CSG starboardTrackArc = portTrackArc.mirrorx()
		
		CSG trackShelf = portTrack.union(starboardTrack).union(backTrack).union(portTrackArc).union(starboardTrackArc)
		/*
//		boardTemp = trackShelf
//		
//		boardTrans = new Transform().rotz(90)
//		boardTemp = boardTemp.transformed(boardTrans)
//		trackShelf = trackShelf.transformed(boardTrans).union(addTabs(boardTemp))
//		
//		boardTrans = boardTrans.rotz(180)
//		boardTemp = boardTemp.transformed(boardTrans)
//		trackShelf = trackShelf.transformed(boardTrans).union(addTabs(boardTemp))
//		
//		boardTrans = boardTrans.rotz(0)
//		boardTemp = boardTemp.transformed(boardTrans)
//		trackShelf = trackShelf.transformed(boardTrans).union(addTabs(boardTemp))
//		
//		trackShelf = trackShelf.transformed(boardTrans.inverse())
 * */
		
		CSG portWall = new Cube(boardThickness.getMM(),bayDepth.getMM(),bayHeight.getMM()/2).toCSG()
			.movex(bayWidth.getMM()/2 + boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/4)
		portWall = portWall.difference(plantShelf)
		portWall = portWall.difference(trackShelf)
		CSG starboardWall = portWall.mirrorx()
		
		CSG backWall = new Cube(bayWidth.getMM()+boardThickness.getMM()*2, boardThickness.getMM(), bayHeight.getMM()/2).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2-boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/4)
		backWall = backWall.difference(plantShelf)
		backWall = backWall.difference(trackShelf)
		
		// Adding screw holes to the back wall thru the port & starboard walls
//		CSG screwHole_home = new Cylinder(	screwDiameter.getMM()/2, // Radius
//									  boardThickness.getMM()*2+1, // Height
//									  (int)6 //resolution
//									  ).toCSG()//convert to CSG to display
//									  .roty(180)
//		screwHole_home = screwHole_home.movez(-screwHole_home.getMinZ()/2)
//		Transform screwTrans = new Transform()
//									.rotx(90)
//									.movex(bayWidth.getMM()/2+boardThickness.getMM()/2)
//									.movey(-bayDepth.getMM()/2-boardThickness.getMM())
//									.movez(screwSpacing.getMM()/2)
//		CSG screwHole =  screwHole_home.transformed(screwTrans)
//		CSG screwHoleSet = screwHole
//		for(double alt = screwSpacing.getMM()/2; alt < backWall.getMaxZ(); alt = alt + screwSpacing.getMM()) {
//			screwHole = screwHole.movez(screwSpacing.getMM())
//			screwHoleSet = screwHoleSet.union(screwHole)
////			println "backWall.getMaxZ() is ${backWall.getMaxZ()}, and alt is ${alt}"
//		}
//		screwHoleSet = screwHoleSet.union(screwHoleSet.mirrorx())
//		backWall = backWall.difference(screwHoleSet)
//		
//		screwTrans = new Transform()
//								.rotx(90)
//								.movex(boardThickness.getMM()*3)
//								.movey(-bayDepth.getMM()/2-boardThickness.getMM())
//								.movez(boardThickness.getMM()/2)
//		screwHole =  screwHole_home.transformed(screwTrans)
//		screwHoleSet = screwHole
//		for(double wid = bayWidth.getMM()/2-boardThickness.getMM(); screwHole.getCenterX() < backWall.getMaxX(); wid = wid + boardThickness.getMM()*6) {
//			screwHoleSet = screwHoleSet.union(screwHole)
//			println "screwHole.getCenterX() is ${screwHole.getCenterX()}"
//			screwHole = screwHole.movex(boardThickness.getMM()*6)
//		}
//		screwHoleSet = screwHoleSet.union(screwHoleSet.mirrorx())
//		back.add(screwHoleSet)
		//backWall = backWall.difference(screwHoleSet)
		
		
		// Assign each component an assembly step, for the exploded view visualization
		plantShelf.addAssemblyStep(1, new Transform())
		trackShelf.addAssemblyStep(2, new Transform().movez(100))
		backWall.addAssemblyStep(3, new Transform().movey(-50))
		portWall.addAssemblyStep(3, new Transform().movex(50))
		starboardWall.addAssemblyStep(3, new Transform().movex(-50))
		
		// Add colored components to returned list, for rendering
		back.add(plantShelf.setColor(javafx.scene.paint.Color.MAGENTA))
		back.add(trackShelf.setColor(javafx.scene.paint.Color.RED))
		back.add(backWall.setColor(javafx.scene.paint.Color.BLUE))
		back.add(portWall.setColor(javafx.scene.paint.Color.CYAN))
		back.add(starboardWall.setColor(javafx.scene.paint.Color.TEAL))
		back.addAll(fasteners)
		
		
		for(CSG c:back)
			c.setManipulator(arg0.getRootListener())
			
		for(DHParameterKinematics kin:arg0.getAllDHChains()) {
			CSG limbRoot =new Cube(1).toCSG()
			limbRoot.setManipulator(kin.getRootListener())
			back.add(limbRoot)

		}
		//back.addAll(armCurve)	//			Uncomment to show and edit the bezier arm curve - JMS, Feb 2023
		
		return back;
	}

	
//	/**
//	 * Adds construction tabs along the X axis, on the side that has the most negative Y value (i.e. at the "bottom" of the piece, in the XY plane).
//	 * Assumes Z can be arbitrary but uniform height.
//	 * Assumes the edge having tabs added extends fully between MinX and MaxX.
//	 * 
//	 * Example usage:
//	 * 	// Copy the target object to a temporary object which never has tabs added, so that the new tabs do not impact MinX and MaxX
//	 *	CSG boardTemp = boardObj
//	 * 	
//	 * 	// Define a transform, which brings the first edge to be tabbed along the X axis, on the side that has the most negative Y value
//	 * 	def boardTrans = new Transform().rotz(90)
//	 * 
//	 * 	// Apply the transform to the temporary object
//	 * 	boardTemp = boardTemp.transformed(boardTrans)
//	 * 
//	 * 	// Apply the same transform to the target object, then add tabs to the target object using the temporary object as input
//	 * 	boardObj = boardObj.transformed(boardTrans).union(addTabs(boardTemp))
//	 * 	
//	 * 	// Modify the existing transform to select a new edge
//	 * 	boardTrans = boardTrans.rotz(180)
//	 * 
//	 * 	// Apply this transform to the temporary object
//	 * 	boardTemp = boardTemp.transformed(boardTrans)
//	 * 
//	 * 	// Apply the same transform to the target object, then add tabs to the target object using the temporary object as input
//	 * 	boardObj = boardObj.transformed(boardTrans).union(addTabs(boardTemp))
//	 * 
//	 * 	// Automatically undo all transformations on the target object
//	 * 	boardObj = boardObj.transformed(boardTrans.inverse())
//	 * 
//	 * 
//	 *
//	 * @param boardTemp the CSG object to add tabs to
//	 * @return the modified CSG object with tabs added
//	 */
//	private ArrayList<CSG> addTabs(CSG boardTemp, Vector3d edgeDirection, LengthParameter screwDiameter) {
//		
//	    // Translate the boardTemp object so that its minimum corner is at the origin
//	    def tabTrans = new Transform().toXMin().toYMin().toZMin()
//	    boardTemp = boardTemp.transformed(tabTrans)
//	    
//	    // Define the size of the tabs and the distance between tab cycles
//	    def tabSize = boardTemp.getMaxZ() * 2
//	    def cycleSize = tabSize * 3
//	    
//	    // Determine the minimum buffer space between the edge of the board and the tabs
//	    def minBuffer = boardTemp.getMaxZ()
//	    
//	    // Create a temporary CSG object for a single tab
//	    CSG tabTemp = new Cube(tabSize, boardTemp.getMaxZ(), boardTemp.getMaxZ()).toCSG()
//	    
//	    // Position the temporary tab object at the first tab location
//	    tabTemp = tabTemp.movex(tabTemp.getMaxX())
//	                     .movey(-tabTemp.getMaxY() + boardTemp.getMinY())
//	                     .movez(tabTemp.getMaxZ())
//	    
//	    // Calculate the clearance beyond the outermost tabs, equal on both sides and never more than minBuffer
//	    def bufferVal = (boardTemp.getMaxX() - (tabSize + cycleSize * iterNum)) / 2
//	    
//		ArrayList<CSG> result = new ArrayList<CSG>()
//		result.add(boardTemp)
//		
//		double holeRadius = screwDiameter.getMM() / 2.0
//		double holeDepth = boardTemp.getMaxZ()
//		
//		Vector3d holeDirection = null
//		
//		if (edgeDirection.equals(Vector3d.X_ONE)) {
//		    holeDirection = Vector3d.Y_ONE;
//		} else if (edgeDirection.equals(Vector3d.Y_ONE)) {
//		    holeDirection = Vector3d.X_ONE;
//		} else if (edgeDirection.equals(Vector3d.Z_ONE)) {
//		    holeDirection = Vector3d.X_ONE;
//		} else if (edgeDirection.equals(Vector3d.X_ONE.negated())) {
//		    holeDirection = Vector3d.X_ONE;
//		} else if (edgeDirection.equals(Vector3d.Y_ONE.negated())) {
//		    holeDirection = Vector3d.X_ONE;
//		} else if (edgeDirection.equals(Vector3d.Z_ONE.negated())) {
//		    holeDirection = Vector3d.X_ONE;
//		} else {
//		    throw new Exception("Invalid edge direction");
//		}
//
//		
//		if (holeDirection != null) {
//			double holeX = (boardTemp.getMaxX() - holeDepth) / 2.0
//			double holeY = (boardTemp.getMinY() + boardTemp.getMaxY()) / 2.0
//			double holeZ = boardTemp.getMinZ()
//			
//			Transform holeTrans = new Transform().translate(holeX, holeY, holeZ)
//			
//			CSG screwHole = new Cylinder(holeRadius, holeDepth, holeDirection).toCSG().transformed(holeTrans)
//			
//			result.add(screwHole)
//			
//			int numHoles = (int)Math.floor((boardTemp.getMaxX() - holeDepth) / (screwDiameter.getValue() * 2))
//			
//			double spacing = (boardTemp.getMaxX() - holeDepth) / (numHoles + 1)
//			
//			for (int i = 1; i <= numHoles; i++) {
//				double holeXPos = holeX + spacing * i
//				holeTrans = new Transform().translate(holeXPos, holeY, holeZ)
//				screwHole = new Cylinder(holeRadius, holeDepth, holeDirection).toCSG().transformed(holeTrans)
//				result.add(screwHole)
//			}
//		}
//	    
//	    // Calculate the number of full tab-space cycles to add, not including the first tab
//	    def iterNum = (boardTemp.getMaxX() - tabSize - minBuffer*2) / cycleSize
//	    iterNum = Math.floor(iterNum) // Round down to ensure an integer value
//	    
//	    // Add the desired number of tabs at regular intervals
//	    for(int i=0; i<=iterNum; i++) {
//	        double xVal = bufferVal + i * cycleSize
//	        boardTemp = boardTemp.union(tabTemp.movex(xVal))
//	    }
//	    
//	    // Translate the boardTemp object back to its original position
//	    boardTemp = boardTemp.transformed(tabTrans.inverse())
//		
//	    return result
//	}

	
	/**
	 * Adds construction tabs along the X axis, on the side that has the most negative Y value (i.e. at the "bottom" of the piece, in the XY plane).
	 * Assumes Z can be arbitrary but uniform height.
	 * Assumes the edge having tabs added extends fully between MinX and MaxX.
	 * 
	 * Example usage:
	 * 	// Copy the target object to a temporary object which never has tabs added, so that the new tabs do not impact MinX and MaxX
	 *	CSG boardTemp = boardObj
	 * 	
	 * 	// Define a transform, which brings the first edge to be tabbed along the X axis, on the side that has the most negative Y value
	 * 	def boardTrans = new Transform().rotz(90)
	 * 
	 * 	// Apply the transform to the temporary object
	 * 	boardTemp = boardTemp.transformed(boardTrans)
	 * 
	 * 	// Apply the same transform to the target object, then add tabs to the target object using the temporary object as input
	 * 	boardObj = boardObj.transformed(boardTrans).union(addTabs(boardTemp))
	 * 	
	 * 	// Modify the existing transform to select a new edge
	 * 	boardTrans = boardTrans.rotz(180)
	 * 
	 * 	// Apply this transform to the temporary object
	 * 	boardTemp = boardTemp.transformed(boardTrans)
	 * 
	 * 	// Apply the same transform to the target object, then add tabs to the target object using the temporary object as input
	 * 	boardObj = boardObj.transformed(boardTrans).union(addTabs(boardTemp))
	 * 
	 * 	// Automatically undo all transformations on the target object
	 * 	boardObj = boardObj.transformed(boardTrans.inverse())
	 * 
	 * 
	 *
	 * @param boardTemp the CSG object to add tabs to
	 * @return the modified CSG object with tabs added
	 */
	private ArrayList<CSG> addTabs(CSG boardTemp, Vector3d edgeDirection, LengthParameter screwDiameter) {
		
		ArrayList<CSG> result = []
		
	    // Translate the boardTemp object so that its minimum corner is at the origin
	    def tabTrans = new Transform().movex(-boardTemp.getMinX()).movey(-boardTemp.getMinY()).movez(-boardTemp.getMinZ())
	    boardTemp = boardTemp.transformed(tabTrans)
	    
	    // Define the size of the tabs and the distance between tab cycles
	    def tabSize = boardTemp.getMaxZ() * 2
	    def cycleSize = tabSize * 3
	    
	    // Determine the minimum buffer space between the edge of the board and the tabs
	    def minBuffer = boardTemp.getMaxZ()
	    
	    // Create a temporary CSG object for a single tab
	    CSG tabTemp = new Cube(tabSize, boardTemp.getMaxZ(), boardTemp.getMaxZ()).toCSG()
	    
	    // Position the temporary tab object at the first tab location
	    tabTemp = tabTemp.movex(tabTemp.getMaxX())
	                     .movey(-tabTemp.getMaxY() + boardTemp.getMinY())
	                     .movez(tabTemp.getMaxZ())
	    
	    // Calculate the number of full tab-space cycles to add, not including the first tab
	    def iterNum = (boardTemp.getMaxX() - tabSize - minBuffer*2) / cycleSize
	    iterNum = Math.floor(iterNum) // Round down to ensure an integer value
	    
	    // Calculate the clearance beyond the outermost tabs, equal on both sides and never more than minBuffer
	    def bufferVal = (boardTemp.getMaxX() - (tabSize + cycleSize * iterNum)) / 2
	    
	    // Add the desired number of tabs at regular intervals
	    for(int i=0; i<=iterNum; i++) {
	        double xVal = bufferVal + i * cycleSize
	        boardTemp = boardTemp.union(tabTemp.movex(xVal))
	    }
	    
	    // Translate the boardTemp object back to its original position
	    boardTemp = boardTemp.transformed(tabTrans.inverse())
		
		result.add(boardTemp)
	    
	    return result
	}

	
	//public CSG addSlots
	
	
}
