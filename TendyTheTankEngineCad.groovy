import com.neuronrobotics.bowlerkernel.Bezier3d.*
import com.neuronrobotics.bowlerkernel.Bezier3d.BezierEditor

import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase

import eu.mihosoft.vrl.v3d.*
import eu.mihosoft.vrl.v3d.Vector3d
import eu.mihosoft.vrl.v3d.CSG
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
		def trackStraightBezierPieces = 6
		def trackCurveBezierPieces = 12
		LengthParameter railElevation = new LengthParameter("Rail Elevation (mm)", 600, [0, 1000])
		railElevation.setMM(600)
		LengthParameter trackDistFromWall = new LengthParameter("Track Distance from Wall (mm)", 25, [0, 1000])
		trackDistFromWall.setMM(40)
		
		// define the parameters for the monorail linear gears
		LengthParameter turningRadius = new LengthParameter("Minimum Turning Radius of the Monorail (mm)", 50, [0, 300])
		turningRadius.setMM(50)
		
		// define the parameters for the construction screw holes
		LengthParameter screwDiameter = new LengthParameter("Screw Hole Diameter (mm)", 3, [0, 20])
		screwDiameter.setMM(3)														// construction correct
		//screwDiameter.setMM(10)															// temporary, for visualization
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
		//armBezierEditor.setCP1(bucketDiameter.getMM()/2-10, armDepth, 0)			//Used to reset control point before manually tweaking - JMS, Feb 2023
		//armBezierEditor.setCP2(bucketDiameter.getMM()/2, armDepth+10, 0)			//Used to reset control point before manually tweaking - JMS, Feb 2023
		ArrayList<Transform> armTrans = armBezierEditor.transforms()
		ArrayList<CSG> armManips = armBezierEditor.getCSG()
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
		
		// Instantiate a bucket to hold fastener CSG objects in
		ArrayList<CSG> fasteners = []
		
		// Save plantShelf to a temporary CSG so that addTabs uses the correct edge lengths
		CSG plantShelfTemp = plantShelf
		
		// Add tabs to the Y- side
		ArrayList<CSG> returned = plantShelfTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		plantShelf = plantShelf.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Add tabs to the X+ side
		returned = plantShelfTemp.addTabs(new Vector3d(1, 0, 0), screwDiameter);
		plantShelf = plantShelf.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Add tabs to the X- side
		returned = plantShelfTemp.addTabs(new Vector3d(-1, 0, 0), screwDiameter);
		plantShelf = plantShelf.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		
		// Define a shelf for the monorail track using primitives
		CSG portTrack = new Cube(trackDistFromWall.getMM(), bayDepth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()/2)
			.movey(0)
			.movez(0)
		CSG starboardTrack = portTrack.mirrorx()
		CSG backTrack = new Cube(bayWidth.getMM(), trackDistFromWall.getMM(), boardThickness.getMM()).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2+trackDistFromWall.getMM()/2)
			.movez(0)
		CSG portTrackCircle = new Cylinder(turningRadius.getMM(), boardThickness.getMM(), (int) 16 ).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()-turningRadius.getMM())
			.movey(-(bayDepth.getMM()/2-trackDistFromWall.getMM()-turningRadius.getMM()))
			.movez(-boardThickness.getMM()/2)
		CSG portTrackSquare = new Cube(turningRadius.getMM(), turningRadius.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayWidth.getMM()/2-trackDistFromWall.getMM()-turningRadius.getMM()/2)
			.movey(-(bayDepth.getMM()/2-trackDistFromWall.getMM()-turningRadius.getMM()/2))
			.movez(0)
		CSG portTrackArc = portTrackSquare.difference(portTrackCircle)
		CSG starboardTrackArc = portTrackArc.mirrorx()
		CSG trackShelfPrimitives = portTrack.union(starboardTrack).union(backTrack).union(portTrackArc).union(starboardTrackArc)
		
		// Define a shelf for the monorail track using a set of bezier curves
		BezierEditor trackBezierEditorA = new BezierEditor(ScriptingEngine.fileFromGit(URL, "trackBezA.json"),trackStraightBezierPieces)
		BezierEditor trackBezierEditorB = new BezierEditor(ScriptingEngine.fileFromGit(URL, "trackBezB.json"),trackCurveBezierPieces)
		BezierEditor trackBezierEditorC = new BezierEditor(ScriptingEngine.fileFromGit(URL, "trackBezC.json"),trackStraightBezierPieces)
		BezierEditor trackBezierEditorD = new BezierEditor(ScriptingEngine.fileFromGit(URL, "trackBezD.json"),trackCurveBezierPieces)
		BezierEditor trackBezierEditorE = new BezierEditor(ScriptingEngine.fileFromGit(URL, "trackBezE.json"),trackStraightBezierPieces)
		
		// Specify each of the start-end points that define the track's bezier curve
		Vector3d portForwardTrackPoint = new Vector3d(bayWidth.getMM()/2-trackDistFromWall.getMM(), bayDepth.getMM()/2, (double) 0)
		Vector3d portBackTrackPoint = new Vector3d(bayWidth.getMM()/2-trackDistFromWall.getMM(), -bayDepth.getMM()/2+trackDistFromWall.getMM()+turningRadius.getMM(), (double) 0)
		Vector3d backPortTrackPoint = new Vector3d(bayWidth.getMM()/2-trackDistFromWall.getMM()-turningRadius.getMM(), -bayDepth.getMM()/2+trackDistFromWall.getMM(), (double) 0)
		Vector3d backStarboardTrackPoint = new Vector3d(-bayWidth.getMM()/2+trackDistFromWall.getMM()+turningRadius.getMM(), -bayDepth.getMM()/2+trackDistFromWall.getMM(), (double) 0)
		Vector3d starboardBackTrackPoint = new Vector3d(-bayWidth.getMM()/2+trackDistFromWall.getMM(), -bayDepth.getMM()/2+trackDistFromWall.getMM()+turningRadius.getMM(), (double) 0)
		Vector3d starboardFrontTrackPoint = new Vector3d(-bayWidth.getMM()/2+trackDistFromWall.getMM(), bayDepth.getMM()/2, (double) 0)
		
		// TODO: should really specify the control points of the rounded corner beziers to explicitly take into account the turningRadius and generate a true arc

		// Hardcode control points programmatically for a single bezier curve
		trackBezierEditorA.setStart(portForwardTrackPoint)
		trackBezierEditorA.setCP1(portForwardTrackPoint.x, portForwardTrackPoint.y-50, portForwardTrackPoint.z)
		trackBezierEditorA.setCP2(portBackTrackPoint.x, portBackTrackPoint.y+50, portBackTrackPoint.z)
		trackBezierEditorA.setEnd(portBackTrackPoint)
		
		// Append another bezier curve
		trackBezierEditorA.addBezierToTheEnd(trackBezierEditorB)
		
		// Hardcode control points programmatically for a single bezier curve
		trackBezierEditorB.setStart(portBackTrackPoint)
		trackBezierEditorB.setCP1(portBackTrackPoint.x, portBackTrackPoint.y-30, portBackTrackPoint.z)
		trackBezierEditorB.setCP2(backPortTrackPoint.x+30, backPortTrackPoint.y, backPortTrackPoint.z)
		trackBezierEditorB.setEnd(backPortTrackPoint)
		
		// Append another bezier curve
		trackBezierEditorB.addBezierToTheEnd(trackBezierEditorC)
		
		// Hardcode control points programmatically for a single bezier curve
		trackBezierEditorC.setStart(backPortTrackPoint)
		trackBezierEditorC.setCP1(backPortTrackPoint.x-50, backPortTrackPoint.y, backPortTrackPoint.z)
		trackBezierEditorC.setCP2(backStarboardTrackPoint.x+50, backStarboardTrackPoint.y, backStarboardTrackPoint.z)
		trackBezierEditorC.setEnd(backStarboardTrackPoint)
		
		// Append another bezier curve
		trackBezierEditorC.addBezierToTheEnd(trackBezierEditorD)
		
		// Hardcode control points programmatically for a single bezier curve
		trackBezierEditorD.setStart(backStarboardTrackPoint)
		trackBezierEditorD.setCP1(backStarboardTrackPoint.x-30, backStarboardTrackPoint.y, backStarboardTrackPoint.z)
		trackBezierEditorD.setCP2(starboardBackTrackPoint.x, starboardBackTrackPoint.y-30, starboardBackTrackPoint.z)
		trackBezierEditorD.setEnd(starboardBackTrackPoint)
		
		// Append another bezier curve
		trackBezierEditorD.addBezierToTheEnd(trackBezierEditorE)
		
		// Hardcode control points programmatically for a single bezier curve
		trackBezierEditorE.setStart(starboardBackTrackPoint)
		trackBezierEditorE.setCP1(starboardBackTrackPoint.x, starboardBackTrackPoint.y+50, starboardBackTrackPoint.z)
		trackBezierEditorE.setCP2(starboardFrontTrackPoint.x, starboardFrontTrackPoint.y-50, starboardFrontTrackPoint.z)
		trackBezierEditorE.setEnd(starboardFrontTrackPoint)
		
		// Add all the bezier point-wise transformations to a list, to generate a polygon later for extrusion
		ArrayList<Transform> trackBezTrans = []
		trackBezTrans.addAll(trackBezierEditorA.transforms())
		trackBezTrans.addAll(trackBezierEditorB.transforms())
		trackBezTrans.addAll(trackBezierEditorC.transforms())
		trackBezTrans.addAll(trackBezierEditorD.transforms())
		trackBezTrans.addAll(trackBezierEditorE.transforms())
		
		// Create an array of CSG objects to display the cartesian manipulators later
		ArrayList<CSG> trackManips = []
		trackManips.addAll(trackBezierEditorA.getCSG())
		trackManips.addAll(trackBezierEditorB.getCSG())
		trackManips.addAll(trackBezierEditorC.getCSG())
		trackManips.addAll(trackBezierEditorD.getCSG())
		trackManips.addAll(trackBezierEditorE.getCSG())
		
		List<Vector3d> trackPoly = [new Vector3d(-bayWidth.getMM()/2, bayDepth.getMM()/2,0),
									new Vector3d(-bayWidth.getMM()/2, -bayDepth.getMM()/2,0),
									new Vector3d(bayWidth.getMM()/2, -bayDepth.getMM()/2,0),
									new Vector3d(bayWidth.getMM()/2, bayDepth.getMM()/2,0)]
		for(Transform trans : trackBezTrans) {
			trackPoly.add(new Vector3d(trans.getX(),trans.getY(),trans.getZ()))
		}
		CSG trackShelfBez = Extrude.points(new Vector3d(0,0,boardThickness.getMM()),trackPoly)
		
		// Select either bezier track shelf or primitives track shelf
		CSG trackShelf = trackShelfBez
		def trackTrans = new Transform()
								.movex(0)
								.movey(0)
								.movez(railElevation.getMM())
	    trackShelf = trackShelf.transformed(trackTrans)
		
		// Save trackShelf to a temporary CSG so that addTabs uses the correct edge lengths
		CSG trackShelfTemp = trackShelf
		
		// Add tabs to the Y- side
		returned = trackShelfTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		trackShelf = trackShelf.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Add tabs to the X+ side
		returned = trackShelfTemp.addTabs(new Vector3d(1, 0, 0), screwDiameter);
		trackShelf = trackShelf.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Add tabs to the X- side
		returned = trackShelfTemp.addTabs(new Vector3d(-1, 0, 0), screwDiameter);
		trackShelf = trackShelf.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		CSG portWall = new Cube(boardThickness.getMM(),bayDepth.getMM(),bayHeight.getMM()/2).toCSG()
			.movex(bayWidth.getMM()/2 + boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/4)
		CSG starboardWall = portWall.mirrorx()
		portWall = portWall.difference(plantShelf).difference(trackShelf).difference(fasteners)
		starboardWall = starboardWall.difference(plantShelf).difference(trackShelf).difference(fasteners)
		
		// Save portWall to a temporary CSG so that addTabs uses the correct edge lengths
		CSG wallTemp = portWall
		
		// Add tabs to the Y- side
		returned = wallTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		portWall = portWall.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Save starboardWall to a temporary CSG so that addTabs uses the correct edge lengths
		wallTemp = starboardWall
		
		// Add tabs to the Y- side
		returned = wallTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		starboardWall = starboardWall.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		CSG backWall = new Cube(bayWidth.getMM()+boardThickness.getMM()*2, boardThickness.getMM(), bayHeight.getMM()/2).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2-boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/4)
		backWall = backWall.difference(plantShelf).difference(trackShelf).difference(portWall).difference(starboardWall).difference(fasteners)
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		// define two input vectors and circle radius
		def p1s = new Vector3d(1, 0, 0)
		def p1e = new Vector3d(1, 2, 0)
		def p2s = new Vector3d(0, 1, 0)
		def p2e = new Vector3d(2, 1, 0)
		def radius = 1
		
		// call the function to get the tangent points
		def points = getTangentPoints(p1s, p1e, p2s, p2e, radius)
		
		// print the results
		println("Tangent point 1: ${points[0]}") // expected output: [1.0, 1.0, 0.0]
		println("Tangent point 2: ${points[1]}") // expected output: [1.0, 1.0, 0.0]
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
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
		back.add(starboardWall.setColor(javafx.scene.paint.Color.AQUAMARINE))
		back.addAll(fasteners)
		
		
		for(CSG c:back)
			c.setManipulator(arg0.getRootListener())
			
		for(DHParameterKinematics kin:arg0.getAllDHChains()) {
			CSG limbRoot =new Cube(1).toCSG()
			limbRoot.setManipulator(kin.getRootListener())
			back.add(limbRoot)

		}
//		back.addAll(armManips)	//			Uncomment to show and edit the bezier arm manipulators - JMS, Feb 2023
//		back.addAll(trackManips)	//			Uncomment to show and edit the bezier track manipulators - JMS, Mar 2023
		
		return back;
	}
	
	def getTangentPoints(Vector3d p1s, Vector3d p1e, Vector3d p2s, Vector3d p2e, double radius) {
	    // create two Vector3d objects representing the input vectors
	    def v1 = p1e.minus(p1s)
	    def v2 = p2e.minus(p2s)
	
	    // calculate the vector between the starting points of the input vectors
	    def p = p2s.minus(p1s)
	
	    // calculate the dot product and cross product of the input vectors
	    def dot = v1.dot(v2)
	    def cross = v1.cross(v2)
	
	    // calculate the coefficients for the quadratic equation
	    def a = v1.lengthSquared() - dot*dot/v2.lengthSquared()
	    def b = 2*p.dot(v1) - 2*dot/v2.lengthSquared()*p.dot(v2)
	    def c = p.lengthSquared() - dot*dot/v2.lengthSquared() - radius*radius
	
	    // solve the quadratic equation
	    def discriminant = b*b - 4*a*c
	    if (discriminant < 0) {
	        // no real solutions, return null
	        println("No real solutions")
	        return null
	    } else {
	        def t1 = (-b - Math.sqrt(discriminant)) / (2*a)
	        def t2 = (-b + Math.sqrt(discriminant)) / (2*a)
	
	        // calculate the two points of intersection
	        def q1 = p1s.plus(v1.times(t1))
	        def q2 = p1s.plus(v1.times(t2))
	
	        // return the two points
	        return [q1, q2]
	    }
	}
	
}
