import com.neuronrobotics.bowlerkernel.Bezier3d.*;

import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase

import eu.mihosoft.vrl.v3d.*
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
		LengthParameter railElevation = new LengthParameter("Rail Elevation (mm)", 600, [0, 1000])
		railElevation.setMM(600)
		LengthParameter trackDistFromWall = new LengthParameter("Track Distance from Wall (mm)", 25, [0, 1000])
		trackDistFromWall.setMM(25)
		
		// define the parameters for the monorail linear gears
		//
		
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
		//armBezierEditor.setCP2(bucketDiameter.getMM()/2, armDepth+10, 0)
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
		//back.addAll(armCurve)	//			Uncomment to show and edit the bezier arm curve - JMS, Feb 2023
		
		return back;
	}
	
}
